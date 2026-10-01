package com.mcmagic.omnira.shop;

import com.mcmagic.omnira.network.MarisaGoodsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

@EventBusSubscriber(modid="omnira")
public final class MarisaTradeSync {
    private static final Map<ServerPlayer,Long> SENT=new WeakHashMap<>();
    private MarisaTradeSync(){}
    private static void send(ServerPlayer player){
        var stock=MarisaTradeLedger.get(player.serverLevel()).stock(player.serverLevel(),player.getUUID());
        PacketDistributor.sendToPlayer(player,new MarisaGoodsPayload(stock.day(),stock.offers().stream().map(o->o.getResult()).toList()));
        SENT.put(player,stock.day());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p)send(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)SENT.remove(p);}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer p&&p.tickCount%20==0
                &&!Objects.equals(SENT.get(p),MarisaTradeLedger.day(p.serverLevel())))send(p);
    }
}
