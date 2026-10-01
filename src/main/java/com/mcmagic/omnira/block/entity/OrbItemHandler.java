package com.mcmagic.omnira.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class OrbItemHandler implements IItemHandler {
    private final CrystalBallBlockEntity owner;
    public OrbItemHandler(CrystalBallBlockEntity owner) {this.owner=owner;}
    public int getSlots(){return owner instanceof LiquidCrystalBallBlockEntity?0:12;}
    public ItemStack getStackInSlot(int slot){return owner.getItem(slot);}
    public int getSlotLimit(int slot){return owner.getMaxStackSize();}
    public boolean isItemValid(int slot,ItemStack stack){return slot>=0 && slot<getSlots() && !com.mcmagic.omnira.item.bottle.PocketBottleItem.restricted(stack);}
    public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
        if(owner.hasPendingLoot())return stack;
        if(stack.isEmpty() || !isItemValid(slot,stack))return stack;
        var existing=owner.getItem(slot);
        if(!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing,stack))return stack;
        int amount=Math.min(stack.getCount(),owner.getMaxStackSize(stack)-existing.getCount());
        if(amount<=0)return stack;
        if(!simulate)owner.setItem(slot,stack.copyWithCount(existing.getCount()+amount));
        return stack.copyWithCount(stack.getCount()-amount);
    }
    public ItemStack extractItem(int slot,int amount,boolean simulate) {
        if(slot<0 || slot>=getSlots() || amount<=0)return ItemStack.EMPTY;
        var stack=owner.getItem(slot);amount=Math.min(amount,stack.getCount());
        if(simulate)return stack.copyWithCount(amount);
        var result=owner.removeItem(slot,amount);owner.setChanged();return result;
    }
}
