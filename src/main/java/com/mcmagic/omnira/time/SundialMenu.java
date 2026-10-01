package com.mcmagic.omnira.time;

import com.mcmagic.omnira.reversal.ReversalBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class SundialMenu extends AbstractContainerMenu {
    private final Container storage;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final SundialBlockEntity machine;

    public SundialMenu(int id,Inventory inventory,RegistryFriendlyByteBuf buffer){
        this(id,inventory,new SimpleContainer(3),ContainerLevelAccess.create(inventory.player.level(),buffer.readBlockPos()),new SimpleContainerData(3),null);
    }
    public SundialMenu(int id,Inventory inventory,SundialBlockEntity machine){
        this(id,inventory,machine,ContainerLevelAccess.create(machine.getLevel(),machine.getBlockPos()),new ContainerData(){
            public int get(int i){return i==0?machine.energy():i==1?machine.progress():machine.selection();}
            public void set(int i,int v){}
            public int getCount(){return 3;}
        },machine);
    }
    private SundialMenu(int id,Inventory inventory,Container storage,ContainerLevelAccess access,ContainerData data,SundialBlockEntity machine){
        super(SundialContent.MENU.get(),id);this.storage=storage;this.access=access;this.data=data;this.machine=machine;
        addDataSlots(data);
        addSlot(new Slot(storage,0,SundialLayout.INPUT_X,SundialLayout.INPUT_Y){
            @Override public boolean mayPlace(ItemStack stack){return (machine==null||machine.progress()==0)&&!SundialTimeline.choices(stack).isEmpty();}
            @Override public boolean mayPickup(Player player){return machine==null||machine.progress()==0;}
        });
        addSlot(new Slot(storage,1,SundialLayout.OUTPUT_X,SundialLayout.OUTPUT_Y){@Override public boolean mayPlace(ItemStack stack){return false;}});
        addSlot(new Slot(storage,2,SundialLayout.FUEL_X,SundialLayout.FUEL_Y){@Override public boolean mayPlace(ItemStack stack){return ReversalBlockEntity.fuel(stack)>0;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,SundialLayout.INVENTORY_X+18*col,SundialLayout.INVENTORY_Y+18*row));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,SundialLayout.INVENTORY_X+18*col,SundialLayout.HOTBAR_Y));
    }
    public int energy(){return data.get(0);}
    public int progress(){return data.get(1);}
    public ItemStack selectedOutput(){
        var choices=SundialTimeline.choices(storage.getItem(0));
        return choices.isEmpty()?ItemStack.EMPTY:choices.get(Math.floorMod(data.get(2),choices.size()));
    }
    public boolean stillValid(Player player){return stillValid(access,player,SundialContent.BLOCK.get());}
    @Override public boolean clickMenuButton(Player player,int id){
        if(machine==null)return false;
        if(id==0){machine.cycle(1);return true;}
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;
        var copy=stack.copy();
        if(index<3){if(!slot.mayPickup(player)||!moveItemStackTo(stack,3,39,true))return ItemStack.EMPTY;}
        else if(slots.get(0).mayPlace(stack)){
            if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        }else if(!moveItemStackTo(stack,2,3,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return copy;
    }
}
