package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.item.bottle.PocketBottleItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerMenu.class)
abstract class BottleMenuMixin {
    // Covers clicks, hotbar swaps, drag placement and shift transfers, including custom Slot subclasses.
    @Redirect(method={"doClick", "moveItemStackTo"}, at=@At(value="INVOKE",
            target="Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean omnira$allowPlacement(Slot slot,ItemStack stack) {
        return com.mcmagic.omnira.item.PortableStorageRules.mayPlace(slot.container,stack) && slot.mayPlace(stack);
    }
}
