package com.mcmagic.omnira.recipe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Destructive recipes must not consume inventories or serialized machine/orb state. */
public final class RecipeInputSafety {
    public static boolean empty(ItemStack stack) {
        return !stack.has(DataComponents.BLOCK_ENTITY_DATA)
                && stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .stream().allMatch(ItemStack::isEmpty);
    }

    public static ItemStack upgradePickaxe(ItemStack source, ItemStack result) {
        var upgraded = result.copy();
        upgraded.applyComponents(source.getComponentsPatch());
        return upgraded;
    }

    private RecipeInputSafety() {}
}
