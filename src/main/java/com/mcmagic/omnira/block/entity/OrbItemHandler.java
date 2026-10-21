package com.mcmagic.omnira.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class OrbItemHandler implements IItemHandler {
    private final CrystalBallBlockEntity owner;
    private final boolean limited;
    public OrbItemHandler(CrystalBallBlockEntity owner) {this(owner,true);}
    public OrbItemHandler(CrystalBallBlockEntity owner,boolean limited) {this.owner=owner;this.limited=limited;}
    public int getSlots(){return owner instanceof LiquidCrystalBallBlockEntity?0:owner.getContainerSize();}
    public ItemStack getStackInSlot(int slot){return owner.getItem(slot);}
    public int getSlotLimit(int slot){return owner.getMaxStackSize();}
    public boolean isItemValid(int slot,ItemStack stack){return slot>=0 && slot<getSlots() && (owner instanceof VoidCrystalBallBlockEntity || !com.mcmagic.omnira.item.bottle.PocketBottleItem.restricted(stack));}
    public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
        if(owner.hasPendingLoot())return stack;
        if(stack.isEmpty() || !isItemValid(slot,stack))return stack;
        if(owner instanceof VoidCrystalBallBlockEntity){if(!simulate&&!com.mcmagic.omnira.progression.BranchStorage.manual())com.mcmagic.omnira.progression.BranchProgress.automated(owner,stack.getCount());return ItemStack.EMPTY;}
        var existing=owner.getItem(slot);
        if(!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing,stack))return stack;
        int amount=Math.min(stack.getCount(),owner.getMaxStackSize(stack)-existing.getCount());
        if(amount<=0)return stack;
        if(!simulate){owner.setItem(slot,stack.copyWithCount(existing.getCount()+amount));
            if(!com.mcmagic.omnira.progression.BranchStorage.manual())com.mcmagic.omnira.progression.BranchProgress.automated(owner,amount);}
        return stack.copyWithCount(stack.getCount()-amount);
    }
    public ItemStack extractItem(int slot,int amount,boolean simulate) {
        if(slot<0 || slot>=getSlots() || amount<=0)return ItemStack.EMPTY;
        boolean capped=limited&&!com.mcmagic.omnira.storage.LogisticsRates.internal();
        var stack=owner.getItem(slot);amount=Math.min(amount,Math.min(stack.getCount(),capped?owner.outputItems():Integer.MAX_VALUE));
        if(simulate)return stack.copyWithCount(amount);
        var result=owner.removeItem(slot,amount);if(capped)owner.outputItems(result.getCount());owner.setChanged();
        if(!com.mcmagic.omnira.progression.BranchStorage.manual())com.mcmagic.omnira.progression.BranchProgress.automated(owner,result.getCount());return result;
    }
}
