package com.mcmagic.omnira.item.bottle;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid="omnira")
public final class BottleEvents {
    @SubscribeEvent public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if(!(event.getItemStack().getItem() instanceof PocketBottleItem) || (!PocketBottleItem.filled(event.getItemStack()) && !event.getEntity().isShiftKeyDown())) return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if(!event.getLevel().isClientSide) event.getItemStack().getItem().use(event.getLevel(),event.getEntity(),event.getHand());
    }
    @SubscribeEvent public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if(!(event.getItemStack().getItem() instanceof PocketBottleItem) || (!PocketBottleItem.filled(event.getItemStack()) && !event.getEntity().isShiftKeyDown())) return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if(!event.getLevel().isClientSide) event.getItemStack().getItem().use(event.getLevel(),event.getEntity(),event.getHand());
    }
    @SubscribeEvent public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        var stack=event.getItemStack();
        if(!stack.is(ModItems.POCKET_MAGIC_BOTTLE.get())) return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if(event.getLevel().isClientSide) return;
        if(PocketBottleItem.filled(stack) || event.getEntity().isShiftKeyDown()) stack.getItem().use(event.getLevel(),event.getEntity(),event.getHand());
        else PocketBottleItem.capture(event.getEntity(),event.getTarget(),stack);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event) {
        var stack=event.getItemStack();
        if(!stack.is(ModItems.POCKET_MAGIC_BOTTLE.get())) return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if(event.getLevel().isClientSide) return;
        if(PocketBottleItem.filled(stack) || event.getEntity().isShiftKeyDown()) stack.getItem().use(event.getLevel(),event.getEntity(),event.getHand());
        else PocketBottleItem.capture(event.getEntity(),event.getTarget(),stack);
    }
}
