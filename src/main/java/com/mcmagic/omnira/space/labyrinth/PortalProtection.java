package com.mcmagic.omnira.space.labyrinth;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid="omnira")
public final class PortalProtection {
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent event){
        if(event.getState().getBlock() instanceof VoidPortalBlock)event.setCanceled(true);
    }
    @SubscribeEvent public static void mining(PlayerInteractEvent.LeftClickBlock event){
        if(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof VoidPortalBlock)event.setCanceled(true);
    }
    private PortalProtection(){}
}
