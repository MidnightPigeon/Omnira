package com.mcmagic.omnira.alchemy;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.OrbUpgradeItem;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.*;
import java.util.*;

public final class ReactionPoolBlockEntity extends BlockEntity implements MenuProvider {
    private static final class Job {
        final ReactionRecipe recipe;
        final ItemStack result;
        double progress;
        int batches;
        Job(ReactionRecipe recipe,ItemStack result){this.recipe=recipe;this.result=result;}
        int inputLeft(FluidStack fluid){return fluid.getAmount()-fluid.getAmount()*batches/10;}
        int inputLeft(){return recipe.fluidInputs().stream().mapToInt(this::inputLeft).sum();}
        int outputLeft(){return recipe.fluidResult().getAmount()-recipe.fluidResult().getAmount()*batches/10;}
    }
    private final List<Job> jobs=new ArrayList<>();
    private boolean internal,loading;
    private int energy;
    private final com.mcmagic.omnira.energy.EnergyInputLimit inputLimit=new com.mcmagic.omnira.energy.EnergyInputLimit(512*16);
    private final int[] arrivals=new int[20];
    private long energyTick=Long.MIN_VALUE;
    private int itemBudget,fluidBudget;
    private long outputTick=Long.MIN_VALUE;
    private double factor;
    private boolean electricalPowered;
    public boolean electricalPowered(){return electricalPowered;}
    private double energyRemainder;
    private ItemStack lastOutputItem=ItemStack.EMPTY;
    private FluidStack lastOutputFluid=FluidStack.EMPTY;
    private long lastItemTick,lastFluidTick;
    public ItemStack outputItem(){return lastOutputItem.copy();}
    public FluidStack outputFluid(){return lastOutputFluid.copy();}
    public long outputItemTick(){return lastItemTick;}
    public long outputFluidTick(){return lastFluidTick;}
    public final PoolFluids fluids=new PoolFluids(this::changed,this::capacity);
    public final ItemStackHandler items=new ItemStackHandler(9){
        @Override public int getSlotLimit(int slot){return 64*multiplier(-1);}
        @Override protected int getStackLimit(int slot,ItemStack stack){return itemLimit(stack,multiplier(-1));}
        @Override public boolean isItemValid(int slot,ItemStack stack){
            for(int i=0;i<9;i++)if(i!=slot&&ItemStack.isSameItemSameComponents(stack,getStackInSlot(i)))return false;
            return true;
        }
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
            if(!internal&&!jobs.isEmpty()){
                var remainder=super.insertItem(slot,stack,true);int added=stack.getCount()-remainder.getCount();
                var trial=snapshot();trial.set(slot,stack.copyWithCount(getStackInSlot(slot).getCount()+added));
                if(!pendingItemsFit(trial,null))return stack;
            }
            return super.insertItem(slot,stack,simulate);
        }
        @Override protected void onContentsChanged(int slot){changed();}
    };
    public final ItemStackHandler upgrades=new ItemStackHandler(3){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){
            var kind=OrbUpgrades.kind(stack);
            if(kind!=OrbUpgradeItem.Kind.INTAKE&&kind!=OrbUpgradeItem.Kind.OUTPUT&&kind!=OrbUpgradeItem.Kind.STACKING&&kind!=OrbUpgradeItem.Kind.INFUSED_STACKING)return false;
            for(int i=0;i<3;i++)if(i!=slot&&kind.sameFamily(OrbUpgrades.kind(getStackInSlot(i))))return false;
            return true;
        }
        @Override public ItemStack extractItem(int slot,int count,boolean simulate){return fitsMultiplier(multiplier(slot))?super.extractItem(slot,count,simulate):ItemStack.EMPTY;}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    /** Player interaction is unfiltered, but never bypasses reaction reservations. */
    public final IFluidHandler manualFluid=new FluidPort(false,false);
    public final IFluidHandler fluidInput=new FluidPort(true,false);
    public final IFluidHandler fluidOutput=new FluidPort(true,true);
    public final IItemHandler itemInput=new ItemPort(false),itemOutput=new ItemPort(true);
    public final IEnergyStorage energyInput=new IEnergyStorage(){
        public int receiveEnergy(int amount,boolean simulate){
            if(amount<=0||level==null||level.isClientSide)return 0;
            advanceEnergyClock();int accepted=inputLimit.receive(level.getGameTime(),amount,40000-energy,simulate);
            if(!simulate&&accepted>0){energy+=accepted;arrivals[(int)Math.floorMod(level.getGameTime(),20)]+=accepted;setChanged();}
            return accepted;
        }
        public int extractEnergy(int n,boolean simulate){return 0;}
        public int getEnergyStored(){return energy;}
        public int getMaxEnergyStored(){return 40000;}
        public boolean canExtract(){return false;}
        public boolean canReceive(){return true;}
    };
    public ReactionPoolBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.REACTION_POOL.get(),pos,state);}
    public int multiplier(int except){int multiplier=1;if(upgrades!=null)for(int i=0;i<3;i++)if(i!=except){var kind=OrbUpgrades.kind(upgrades.getStackInSlot(i));if(kind!=null)multiplier=Math.max(multiplier,kind.capacityMultiplier);}return multiplier;}
    public int capacity(){return PoolFluids.CAPACITY*multiplier(-1);}
    public int itemLimit(ItemStack stack,int multiplier){return stack.getMaxStackSize()>1?stack.getMaxStackSize()*multiplier:1;}
    public boolean canSetItem(int slot,ItemStack stack){
        if(stack.isEmpty())return true;if(!items.isItemValid(slot,stack)||stack.getCount()>itemLimit(stack,multiplier(-1)))return false;
        var trial=snapshot();trial.set(slot,stack);return pendingItemsFit(trial,null);
    }
    public boolean fitsMultiplier(int multiplier){
        if(fluids.total()>PoolFluids.CAPACITY*multiplier)return false;
        for(var stack:snapshot())if(stack.getCount()>itemLimit(stack,multiplier))return false;
        return pendingItemsFit(snapshot(),null,multiplier)&&pendingFluidsFit(fluids.snapshot(),PoolFluids.CAPACITY*multiplier);
    }
    public boolean controller(){return getBlockState().getValue(ReactionPoolBlock.PART)==4;}
    public boolean complete(){return level!=null&&ReactionPoolBlock.complete(level,worldPosition,getBlockState().getValue(ReactionPoolBlock.FACING));}
    public double efficiency(){return factor;}
    public double electricalEfficiency(){advanceEnergyClock();return Math.min(16,Math.min(Arrays.stream(arrivals).sum(),energy*20.0)/512.0);}
    public boolean powered(){return factor>=1&&complete();}
    public boolean reacting(){return !jobs.isEmpty();}
    public int reactionCount(){return jobs.size();}
    public int progressPercent(){return jobs.isEmpty()?0:(int)jobs.stream().mapToDouble(j->100*j.progress/j.recipe.duration()).average().orElse(0);}
    public boolean supportMatches(Ingredient support){
        if(support.isEmpty())return true;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(support.test(new ItemStack(level.getBlockState(worldPosition.offset(x,-1,z)).getBlock())))return true;
        return false;
    }
    private List<ItemStack> snapshot(){var list=new ArrayList<ItemStack>();for(int i=0;i<9;i++)list.add(items.getStackInSlot(i).copy());return list;}
    private boolean outputsFit(ReactionRecipe recipe,List<ItemStack> stored){
        for(var output:recipe.allItemResults()){
            boolean fits=false;
            for(var stack:stored){
                if(ItemStack.isSameItemSameComponents(stack,output)){
                    if(stack.getCount()+output.getCount()>itemLimit(stack,multiplier(-1)))return false;
                    fits=true;
                }else if(stack.isEmpty())fits=true;
            }
            if(!fits)return false;
        }
        return true;
    }
    private boolean pendingItemsFit(List<ItemStack> stored,ReactionRecipe candidate){
        return pendingItemsFit(stored,candidate,multiplier(-1));
    }
    private boolean pendingItemsFit(List<ItemStack> stored,ReactionRecipe candidate,int multiplier){
        // Test every outcome of a new random recipe; existing jobs have already fixed their outcome.
        var outcomes=candidate==null?List.<List<ItemStack>>of(List.of()):candidate.outputOptions();
        for(var outcome:outcomes){
            var outputs=new ArrayList<ItemStack>();for(var job:jobs)outputs.addAll(job.recipe.completeOutputs(job.result));
            outputs.addAll(outcome);
            if(!ReactionItemOutputs.fit(stored,outputs,s->itemLimit(s,multiplier)))return false;
        }
        return true;
    }
    private boolean fluidScheduleFits(ReactionRecipe recipe){
        var trial=new PoolFluids(()->{},this::capacity);for(var f:fluids.snapshot())trial.fill(f,IFluidHandler.FluidAction.EXECUTE);
        for(int step=1;step<=10;step++){
            for(var fluid:recipe.fluidInputs()){
                int amount=ReactionRecipe.batchAmount(fluid,step);
                if(amount>0&&trial.drain(fluid.copyWithAmount(amount),IFluidHandler.FluidAction.EXECUTE).getAmount()!=amount)return false;
            }
            int out=ReactionRecipe.batchAmount(recipe.fluidResult(),step);
            if(out>0&&trial.fill(recipe.fluidResult().copyWithAmount(out),IFluidHandler.FluidAction.EXECUTE)!=out)return false;
        }
        return true;
    }
    public boolean start(ReactionRecipe recipe){
        if(level==null||level.isClientSide||jobs.stream().anyMatch(j->sameRecipe(j.recipe,recipe)))return false;
        var input=new ReactionRecipe.Input(items,fluids,this::supportMatches);
        if(!recipe.matches(input,level))return false;
        for(var output:recipe.allItemResults())for(var stored:snapshot())if(ItemStack.isSameItemSameComponents(stored,output)&&stored.getCount()>=itemLimit(stored,multiplier(-1)))return false;
        var after=snapshot();var slots=recipe.assignment(input);
        for(int i=0;i<slots.length;i++)after.get(slots[i]).shrink(recipe.items().get(i).count());
        if(!outputsFit(recipe,after)||!pendingItemsFit(after,recipe)||!fluidScheduleFits(recipe))return false;
        var job=new Job(recipe,recipe.results().isEmpty()?ItemStack.EMPTY:recipe.results().get(level.random.nextInt(recipe.results().size())).copy());
        jobs.add(job);
        if(!pendingFluidsFit(fluids.snapshot())){jobs.remove(job);return false;}
        for(int i=0;i<slots.length;i++)items.extractItem(slots[i],recipe.items().get(i).count(),false);
        changed();return true;
    }
    public void startAvailable(List<ReactionRecipe> recipes){
        for(boolean catalytic:new boolean[]{true,false})for(var recipe:recipes)
            if(catalytic!=recipe.support().isEmpty())start(recipe);
    }
    private boolean sameRecipe(ReactionRecipe a,ReactionRecipe b){
        var ops=level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return ReactionRecipe.CODEC.codec().encodeStart(ops,a).getOrThrow().equals(ReactionRecipe.CODEC.codec().encodeStart(ops,b).getOrThrow());
    }
    public static void tick(Level level,BlockPos pos,BlockState state,ReactionPoolBlockEntity pool){
        if(!pool.controller()||level.isClientSide)return;
        pool.advanceEnergyClock();
        double mechanical=pool.mechanicalPower(),electric=pool.electricalEfficiency();
        double next=pool.complete()?Math.max(mechanical,electric):0;
        if(next<1)next=0;
        boolean electrical=next>=1&&electric>mechanical;
        boolean modeChanged=electrical!=pool.electricalPowered;pool.electricalPowered=electrical;
        if(electrical)for(int part:new int[]{3,5}){
            var facing=state.getValue(ReactionPoolBlock.FACING);
            com.mcmagic.omnira.energy.ShaftCurrent.spark(level,pos.offset(ReactionPoolBlock.offset(facing,part)),ReactionPoolBlock.outward(facing,part));
        }
        if(next>=1&&electric>mechanical){
            double cost=next*512/20+pool.energyRemainder;int whole=(int)cost;
            pool.energy-=whole;pool.energyRemainder=cost-whole;
        }
        if(modeChanged||Math.abs(next-pool.factor)>.001){pool.factor=next;pool.changed();}else pool.factor=next;
        if(pool.powered()){
            pool.outputBudget();pool.pushOutputs();
            pool.advance(next);
            pool.startAvailable(level.getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get())
                    .stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList());
        }
        if(level.getGameTime()%20==0)pool.changed();
    }
    /** Settled input and output are proportional cumulative integers, including non-divisible mod recipes. */
    public void advance(double work){
        if(work<=0)return;
        for(var job:new ArrayList<>(jobs))advanceJob(job,work);
    }
    private void advanceJob(Job job,double work){
        var active=job.recipe;
        if(!supportMatches(active.support()))return;
        double next=Math.min(active.duration(),job.progress+work);int due=Math.min(10,(int)Math.floor(next*10/active.duration()));
        while(job.batches<due){
            int step=job.batches+1;
            int out=ReactionRecipe.batchAmount(active.fluidResult(),step);
            // Validate the entire installment before consuming any reagent.
            if(active.fluidInputs().stream().anyMatch(f->fluids.amount(f)<ReactionRecipe.batchAmount(f,step))){
                boolean explosive=ReactionFailure.explosive(active.totalFluidInput()-job.inputLeft(),active.totalFluidInput());
                jobs.remove(job);changed();
                if(explosive&&level instanceof net.minecraft.server.level.ServerLevel server)ReactionFailure.blast(server,worldPosition,true);
                return;
            }
            var trial=new PoolFluids(()->{},this::capacity);for(var f:fluids.snapshot())trial.fill(f,IFluidHandler.FluidAction.EXECUTE);
            for(var fluid:active.fluidInputs())trial.drain(fluid.copyWithAmount(ReactionRecipe.batchAmount(fluid,step)),IFluidHandler.FluidAction.EXECUTE);
            if(out>0&&trial.fill(active.fluidResult().copyWithAmount(out),IFluidHandler.FluidAction.EXECUTE)!=out){
                job.progress=(double)step*active.duration()/10;setChanged();return;
            }
            for(var fluid:active.fluidInputs())fluids.drain(fluid.copyWithAmount(ReactionRecipe.batchAmount(fluid,step)),IFluidHandler.FluidAction.EXECUTE);
            if(out>0)fluids.fill(active.fluidResult().copyWithAmount(out),IFluidHandler.FluidAction.EXECUTE);
            job.batches++;job.progress=(double)step*active.duration()/10;setChanged();
        }
        job.progress=next;setChanged();
        if(job.batches==10){
            if(!ReactionItemOutputs.fit(snapshot(),job.recipe.completeOutputs(job.result),s->itemLimit(s,multiplier(-1))))return;
            internal=true;
            try{for(var output:job.recipe.completeOutputs(job.result)){
                var rest=output.copy();for(int i=0;i<9;i++)rest=items.insertItem(i,rest,false);
                if(!rest.isEmpty())throw new IllegalStateException("Reserved reaction output lost");}
            }finally{internal=false;}
            jobs.remove(job);changed();
        }
    }
    private double mechanicalPower(){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))return com.mcmagic.omnira.compat.CreateReactionPool.power(this);
        double count=0;var facing=getBlockState().getValue(ReactionPoolBlock.FACING);
        for(int part:new int[]{3,5}){
            var side=ReactionPoolBlock.outward(facing,part);var at=worldPosition.offset(ReactionPoolBlock.offset(facing,part)).relative(side);
            count+=com.mcmagic.omnira.space.SpacetimePortState.driveAt(level,at,side.getOpposite());
        }
        return count;
    }
    private void advanceEnergyClock(){
        if(level==null)return;long now=level.getGameTime();
        if(energyTick==Long.MIN_VALUE||now-energyTick>=20||now<energyTick)Arrays.fill(arrivals,0);
        else for(long tick=energyTick+1;tick<=now;tick++)arrivals[(int)Math.floorMod(tick,20)]=0;
        energyTick=now;
    }
    private boolean allowed(OrbUpgradeItem.Kind kind,ItemStack item,FluidStack fluid){
        for(int i=0;i<3;i++)if(OrbUpgrades.kind(upgrades.getStackInSlot(i))==kind){
            var tag=OrbUpgrades.settings(upgrades.getStackInSlot(i));boolean match=false;
            for(int j=0;j<3;j++)if(OrbFilterMatcher.matches(level,ItemStack.parseOptional(level.registryAccess(),tag.getCompound("Filter"+j)),item,fluid))match=true;
            return match==tag.getBoolean("Whitelist");
        }
        return true;
    }
    private boolean pendingFluidsFit(List<FluidStack> contents){
        return pendingFluidsFit(contents,capacity());
    }
    private boolean pendingFluidsFit(List<FluidStack> contents,int capacity){
        int total=contents.stream().mapToInt(FluidStack::getAmount).sum();var kinds=new ArrayList<>(contents);
        for(var job:jobs){
            total+=Math.max(0,job.outputLeft()-job.inputLeft());
            var output=job.recipe.fluidResult();
            if(job.outputLeft()>0&&kinds.stream().noneMatch(f->FluidStack.isSameFluidSameComponents(f,output)))kinds.add(output);
        }
        return total<=capacity&&kinds.size()<=4;
    }
    private int safeFill(FluidStack fluid){
        int accepted=fluids.fill(fluid,IFluidHandler.FluidAction.SIMULATE);
        if(jobs.isEmpty())return accepted;
        int low=0,high=accepted;
        while(low<high){int mid=(low+high+1)/2;var trial=new PoolFluids(()->{},this::capacity);for(var f:fluids.snapshot())trial.fill(f,IFluidHandler.FluidAction.EXECUTE);
            trial.fill(fluid.copyWithAmount(mid),IFluidHandler.FluidAction.EXECUTE);
            if(pendingFluidsFit(trial.snapshot()))low=mid;else high=mid-1;
        }
        return low;
    }
    private void outputBudget(){
        if(level==null)return;long second=Math.floorDiv(level.getGameTime(),20);
        if(outputTick!=second){outputTick=second;itemBudget=1;fluidBudget=500;}
    }
    private void pushOutputs(){
        var facing=getBlockState().getValue(ReactionPoolBlock.FACING);
        for(int part:new int[]{6,8}){
            var direction=ReactionPoolBlock.outward(facing,part);var at=worldPosition.offset(ReactionPoolBlock.offset(facing,part)).relative(direction);
            if(!level.hasChunkAt(at))continue;
            if(part==6){
                var target=level.getCapability(Capabilities.ItemHandler.BLOCK,at,direction.getOpposite());if(target==null)continue;
                for(int slot=0;slot<9;slot++){
                    var sample=itemOutput.extractItem(slot,1,true);if(sample.isEmpty())continue;
                    var rest=ItemHandlerHelper.insertItemStacked(target,sample,true);
                    if(rest.isEmpty()){var extracted=itemOutput.extractItem(slot,1,false);var rejected=ItemHandlerHelper.insertItemStacked(target,extracted,false);
                        if(!rejected.isEmpty()){internal=true;try{items.insertItem(slot,rejected,false);}finally{internal=false;}}break;}
                }
            }else{
                var target=level.getCapability(Capabilities.FluidHandler.BLOCK,at,direction.getOpposite());if(target==null)continue;
                var sample=fluidOutput.drain(500,IFluidHandler.FluidAction.SIMULATE);
                if(sample.isEmpty())continue;int accepted=target.fill(sample,IFluidHandler.FluidAction.SIMULATE);
                if(accepted>0){var extracted=fluidOutput.drain(sample.copyWithAmount(accepted),IFluidHandler.FluidAction.EXECUTE);
                    int filled=target.fill(extracted,IFluidHandler.FluidAction.EXECUTE);if(filled<extracted.getAmount())fluids.fill(extracted.copyWithAmount(extracted.getAmount()-filled),IFluidHandler.FluidAction.EXECUTE);}
            }
        }
    }
    private final class ItemPort implements IItemHandler {
        private final boolean output;
        ItemPort(boolean output){this.output=output;}
        public int getSlots(){return 9;}
        public ItemStack getStackInSlot(int slot){return items.getStackInSlot(slot).copy();}
        public int getSlotLimit(int slot){return items.getSlotLimit(slot);}
        public boolean isItemValid(int slot,ItemStack stack){return !output&&allowed(OrbUpgradeItem.Kind.INTAKE,stack,null)&&items.isItemValid(slot,stack);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isItemValid(slot,stack)?items.insertItem(slot,stack,simulate):stack;}
        public ItemStack extractItem(int slot,int count,boolean simulate){
            outputBudget();if(!output||!powered()||itemBudget<=0||!allowed(OrbUpgradeItem.Kind.OUTPUT,items.getStackInSlot(slot),null))return ItemStack.EMPTY;
            var extracted=items.extractItem(slot,Math.min(count,itemBudget),simulate);
            if(!simulate){itemBudget-=extracted.getCount();if(!extracted.isEmpty()){lastOutputItem=extracted.copyWithCount(1);lastItemTick=level.getGameTime();changed();}}return extracted;
        }
    }
    private final class FluidPort implements IFluidHandler {
        private final boolean filtered,output;
        FluidPort(boolean filtered,boolean output){this.filtered=filtered;this.output=output;}
        public int getTanks(){return 4;}
        public FluidStack getFluidInTank(int tank){return fluids.getFluidInTank(tank);}
        public int getTankCapacity(int tank){return fluids.getTankCapacity(tank);}
        public boolean isFluidValid(int tank,FluidStack fluid){return !output&&(!filtered||allowed(OrbUpgradeItem.Kind.INTAKE,ItemStack.EMPTY,fluid));}
        public int fill(FluidStack fluid,FluidAction action){
            if(fluid.isEmpty()||!isFluidValid(0,fluid))return 0;
            return fluids.fill(fluid.copyWithAmount(Math.min(fluid.getAmount(),safeFill(fluid))),action);
        }
        public FluidStack drain(FluidStack fluid,FluidAction action){
            if(fluid.isEmpty()||filtered&&!output)return FluidStack.EMPTY;
            int amount=Math.min(fluid.getAmount(),fluids.amount(fluid));
            if(output){outputBudget();if(!powered()||!allowed(OrbUpgradeItem.Kind.OUTPUT,ItemStack.EMPTY,fluid))return FluidStack.EMPTY;amount=Math.min(amount,fluidBudget);}
            var result=fluids.drain(fluid.copyWithAmount(amount),action);
            if(output&&action.execute()){fluidBudget-=result.getAmount();if(!result.isEmpty()){lastOutputFluid=result.copy();lastFluidTick=level.getGameTime();changed();}}return result;
        }
        public FluidStack drain(int amount,FluidAction action){
            var stored=fluids.snapshot();
            for(int i=0;i<stored.size();i++){
                int index=output?stored.size()-1-i:i;
                var result=drain(stored.get(index).copyWithAmount(amount),action);if(!result.isEmpty())return result;
            }return FluidStack.EMPTY;
        }
    }
    public final net.neoforged.neoforge.items.IItemHandler nodeItems=new net.neoforged.neoforge.items.IItemHandler(){
        public int getSlots(){return items.getSlots();}
        public ItemStack getStackInSlot(int slot){return items.getStackInSlot(slot);}
        public int getSlotLimit(int slot){return items.getSlotLimit(slot);}
        public boolean isItemValid(int slot,ItemStack stack){return itemInput.isItemValid(slot,stack);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return itemInput.insertItem(slot,stack,simulate);}
        public ItemStack extractItem(int slot,int count,boolean simulate){return items.extractItem(slot,count,simulate);}
    };
    public final IFluidHandler nodeFluids=new IFluidHandler(){
        public int getTanks(){return fluids.getTanks();}
        public FluidStack getFluidInTank(int tank){return fluids.getFluidInTank(tank);}
        public int getTankCapacity(int tank){return fluids.getTankCapacity(tank);}
        public boolean isFluidValid(int tank,FluidStack stack){return fluidInput.isFluidValid(tank,stack);}
        public int fill(FluidStack stack,FluidAction action){return fluidInput.fill(stack,action);}
        public FluidStack drain(FluidStack stack,FluidAction action){return manualFluid.drain(stack,action);}
        public FluidStack drain(int amount,FluidAction action){return manualFluid.drain(amount,action);}
    };
    public void changed(){
        if(loading)return;setChanged();
        if(level!=null&&!level.isClientSide&&controller())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public void dropContents(){for(int i=0;i<9;i++)Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,items.getStackInSlot(i));
        for(int i=0;i<3;i++)Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,upgrades.getStackInSlot(i));}
    public Component getDisplayName(){return Component.translatable("block.omnira.large_alchemical_reaction_pool");}
    public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new ReactionPoolMenu(id,inventory,this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);
        var compact=new ItemStackHandler(9);int[] counts=new int[9];
        for(int i=0;i<9;i++){counts[i]=items.getStackInSlot(i).getCount();compact.setStackInSlot(i,items.getStackInSlot(i).copyWithCount(1));}
        tag.put("Items",compact.serializeNBT(registries));tag.putIntArray("ItemCounts",counts);
        tag.put("Upgrades",upgrades.serializeNBT(registries));tag.put("Fluids",fluids.save(registries));tag.putInt("Energy",energy);
        tag.putDouble("Factor",factor);tag.putBoolean("ElectricalPowered",electricalPowered);
        if(!lastOutputItem.isEmpty())tag.put("OutputItem",lastOutputItem.save(registries));
        if(!lastOutputFluid.isEmpty())tag.put("OutputFluid",lastOutputFluid.save(registries));
        tag.putLong("OutputItemTick",lastItemTick);tag.putLong("OutputFluidTick",lastFluidTick);
        var reactions=new ListTag();for(var job:jobs){var entry=new CompoundTag();
            entry.put("Recipe",ReactionRecipe.CODEC.codec().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),job.recipe).getOrThrow());
            entry.putDouble("Progress",job.progress);entry.putInt("Batches",job.batches);if(!job.result.isEmpty())entry.put("Result",job.result.save(registries));reactions.add(entry);
        }tag.put("Reactions",reactions);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);loading=true;
        try{upgrades.deserializeNBT(registries,tag.getCompound("Upgrades"));items.deserializeNBT(registries,tag.getCompound("Items"));
            var counts=tag.getIntArray("ItemCounts");for(int i=0;i<Math.min(9,counts.length);i++)if(!items.getStackInSlot(i).isEmpty())items.getStackInSlot(i).setCount(Math.clamp(counts[i],1,itemLimit(items.getStackInSlot(i),multiplier(-1))));
            fluids.load(tag.getList("Fluids",Tag.TAG_COMPOUND),registries);
            energy=Math.clamp(tag.getInt("Energy"),0,40000);factor=tag.getDouble("Factor");electricalPowered=tag.getBoolean("ElectricalPowered");
            lastOutputItem=ItemStack.parseOptional(registries,tag.getCompound("OutputItem"));
            lastOutputFluid=FluidStack.parseOptional(registries,tag.getCompound("OutputFluid"));
            lastItemTick=tag.getLong("OutputItemTick");lastFluidTick=tag.getLong("OutputFluidTick");
            jobs.clear();for(var value:tag.getList("Reactions",Tag.TAG_COMPOUND)){var entry=(CompoundTag)value;
                var recipe=ReactionRecipe.CODEC.codec().parse(registries.createSerializationContext(NbtOps.INSTANCE),entry.get("Recipe")).result().orElse(null);
                if(recipe!=null){var job=new Job(recipe,ItemStack.parseOptional(registries,entry.getCompound("Result")));job.progress=Math.clamp(entry.getDouble("Progress"),0,recipe.duration());job.batches=Math.clamp(entry.getInt("Batches"),0,10);jobs.add(job);}
            }
        }finally{loading=false;}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
