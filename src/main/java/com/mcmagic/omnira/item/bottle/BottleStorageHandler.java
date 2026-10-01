package com.mcmagic.omnira.item.bottle;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** Rejects insertion before a storage provider can mutate either inventory; extraction is untouched. */
public class BottleStorageHandler implements IItemHandler {
    protected final IItemHandler delegate;
    private BottleStorageHandler(IItemHandler delegate) {this.delegate=delegate;}
    public static IItemHandler unwrap(IItemHandler handler){return handler instanceof BottleStorageHandler wrapped?wrapped.delegate:handler;}
    public static IItemHandler wrap(IItemHandler handler) {
        if(handler instanceof BottleStorageHandler) return handler;
        return handler instanceof IItemHandlerModifiable modifiable?new Modifiable(modifiable):new BottleStorageHandler(handler);
    }
    @Override public int getSlots() {return delegate.getSlots();}
    @Override public ItemStack getStackInSlot(int slot) {return delegate.getStackInSlot(slot);}
    @Override public int getSlotLimit(int slot) {return delegate.getSlotLimit(slot);}
    private boolean allowed(ItemStack stack){
        return delegate instanceof net.neoforged.neoforge.items.wrapper.InvWrapper inv
                ?com.mcmagic.omnira.item.PortableStorageRules.mayPlace(inv.getInv(),stack):!PocketBottleItem.restricted(stack);
    }
    @Override public boolean isItemValid(int slot,ItemStack stack) {return allowed(stack) && delegate.isItemValid(slot,stack);}
    @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
        return allowed(stack)?delegate.insertItem(slot,stack,simulate):stack;
    }
    @Override public ItemStack extractItem(int slot,int amount,boolean simulate) {return delegate.extractItem(slot,amount,simulate);}
    private static final class Modifiable extends BottleStorageHandler implements IItemHandlerModifiable {
        private final IItemHandlerModifiable modifiable;
        private Modifiable(IItemHandlerModifiable delegate) {super(delegate);modifiable=delegate;}
        // Direct setters are also used for loading and extraction. Do not silently destroy legacy contents.
        @Override public void setStackInSlot(int slot,ItemStack stack) {modifiable.setStackInSlot(slot,stack);}
    }
}
