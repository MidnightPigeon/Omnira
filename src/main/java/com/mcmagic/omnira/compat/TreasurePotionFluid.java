package com.mcmagic.omnira.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;

/** Create owns potion-fluid component encoding; never fabricate a bare potion fluid. */
public final class TreasurePotionFluid {
    public static FluidStack prepare(FluidStack stack, net.minecraft.util.RandomSource random) {
        if(!BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().equals("create:potion"))return stack;
        var potions=BuiltInRegistries.POTION.holders().filter(p->!p.value().getEffects().isEmpty()).toList();
        if(potions.isEmpty())return FluidStack.EMPTY;
        var item=PotionContents.createItemStack(Items.POTION,potions.get(random.nextInt(potions.size())));
        var result=com.simibubi.create.content.fluids.potion.PotionFluidHandler.getFluidFromPotionItem(item);
        result.setAmount(stack.getAmount());return result;
    }
    private TreasurePotionFluid(){}
}
