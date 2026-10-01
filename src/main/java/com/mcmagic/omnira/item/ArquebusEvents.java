package com.mcmagic.omnira.item;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.menu.ArquebusMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class ArquebusEvents {
    public static final int SHOT_INTERVAL=6;
    private record FollowUp(ItemStack gun,ItemStack snapshot,ResourceKey<Level> dimension,long due) {}
    private static final Map<UUID,FollowUp> SHOTS=new HashMap<>();
    private static final Map<UUID,Long> SENSE=new HashMap<>();
    private static final Map<UUID,Long> CONCEAL=new HashMap<>();
    public static final double SENSE_RADIUS=36;
    private ArquebusEvents() {}
    public static void queue(ServerPlayer player,ItemStack gun) {
        SHOTS.remove(player.getUUID());
        if(ArquebusPlugin.of(gun).shots==2) SHOTS.put(player.getUUID(),new FollowUp(gun,gun.copy(),
                player.level().dimension(),player.level().getGameTime()+SHOT_INTERVAL));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;
        senseTick(player);
        concealTick(player);
        var shot=SHOTS.get(player.getUUID());if(shot==null)return;
        if(!player.isAlive() || player.isSpectator() || player.getMainHandItem()!=shot.gun
                || !ItemStack.matches(shot.snapshot,shot.gun) || player.containerMenu instanceof ArquebusMenu
                || !player.level().dimension().equals(shot.dimension)) {SHOTS.remove(player.getUUID());return;}
        if(player.level().getGameTime()<shot.due)return;
        SHOTS.remove(player.getUUID());
        ArcaneArquebusItem.fire(player,shot.gun);
    }
    public static void senseTick(ServerPlayer player) {
        var gun=player.getMainHandItem();
        if(!player.isAlive() || player.isSpectator() || !(gun.getItem() instanceof ArcaneArquebusItem)
                || ArquebusPlugin.of(gun)!=ArquebusPlugin.KINGS_NEW_CLOTHES) {SENSE.remove(player.getUUID());return;}
        long now=player.level().getGameTime();
        long due=SENSE.computeIfAbsent(player.getUUID(),id->now+100);
        if(now<due)return;
        SENSE.put(player.getUUID(),now+100);
        var mana=player.getData(com.mcmagic.omnira.registry.ModAttachments.MANA);
        if(!mana.canSpend(50))return;
        player.setData(com.mcmagic.omnira.registry.ModAttachments.MANA,mana.spend(50));
        var ids=player.level().getEntities(player,player.getBoundingBox().inflate(SENSE_RADIUS),
                e->com.mcmagic.omnira.spell.SpellCasting.validTarget(e) && e.distanceToSqr(player)<=SENSE_RADIUS*SENSE_RADIUS)
                .stream().map(net.minecraft.world.entity.Entity::getUUID).toList();
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new com.mcmagic.omnira.network.GhostSensePayload(ids));
    }
    public static void concealTick(ServerPlayer player) {
        var gun=player.getMainHandItem();
        if(!player.isAlive() || player.isSpectator() || !(gun.getItem() instanceof ArcaneArquebusItem)
                || ArquebusPlugin.of(gun)!=ArquebusPlugin.ANCESTOR_LAUNCHER) {CONCEAL.remove(player.getUUID());return;}
        long now=player.level().getGameTime();
        long due=CONCEAL.computeIfAbsent(player.getUUID(),id->now+60);
        if(now<due)return;
        CONCEAL.put(player.getUUID(),now+60);
        var mana=player.getData(com.mcmagic.omnira.registry.ModAttachments.MANA);
        if(!mana.canSpend(10))return;
        player.setData(com.mcmagic.omnira.registry.ModAttachments.MANA,mana.spend(10));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,100,0,false,false,true));
    }
    private static void clear(UUID id) {SHOTS.remove(id);SENSE.remove(id);CONCEAL.remove(id);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {clear(e.getEntity().getUUID());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {clear(e.getEntity().getUUID());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) {clear(e.getEntity().getUUID());}
    @SubscribeEvent public static void stop(ServerStoppedEvent e) {SHOTS.clear();SENSE.clear();CONCEAL.clear();}
}
