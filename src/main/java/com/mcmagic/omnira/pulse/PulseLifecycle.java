package com.mcmagic.omnira.pulse;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class PulseLifecycle {
    @net.neoforged.bus.api.SubscribeEvent public static void beforeTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))com.mcmagic.omnira.compat.create.PulseOrbKinetics.maintain(event.getServer());
        PulseChanneling.tick(event.getServer());
        PulseNetworks.updatePlayers(event.getServer(),event.getServer().getPlayerList().getPlayers());
    }
    @net.neoforged.bus.api.SubscribeEvent public static void afterTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        if(event.getServer().getTickCount()%20==0){
            PulseNetworks.updatePlayers(event.getServer(),event.getServer().getPlayerList().getPlayers());
            PulseNetworks.absorbPlayers(event.getServer());
        }
    }
    @net.neoforged.bus.api.SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){if(net.neoforged.fml.ModList.get().isLoaded("create"))com.mcmagic.omnira.compat.create.PulseOrbKinetics.clear(event.getServer());PulseChanneling.clear(event.getServer());PulseNetworks.clear(event.getServer());}
    private PulseLifecycle(){}
}
