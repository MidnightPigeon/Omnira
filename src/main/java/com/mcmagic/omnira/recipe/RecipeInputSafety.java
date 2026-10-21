package com.mcmagic.omnira.recipe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Destructive recipes must not consume inventories, captured entities or serialized machine/orb state. */
public final class RecipeInputSafety {
    public static boolean empty(ItemStack stack) {
        return !com.mcmagic.omnira.item.bottle.PocketBottleItem.filled(stack)
                && !stack.has(DataComponents.BLOCK_ENTITY_DATA)
                && !(stack.is(com.mcmagic.omnira.registry.ModItems.CRUISE_ORB.get())&&stack.has(DataComponents.CUSTOM_DATA))
                && stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, net.minecraft.world.item.component.BundleContents.EMPTY)
                    .isEmpty()
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
