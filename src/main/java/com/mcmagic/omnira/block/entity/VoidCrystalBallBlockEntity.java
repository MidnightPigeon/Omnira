package com.mcmagic.omnira.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Stateless sink: accepted input never becomes recoverable storage. */
public final class VoidCrystalBallBlockEntity extends CrystalBallBlockEntity {
    public VoidCrystalBallBlockEntity(BlockPos pos,BlockState state){super(pos,state);}
    @Override protected boolean shatterDrops(){return false;}
    @Override protected void shattered(net.minecraft.server.level.ServerLevel server){
        net.minecraft.world.level.block.Block.popResource(server,worldPosition,new ItemStack(com.mcmagic.omnira.registry.ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()));
    }
    public final IFluidHandler fluidHandler=new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int tank){return FluidStack.EMPTY;}
        public int getTankCapacity(int tank){return Integer.MAX_VALUE;}
        public boolean isFluidValid(int tank,FluidStack fluid){return tank==0&&!fluid.isEmpty();}
        public int fill(FluidStack fluid,FluidAction action){return fluid.getAmount();}
        public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.EMPTY;}
        public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
    };
    @Override public int getContainerSize(){return 1;}
    @Override public ItemStack getItem(int slot){return ItemStack.EMPTY;}
    @Override public void setItem(int slot,ItemStack stack){}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0;}
    @Override protected Component getDefaultName(){return Component.translatable("block.omnira.void_crystal_ball");}
    private int ticks;
    @Override public void serverTick(){
        if(level==null||level.isClientSide||++ticks%20!=0||!upgrades.has(com.mcmagic.omnira.item.OrbUpgradeItem.Kind.INTAKE))return;
        var tag=OrbUpgrades.settings(upgrades.getStackInSlot(1));
        var directions=OrbAutomation.directions(this,true);int mask=tag.contains("Directions")?tag.getInt("Directions"):1;
        int cursor=Math.floorMod(tag.getInt("Cursor"),directions.length);boolean itemTaken=false,fluidTaken=false;
        for(int n=0;n<directions.length;n++){
            int index=(cursor+n)%directions.length;if((mask&(1<<index))==0)continue;
            var direction=directions[index];var pos=worldPosition.relative(direction);if(!level.hasChunkAt(pos))continue;
            var items=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,direction.getOpposite());
            if(!itemTaken&&items!=null)for(int i=0;i<items.getSlots();i++){
                var offered=items.extractItem(i,1,true);
                if(!offered.isEmpty()&&upgrades.matches(1,offered,null)){itemTaken=!items.extractItem(i,1,false).isEmpty();break;}
            }
            var fluids=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,direction.getOpposite());
            if(!fluidTaken&&fluids!=null)for(int i=0;i<fluids.getTanks();i++){
                var offered=fluids.getFluidInTank(i);
                if(!offered.isEmpty()&&upgrades.matches(1,ItemStack.EMPTY,offered)){
                    fluidTaken=!fluids.drain(offered.copyWithAmount(500),IFluidHandler.FluidAction.EXECUTE).isEmpty();break;
                }
            }
            if(itemTaken||fluidTaken){tag.putInt("Cursor",(index+1)%directions.length);}
            if(itemTaken&&fluidTaken)break;
        }
        if(itemTaken||fluidTaken)upgrades.update(1,tag);
    }
}
