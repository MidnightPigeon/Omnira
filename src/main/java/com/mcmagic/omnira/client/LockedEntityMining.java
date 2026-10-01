package com.mcmagic.omnira.client;
import com.mcmagic.omnira.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class LockedEntityMining {
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        var player=Minecraft.getInstance().player;
        if(player!=null && player.hasEffect(ModEffects.CONSTRUCTION_LOCK)) {event.setCanceled(true);event.setSwingHand(false);}
    }
}
