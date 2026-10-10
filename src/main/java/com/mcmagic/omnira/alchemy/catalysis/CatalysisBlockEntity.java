package com.mcmagic.omnira.alchemy.catalysis;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.OrbUpgradeItem.Kind;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.*;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;
import java.util.*;

public final class CatalysisBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY=4000,STEP_TICKS=200,STEP_FLUID=500;
    private boolean internal;
    private int stage=1,elapsed,stemStage=1;
    private long visualUpdatedAt;
    private boolean stemReady;
    private boolean naturalMode,naturalCycle;
    private int stageCount=4;
    private ItemStack boundSoil=ItemStack.EMPTY,boundSeed=ItemStack.EMPTY;
    private FluidStack boundFluid=FluidStack.EMPTY;
    private final List<ItemStack> harvest=new ArrayList<>();
    private final List<ItemStack> overflow=new ArrayList<>();
    public final ItemStackHandler items=new ItemStackHandler(3){
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0?CatalysisGrowth.acceptsSubstrate(level,stack):slot==1;}
        @Override protected void onContentsChanged(int slot){if(!internal&&slot<2)reset();setChanged();}
    };
    public final ItemStackHandler upgrades=new ItemStackHandler(2){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return OrbUpgrades.kind(stack)==(slot==0?Kind.INTAKE:Kind.OUTPUT);}
        @Override protected void onContentsChanged(int slot){setChanged();}
    };
    public final FluidTank tank=new FluidTank(CAPACITY){@Override protected void onContentsChanged(){setChanged();}};
    public final IItemHandler extractOnly=new Port(false),nodeItems=new Port(true);
    public final IFluidHandler nodeFluids=new IFluidHandler(){
        public int getTanks(){return 1;}public FluidStack getFluidInTank(int i){return tank.getFluidInTank(i);}
        public int getTankCapacity(int i){return CAPACITY;}public boolean isFluidValid(int i,FluidStack f){return tank.isFluidValid(i,f);}
        public int fill(FluidStack f,FluidAction action){return tank.fill(f,action);}
        public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
        public FluidStack drain(FluidStack f,FluidAction action){return FluidStack.EMPTY;}
    };
    private final class Port implements IItemHandler {
        private final boolean productOnly;Port(boolean productOnly){this.productOnly=productOnly;}
        public int getSlots(){return productOnly?1:3;}private int index(int i){return productOnly?2:i;}
        public ItemStack getStackInSlot(int i){return items.getStackInSlot(index(i));}
        public int getSlotLimit(int i){return items.getSlotLimit(index(i));}
        public boolean isItemValid(int i,ItemStack s){return false;}
        public ItemStack insertItem(int i,ItemStack s,boolean simulate){return s;}
        public ItemStack extractItem(int i,int n,boolean simulate){return items.extractItem(index(i),n,simulate);}
    }
    public CatalysisBlockEntity(BlockPos pos,BlockState state){super(CatalysisContent.ENTITY.get(),pos,state);}
    public int stage(){return stage;}
    public boolean catalyzing(){
        return complete()&&tank.getFluidAmount()>=STEP_FLUID&&overflow.isEmpty()
                &&items.getStackInSlot(2).isEmpty()&&plan()!=null;
    }
    public float visualStage(double time){
        if(naturalMode||tank.getFluidAmount()<STEP_FLUID||!items.getStackInSlot(2).isEmpty()||plan()==null)return stage;
        double extrapolated=level!=null&&level.isClientSide?Math.clamp(time-visualUpdatedAt,0,20):0;
        return stage+(float)Math.clamp((elapsed+extrapolated)/STEP_TICKS,0,1);
    }
    public int stemStage(){return stemStage;}
    public boolean stemReady(){return stemReady;}
    public CatalysisNaturalGrowth.Rule naturalRule(){return level==null?null:CatalysisNaturalGrowth.resolve(level,items.getStackInSlot(0),items.getStackInSlot(1));}
    public CatalysisGrowth.Plan plan(){
        if(level==null)return null;
        if(tank.isEmpty()){var rule=naturalRule();return rule==null?null:rule.plan();}
        return CatalysisGrowth.resolve(level,items.getStackInSlot(0),items.getStackInSlot(1),tank.getFluid());
    }
    public boolean complete(){
        if(level==null||getBlockState().getValue(CatalysisBlock.PART)!=0)return false;
        for(int i=0;i<3;i++){var s=level.getBlockState(worldPosition.above(i));if(!s.is(CatalysisContent.BLOCK.get())||s.getValue(CatalysisBlock.PART)!=i
                ||s.getValue(CatalysisBlock.FACING)!=getBlockState().getValue(CatalysisBlock.FACING))return false;}return true;
    }
    private void reset(){stage=1;stemStage=1;stemReady=false;elapsed=0;naturalCycle=false;stageCount=4;harvest.clear();boundSeed=ItemStack.EMPTY;boundSoil=ItemStack.EMPTY;boundFluid=FluidStack.EMPTY;}
    private CatalysisGrowth.Plan preparePlan(){
        if(!ItemStack.isSameItemSameComponents(boundSoil,items.getStackInSlot(0))||!ItemStack.isSameItemSameComponents(boundSeed,items.getStackInSlot(1))
                ||!tank.isEmpty()&&!boundFluid.isEmpty()&&!FluidStack.isSameFluidSameComponents(boundFluid,tank.getFluid())){
            reset();boundSoil=items.getStackInSlot(0).copyWithCount(1);boundSeed=items.getStackInSlot(1).copyWithCount(1);setChanged();
        }
        if(!tank.isEmpty())boundFluid=tank.getFluid().copyWithAmount(1);
        var plan=plan();if(plan==null)return null;
        if(naturalMode!=tank.isEmpty()||stageCount!=plan.stages()){
            stage=1+(stage-1)*(plan.stages()-1)/Math.max(1,stageCount-1);elapsed=0;
            stageCount=plan.stages();naturalMode=tank.isEmpty();setChanged();
        }
        return plan;
    }
    public void naturalTick(net.minecraft.util.RandomSource random){
        if(!(level instanceof ServerLevel server)||!complete()||!tank.isEmpty()||!overflow.isEmpty()||!items.getStackInSlot(2).isEmpty())return;
        var plan=preparePlan();var rule=naturalRule();if(plan==null||rule==null)return;
        if(rule.kind()==CatalysisNaturalGrowth.Kind.VINE&&stage>=26)return;
        if(!rule.advance(server,worldPosition.above(),items.getStackInSlot(0),random))return;
        naturalCycle=true;
        if(rule.kind()==CatalysisNaturalGrowth.Kind.STEM&&!stemReady){stemStage++;stemReady=stemStage>=8;setChanged();return;}
        boolean mature=rule.kind()==CatalysisNaturalGrowth.Kind.VINE?random.nextFloat()<.11F:stage+1>=plan.stages();
        if(mature){
            var result=rule.harvest(server,worldPosition.above(),boundSoil,boundSeed);if(result.isEmpty())return;
            overflow.addAll(result);stage=1;naturalCycle=false;flushHarvest();
        }else stage++;
        setChanged();
    }
    public void tick(){
        if(!(level instanceof ServerLevel server)||!complete())return;
        if(level.getGameTime()%20==0)CatalysisAutomation.tick(this);
        if(!overflow.isEmpty()){
            flushHarvest();return;
        }
        var plan=preparePlan();if(plan==null||tank.isEmpty())return;
        if(tank.getFluidAmount()<STEP_FLUID||!items.getStackInSlot(2).isEmpty())return;
        if(++elapsed<STEP_TICKS){if(elapsed%20==0)setChanged();return;}
        elapsed=STEP_TICKS;
        if(plan.yield()==CatalysisGrowth.Yield.FRUIT&&!stemReady){
            tank.drain(STEP_FLUID,IFluidHandler.FluidAction.EXECUTE);elapsed=0;stemStage++;
            stemReady=stemStage>=8;setChanged();return;
        }
        boolean mature=stage+1>=plan.stages();
        if(mature&&harvest.isEmpty()){
            var natural=naturalCycle?naturalRule():null;
            harvest.addAll(natural!=null?natural.harvest(server,worldPosition.above(),boundSoil,boundSeed):plan.harvest(server,worldPosition.above(),boundSoil,boundSeed));setChanged();
            if(harvest.isEmpty()){elapsed=0;return;}
        }
        tank.drain(STEP_FLUID,IFluidHandler.FluidAction.EXECUTE);elapsed=0;
        if(mature){overflow.addAll(harvest);harvest.clear();stage=1;naturalCycle=false;flushHarvest();}else stage++;
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,worldPosition.getX()+.5,worldPosition.getY()+1.5,worldPosition.getZ()+.5,6,.25,.45,.25,.01);
        setChanged();
    }
    private void flushHarvest(){
        if(overflow.isEmpty())return;
        var next=overflow.getFirst();var old=items.getStackInSlot(2);
        if(!old.isEmpty()&&!ItemStack.isSameItemSameComponents(old,next))return;
        int amount=Math.min(next.getCount(),next.getMaxStackSize()-old.getCount());if(amount<=0)return;
        internal=true;try{items.setStackInSlot(2,next.copyWithCount(old.getCount()+amount));}finally{internal=false;}
        next.shrink(amount);if(next.isEmpty())overflow.removeFirst();setChanged();
    }
    public boolean filter(int slot,ItemStack item,FluidStack fluid){
        var tag=OrbUpgrades.settings(upgrades.getStackInSlot(slot));boolean match=false;
        for(int i=0;i<3;i++)if(OrbFilterMatcher.matches(level,ItemStack.parseOptional(level.registryAccess(),tag.getCompound("Filter"+i)),item,fluid))match=true;
        return match==tag.getBoolean("Whitelist");
    }
    public void dropContents(){
        if(level==null)return;for(int i=0;i<3;i++)Block.popResource(level,worldPosition,items.getStackInSlot(i));
        for(int i=0;i<2;i++)Block.popResource(level,worldPosition,upgrades.getStackInSlot(i));overflow.forEach(s->Block.popResource(level,worldPosition,s));
    }
    @Override public Component getDisplayName(){return CatalysisContent.BLOCK.get().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new CatalysisMenu(id,inv,this);}
    @Override public void setChanged(){super.setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.saveAdditional(tag,provider);tag.put("Items",items.serializeNBT(provider));tag.put("Upgrades",upgrades.serializeNBT(provider));tag.put("Fluid",tank.writeToNBT(provider,new CompoundTag()));
        tag.putInt("Stage",stage);tag.putInt("Elapsed",elapsed);
        tag.putInt("StemStage",stemStage);tag.putBoolean("StemReady",stemReady);
        tag.putBoolean("NaturalMode",naturalMode);tag.putBoolean("NaturalCycle",naturalCycle);tag.putInt("StageCount",stageCount);
        if(!boundSeed.isEmpty())tag.put("Seed",boundSeed.save(provider));if(!boundSoil.isEmpty())tag.put("Soil",boundSoil.save(provider));
        if(!boundFluid.isEmpty())tag.put("BoundFluid",boundFluid.save(provider));
        var pending=new ListTag();harvest.forEach(s->pending.add(s.save(provider)));tag.put("Harvest",pending);
        var out=new ListTag();overflow.forEach(s->out.add(s.save(provider)));tag.put("Overflow",out);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.loadAdditional(tag,provider);internal=true;
        visualUpdatedAt=level==null?0:level.getGameTime();
        try{items.deserializeNBT(provider,tag.getCompound("Items"));upgrades.deserializeNBT(provider,tag.getCompound("Upgrades"));tank.readFromNBT(provider,tag.getCompound("Fluid"));}
        finally{internal=false;}
        stage=Math.clamp(tag.getInt("Stage"),1,64);elapsed=Math.clamp(tag.getInt("Elapsed"),0,STEP_TICKS);
        stemStage=Math.clamp(tag.getInt("StemStage"),1,8);stemReady=tag.getBoolean("StemReady");
        naturalMode=tag.getBoolean("NaturalMode");naturalCycle=tag.getBoolean("NaturalCycle");stageCount=tag.contains("StageCount")?Math.clamp(tag.getInt("StageCount"),2,64):stageCountFromSaved();
        boundSeed=ItemStack.parseOptional(provider,tag.getCompound("Seed"));boundSoil=ItemStack.parseOptional(provider,tag.getCompound("Soil"));boundFluid=FluidStack.parseOptional(provider,tag.getCompound("BoundFluid"));
        harvest.clear();for(var t:tag.getList("Harvest",10)){var s=ItemStack.parseOptional(provider,(CompoundTag)t);if(!s.isEmpty())harvest.add(s);}
        overflow.clear();for(var t:tag.getList("Overflow",10)){var s=ItemStack.parseOptional(provider,(CompoundTag)t);if(!s.isEmpty())overflow.add(s);}
    }
    private int stageCountFromSaved(){var plan=plan();return plan==null?4:plan.stages();}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider){return saveWithoutMetadata(provider);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
