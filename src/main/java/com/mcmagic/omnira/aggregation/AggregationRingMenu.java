package com.mcmagic.omnira.aggregation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class AggregationRingMenu extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    public AggregationRingMenu(int id,Inventory player,RegistryFriendlyByteBuf buf){
        this(id,player,new SimpleContainer(AggregationLayout.SLOTS),ContainerLevelAccess.create(player.player.level(),buf.readBlockPos()),new SimpleContainerData(2));
    }
    public AggregationRingMenu(int id,Inventory player,AggregationRingBlockEntity machine){
        this(id,player,machine,ContainerLevelAccess.create(machine.getLevel(),machine.getBlockPos()),new ContainerData(){
            public int get(int i){return i==0?machine.progress():machine.manaCost();}public void set(int i,int v){}public int getCount(){return 2;}
        });
    }
    private AggregationRingMenu(int id,Inventory player,Container inventory,ContainerLevelAccess access,ContainerData data){
        super(AggregationContent.MENU.get(),id);this.inventory=inventory;this.access=access;this.data=data;addDataSlots(data);
        for(int i=0;i<AggregationLayout.SLOTS;i++){
            final int index=i;var xy=AggregationLayout.UI[i];
            addSlot(new Slot(inventory,i,xy[0],xy[1]){
                @Override public boolean mayPlace(ItemStack stack){return progress()==0&&AggregationRingBlockEntity.accepts(index,stack)&&com.mcmagic.omnira.spell.SpellPattern.compatibleSlots(inventory,index,stack);}
                @Override public boolean mayPickup(Player player){return progress()==0;}
                @Override public int getMaxStackSize(){return index==AggregationLayout.CORE?1:super.getMaxStackSize();}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,9+row*9+col,15+col*18,190+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(player,col,15+col*18,248));
    }
    public int progress(){return data.get(0);}
    public int manaCost(){return data.get(1);}
    @Override public boolean stillValid(Player player){return stillValid(access,player,AggregationContent.BLOCK.get());}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty()||!slot.mayPickup(player))return ItemStack.EMPTY;
        var copy=stack.copy();boolean moved=false;
        if(index<AggregationLayout.SLOTS)moved=moveItemStackTo(stack,AggregationLayout.SLOTS,slots.size(),true);
        else if(progress()==0){
            int[][] ranges=com.mcmagic.omnira.menu.CrystalProcessingTableMenu.isTargetMicrocore(stack)?new int[][]{{0,2},{2,5},{5,8}}:
                    new int[][]{{8,11},{2,5},{5,8}};
            for(var range:ranges)if(moveItemStackTo(stack,range[0],range[1],false)){moved=true;break;}
        }
        if(!moved)return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
