package com.mcmagic.omnira.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AnalysisCrystalItem extends Item {
    public AnalysisCrystalItem() { super(new Properties().durability(128)); }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (stack.getDamageValue() + 1 >= stack.getMaxDamage()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copyWithCount(1);
        remainder.setDamageValue(stack.getDamageValue() + 1);
        return remainder;
    }
}
