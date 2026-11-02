package com.mcmagic.omnira.block.entity;


import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class SimpleCondensationTableBlockEntity extends CrystalProcessingTableBlockEntity {
    public final com.mcmagic.omnira.workstation.CondensationWork work=new com.mcmagic.omnira.workstation.CondensationWork(this);
    private boolean clientWorking;
    private long clientStarted;
    private long tailStarted,tailStopped=Long.MIN_VALUE;
    public long condensationTailStarted(){return tailStarted;}
    public long condensationTailStopped(){return tailStopped;}
    public boolean condensationTailVisible(double time){return tailStopped!=Long.MIN_VALUE&&time-tailStopped<28;}
    public boolean condensationVisible(){return condensing()||level!=null&&condensationTailVisible(level.getGameTime());}
    public boolean condensing(){return level!=null&&level.isClientSide?clientWorking:work.active();}
    public long condensationStarted(){return level!=null&&level.isClientSide?clientStarted:work.started();}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){
        var tag=super.getUpdateTag(registries);
        tag.putBoolean("Condensing",work.active());tag.putLong("CondensationStarted",work.started());return tag;
    }
    @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);
        boolean working=tag.getBoolean("Condensing");long started=tag.getLong("CondensationStarted");
        if(level!=null&&level.isClientSide&&clientWorking&&(!working||started!=clientStarted)){
            tailStarted=clientStarted;tailStopped=level.getGameTime();
        }
        clientWorking=working;clientStarted=started;
    }
    public SimpleCondensationTableBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.SIMPLE_CONDENSATION_TABLE.get(),pos,state,1);}
    protected SimpleCondensationTableBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) {super(type,pos,state,1);}
    @Override public int getContainerSize() {return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {return false;}
    @Override protected Component getDefaultName() {return Component.translatable("container.omnira.simple_condensation_table");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) {return null;}
}
