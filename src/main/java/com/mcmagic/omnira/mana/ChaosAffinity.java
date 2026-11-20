package com.mcmagic.omnira.mana;

import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;
import java.util.function.Consumer;

/** Paid casts may enqueue one independent, free echo; echoes never enter this lottery. */
@EventBusSubscriber(modid="omnira")
public final class ChaosAffinity {
    public static final int ECHO_DELAY=4;
    private record Echo(int due,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                        Consumer<ServerPlayer> action) {}
    private static final Map<UUID,List<Echo>> ECHOES=new HashMap<>();

    public static boolean active(Player player) {
        return player.getData(ModAttachments.AFFINITY).active()==Affinity.CHAOS;
    }

    public static void echo(Player player,Consumer<ServerPlayer> action) {
        if(player instanceof ServerPlayer p && p.isAlive() && !p.isSpectator() && active(p)
                && p.getRandom().nextFloat()<.25F)
            ECHOES.computeIfAbsent(p.getUUID(),key->new ArrayList<>())
                    .add(new Echo(p.tickCount+ECHO_DELAY,p.level().dimension(),action));
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var echoes=ECHOES.remove(player.getUUID());
        if(echoes==null || !player.isAlive() || player.isSpectator())return;
        for(var echo:echoes){
            if(echo.dimension!=player.level().dimension())continue;
            if(player.tickCount-echo.due<0)
                ECHOES.computeIfAbsent(player.getUUID(),key->new ArrayList<>()).add(echo);
            else if(player.isAlive())echo.action.accept(player);
        }
    }

    public static void retribution(ServerPlayer player) {
        var random=player.getRandom();
        double y=random.nextDouble()*2-1,angle=random.nextDouble()*Math.PI*2;
        double horizontal=Math.sqrt(1-y*y);
        var direction=new Vec3(horizontal*Math.cos(angle),y,horizontal*Math.sin(angle));
        var center=player.getBoundingBox().getCenter();
        var start=center.add(direction.scale(3));
        com.mcmagic.omnira.weaver.WeaverCombat.retributionRay(player.serverLevel(),start,direction.scale(-1));
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){ECHOES.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){ECHOES.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void died(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event){ECHOES.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){ECHOES.clear();}
    private ChaosAffinity(){}
}
