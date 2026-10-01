package com.mcmagic.omnira.item;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class SwordFocus {
    public static final int DURATION=60;
    public static final float SEGMENT_HEAL=4;
    public static final float COMPLETE_HEAL=12;
    private static final Map<Player,Channel> CHANNELS=java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private SwordFocus() {}

    private static final class Channel {
        final ItemStack stack;
        final RitualSwordItem.Kind kind;
        final double cost;
        final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        final Supplier<Vec3> anchor;
        int elapsed;
        double paid;
        Channel(Player player,ItemStack stack,RitualSwordItem.Kind kind) {
            this.stack=stack;this.kind=kind;cost=player.getAttributeValue(ModAttributes.MAX_MANA)*.25;
            dimension=player.level().dimension();
            Vec3 position=player.position();
            Supplier<Vec3> local=kind==RitualSwordItem.Kind.NEEDLE && ModList.get().isLoaded("sable")
                    ?com.mcmagic.omnira.compat.SableSwordFocus.capture(player):null;
            anchor=local==null?()->position:local;
        }
    }
    public static boolean canBegin(Player player,RitualSwordItem.Kind kind) {
        if(!allowed(player,kind) || player.getHealth()>=player.getMaxHealth())return false;
        double cost=player.getAttributeValue(ModAttributes.MAX_MANA)*.25;
        return player.getData(ModAttachments.MANA).canSpend(kind==RitualSwordItem.Kind.NAIL?cost/3:cost);
    }
    private static boolean allowed(Player player,RitualSwordItem.Kind kind) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger()
                && (kind==RitualSwordItem.Kind.NEEDLE || (player.onGround() && !player.getAbilities().flying && !player.isFallFlying()));
    }
    public static void begin(Player player,ItemStack stack,RitualSwordItem.Kind kind) {
        CHANNELS.put(player,new Channel(player,stack,kind));
    }
    public static boolean stop(Player player) {return CHANNELS.remove(player)!=null;}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {stop(e.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {cancel(e.getEntity());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) {stop(e.getEntity());}
    @SubscribeEvent public static void shutdown(ServerStoppedEvent e) {CHANNELS.clear();}
    private static void cancel(Player player) {stop(player);player.stopUsingItem();}
    public static void advance(ServerPlayer player,ItemStack stack,int elapsed) {
        Channel channel=CHANNELS.get(player);
        if(channel==null || channel.stack!=stack || !allowed(player,channel.kind) || player.level().dimension()!=channel.dimension) {
            cancel(player);return;
        }
        if(elapsed<=channel.elapsed || elapsed>DURATION)return;
        channel.elapsed=elapsed;
        boolean nail=channel.kind==RitualSwordItem.Kind.NAIL;
        // Snapshot the maximum at channel start; both costs deliberately bypass discounts.
        double due=nail?channel.cost*(elapsed/20)/3:channel.cost*elapsed/DURATION;
        double payment=Math.max(0,due-channel.paid);
        var mana=player.getData(ModAttachments.MANA);
        if(payment>mana.current()+1.0E-7) {cancel(player);return;}
        if(payment>0)player.setData(ModAttachments.MANA,mana.spend(Math.min(payment,mana.current())));
        channel.paid=due;
        if(nail && elapsed%20==0)player.heal(SEGMENT_HEAL);
        if(!nail && elapsed==DURATION)player.heal(COMPLETE_HEAL);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        Player player=event.getEntity();
        Channel channel=CHANNELS.get(player);
        if(channel==null)return;
        boolean sameWeapon=player.level().isClientSide?player.getUseItem().is(channel.stack.getItem()):player.getUseItem()==channel.stack;
        if(!player.isUsingItem() || !sameWeapon || !allowed(player,channel.kind)
                || player.level().dimension()!=channel.dimension) {cancel(player);return;}
        if(channel.kind==RitualSwordItem.Kind.NEEDLE) {
            Vec3 position=channel.anchor.get();
            if(position==null) {cancel(player);return;}
            player.setPos(position);player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        }
    }
}
