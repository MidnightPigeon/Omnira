package com.mcmagic.omnira.pulse;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Menu and automation access the same shared slot; block saves contain only the local migration slot. */
public final class PulseOrbUpgrade extends ItemStackHandler {
    private final PulseOrbEntity orb;
    PulseOrbUpgrade(PulseOrbEntity orb){super(1);this.orb=orb;}
    private ItemStackHandler target(){return orb.getLevel() instanceof net.minecraft.server.level.ServerLevel level?PulseOrbGroups.get(level).view(orb):this;}
    ItemStack local(){return super.getStackInSlot(0);}
    void local(ItemStack stack){super.setStackInSlot(0,stack);}
    @Override public int getSlotLimit(int slot){return 1;}
    @Override public boolean isItemValid(int slot,ItemStack stack){return orb.accepts(stack);}
    @Override public ItemStack getStackInSlot(int slot){var target=target();return target==this?super.getStackInSlot(slot):target.getStackInSlot(slot);}
    @Override public void setStackInSlot(int slot,ItemStack stack){var target=target();if(target==this)super.setStackInSlot(slot,stack);else target.setStackInSlot(slot,stack);}
    @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(!isItemValid(slot,stack))return stack;var target=target();return target==this?super.insertItem(slot,stack,simulate):target.insertItem(slot,stack,simulate);}
    @Override public ItemStack extractItem(int slot,int amount,boolean simulate){var target=target();return target==this?super.extractItem(slot,amount,simulate):target.extractItem(slot,amount,simulate);}
    @Override protected void onContentsChanged(int slot){orb.sync();PulseNetworks.orbChanged(orb);}
}
