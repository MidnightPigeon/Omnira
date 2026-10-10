package com.mcmagic.omnira.alchemy;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RoughAlchemyCauldronBlockEntity extends BlockEntity {
    public static final int CAPACITY=4000;
    private ReactionRecipe active;
    private net.minecraft.world.item.ItemStack output=net.minecraft.world.item.ItemStack.EMPTY;
    private int progress,consumed,batches;
    private boolean internal;
    public final net.neoforged.neoforge.items.ItemStackHandler items=new net.neoforged.neoforge.items.ItemStackHandler(4){
        @Override public net.minecraft.world.item.ItemStack insertItem(int slot,net.minecraft.world.item.ItemStack stack,boolean simulate){
            if(stack.isEmpty())return stack;
            for(int i=0;i<4;i++)if(i!=slot&&net.minecraft.world.item.ItemStack.isSameItemSameComponents(stack,getStackInSlot(i)))return stack;
            var remainder=super.insertItem(slot,stack,true);
            int added=stack.getCount()-remainder.getCount();
            if(!internal&&active!=null&&added>0){
                var trial=snapshot();var old=trial.get(slot);
                trial.set(slot,stack.copyWithCount(old.getCount()+added));
                if(!outputsFit(active,trial))return stack;
            }
            return super.insertItem(slot,stack,simulate);
        }
        @Override protected void onContentsChanged(int slot){changed();}
    };
    public final net.neoforged.neoforge.fluids.capability.templates.FluidTank tank=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(CAPACITY){
        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack fluid,FluidAction action){
            if(!internal&&active!=null&&!active.fluidResult().isEmpty()){
                var result=active.fluidResult();
                if(!net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(fluid,result))return 0;
                int remaining=result.getAmount()-result.getAmount()*batches/10;
                return super.fill(fluid.copyWithAmount(Math.min(fluid.getAmount(),Math.max(0,CAPACITY-getFluidAmount()-remaining))),action);
            }
            return super.fill(fluid,action);
        }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(int amount,FluidAction action){
            return super.drain(amount,action);
        }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack fluid,FluidAction action){
            return net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(fluid,getFluid())?drain(fluid.getAmount(),action):net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        }
        @Override protected void onContentsChanged(){changed();}
    };
    private void changed(){
        setChanged();
        if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public boolean working(){return active!=null;}
    public int progress(){return progress;}
    public net.minecraft.world.item.ItemStack insert(net.minecraft.world.item.ItemStack stack){
        var remaining=stack.copy();
        for(int i=0;i<4;i++)remaining=items.insertItem(i,remaining,false);
        return remaining;
    }
    private java.util.List<net.minecraft.world.item.ItemStack> snapshot(){
        var result=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        for(int i=0;i<4;i++)result.add(items.getStackInSlot(i).copy());return result;
    }
    private boolean outputsFit(ReactionRecipe recipe,java.util.List<net.minecraft.world.item.ItemStack> inventory){
        for(var outcome:recipe.outputOptions())if(!ReactionItemOutputs.fit(inventory,outcome,net.minecraft.world.item.ItemStack::getMaxStackSize))return false;
        var fluid=recipe.fluidResult();
        return fluid.isEmpty()||(tank.isEmpty()||net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(fluid,tank.getFluid()))
                &&tank.getFluidAmount()+fluid.getAmount()<=CAPACITY;
    }
    public boolean start(ReactionRecipe recipe){
        if(active!=null||!reaction()||level==null||level.isClientSide)return false;
        if(recipe.requiresPool())return false;
        var input=new ReactionRecipe.Input(this);if(!recipe.matches(input,level))return false;
        for(var result:recipe.allItemResults())for(int i=0;i<4;i++){
            var stored=items.getStackInSlot(i);
            if(net.minecraft.world.item.ItemStack.isSameItemSameComponents(stored,result)&&stored.getCount()>=stored.getMaxStackSize())return false;
        }
        int[] slots=recipe.assignment(input);var after=snapshot();
        for(int i=0;i<slots.length;i++)after.get(slots[i]).shrink(recipe.items().get(i).count());
        if(!outputsFit(recipe,after))return false;
        active=recipe;progress=consumed=batches=0;
        output=recipe.results().isEmpty()?net.minecraft.world.item.ItemStack.EMPTY:recipe.results().get(level.random.nextInt(recipe.results().size())).copy();
        for(int i=0;i<slots.length;i++)items.extractItem(slots[i],recipe.items().get(i).count(),false);
        changed();return true;
    }
    public boolean startNext(java.util.List<ReactionRecipe> recipes){
        for(boolean catalytic:new boolean[]{true,false})for(var recipe:recipes)
            if(catalytic!=recipe.support().isEmpty()&&start(recipe))return true;
        return false;
    }
    public static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,RoughAlchemyCauldronBlockEntity vessel){
        if(!vessel.reaction())return;
        if(level.isClientSide){
            if(vessel.working()&&level.random.nextInt(4)==0)level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.getX()+.3+level.random.nextDouble()*.4,pos.getY()+.9,pos.getZ()+.3+level.random.nextDouble()*.4,0,.025,0);
            return;
        }
        if(vessel.active==null){
            vessel.startNext(level.getRecipeManager().getAllRecipesFor(com.mcmagic.omnira.registry.ModRecipes.REACTION_TYPE.get())
                    .stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList());
            return;
        }
        var recipe=vessel.active;if(!recipe.supportMatches(vessel))return;
        int next=Math.min(recipe.duration(),vessel.progress+1);
        int due=Math.min(10,(int)((long)next*10/recipe.duration()));
        vessel.internal=true;
        try{
            while(vessel.batches<due){
                int step=vessel.batches+1;
                int required=Math.max(0,recipe.fluid().getAmount()*step/10-vessel.consumed);
                if(required>0&&(required>vessel.tank.getFluidAmount()
                        ||!net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(recipe.fluid(),vessel.tank.getFluid()))){
                    boolean explosive=ReactionFailure.explosive(vessel.consumed,recipe.fluid().getAmount());
                    vessel.active=null;vessel.output=net.minecraft.world.item.ItemStack.EMPTY;vessel.progress=vessel.consumed=vessel.batches=0;vessel.changed();
                    if(explosive)ReactionFailure.blast((net.minecraft.server.level.ServerLevel)level,pos,false);
                    return;
                }
                int produced=ReactionRecipe.batchAmount(recipe.fluidResult(),step);
                if(produced>0&&vessel.tank.fill(recipe.fluidResult().copyWithAmount(produced),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)!=produced){
                    vessel.progress=(recipe.duration()*step+9)/10;vessel.setChanged();return;
                }
                if(required>0)vessel.tank.drain(required,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                if(produced>0)vessel.tank.fill(recipe.fluidResult().copyWithAmount(produced),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                vessel.consumed+=required;vessel.batches=step;vessel.setChanged();
            }
            vessel.progress=next;
            if(next>=recipe.duration()){
                if(!ReactionItemOutputs.fit(vessel.snapshot(),recipe.completeOutputs(vessel.output),net.minecraft.world.item.ItemStack::getMaxStackSize)){
                    vessel.setChanged();return;
                }
                for(var result:recipe.completeOutputs(vessel.output)){
                    if(!vessel.insert(result).isEmpty())throw new IllegalStateException("Reserved reaction output lost");
                }
                vessel.active=null;vessel.output=net.minecraft.world.item.ItemStack.EMPTY;vessel.progress=vessel.consumed=vessel.batches=0;
                vessel.changed();
            }else vessel.setChanged();
        }finally{vessel.internal=false;}
    }
    private BlockState campfire=Blocks.CAMPFIRE.defaultBlockState();
    private CompoundTag campfireData=new CompoundTag();
    public RoughAlchemyCauldronBlockEntity(BlockPos pos,BlockState state){
        super(ModBlockEntityTypes.ROUGH_ALCHEMY_CAULDRON.get(),pos,state);
    }
    public boolean reaction(){return getBlockState().is(com.mcmagic.omnira.registry.ModBlocks.PRIMITIVE_REACTION_CAULDRON.get());}
    public void rememberCampfire(BlockState state,CompoundTag data){campfire=state;campfireData=data.copy();setChanged();}
    public BlockState campfire(){return campfire;}
    public CompoundTag campfireData(){return campfireData.copy();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);
        tag.put("Fluid",tank.writeToNBT(registries,new CompoundTag()));
        tag.put("Items",items.serializeNBT(registries));
        if(active!=null){
            tag.put("Reaction",ReactionRecipe.CODEC.codec().encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),active).getOrThrow());
            tag.putInt("Progress",progress);tag.putInt("Consumed",consumed);tag.putInt("Batches",batches);
            if(!output.isEmpty())tag.put("Output",output.save(registries));
        }
        tag.put("Campfire",NbtUtils.writeBlockState(campfire));tag.put("CampfireData",campfireData);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);tank.readFromNBT(registries,tag.getCompound("Fluid"));
        if(tag.contains("Items"))items.deserializeNBT(registries,tag.getCompound("Items"));
        active=tag.contains("Reaction")?ReactionRecipe.CODEC.codec().parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),tag.get("Reaction")).result().orElse(null):null;
        progress=tag.getInt("Progress");consumed=tag.getInt("Consumed");
        batches=tag.contains("Batches")?Math.clamp(tag.getInt("Batches"),0,10):
                active!=null&&active.fluidResult().isEmpty()?Math.min(10,(int)((long)progress*10/active.duration())):0;
        output=net.minecraft.world.item.ItemStack.parseOptional(registries,tag.getCompound("Output"));
        if(tag.contains("Campfire")){
            var state=NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK),tag.getCompound("Campfire"));
            if(state.is(BlockTags.CAMPFIRES))campfire=state;
        }
        campfireData=tag.getCompound("CampfireData").copy();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
