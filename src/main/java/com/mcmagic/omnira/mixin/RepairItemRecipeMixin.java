package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.item.AnalysisCrystalItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin extends CustomRecipe {
    protected RepairItemRecipeMixin(CraftingBookCategory category) {super(category);}

    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remaining=super.getRemainingItems(input);
        // Repair consumes its tools; analysis recipes still return a worn catalyst.
        for(int i=0;i<input.size();i++)if(input.getItem(i).getItem() instanceof AnalysisCrystalItem)
            remaining.set(i,ItemStack.EMPTY);
        return remaining;
    }
}
