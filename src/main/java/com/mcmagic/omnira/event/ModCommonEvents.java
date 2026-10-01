package com.mcmagic.omnira.event;

import com.mcmagic.omnira.Omnira;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@EventBusSubscriber(modid = Omnira.MOD_ID)
public final class ModCommonEvents {
    private ModCommonEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        Omnira.LOGGER.debug("Omnira server hooks are ready.");
    }
}
