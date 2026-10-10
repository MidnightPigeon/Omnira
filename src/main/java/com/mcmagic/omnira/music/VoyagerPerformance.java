package com.mcmagic.omnira.music;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

@EventBusSubscriber(modid="omnira")
public final class VoyagerPerformance {
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    public static final int[] TRACK_TICKS={3630,5421};
    private static final class Session {
        final ItemStack instrument;
        int age,track,remaining;
        Session(Player p){instrument=p.getOffhandItem();track=p.getRandom().nextInt(2);remaining=TRACK_TICKS[track];}
    }
    private VoyagerPerformance() {}
    public static boolean playing(Player p){return SESSIONS.containsKey(p.getUUID());}
    public static boolean eligible(Player p){return p.isAlive()&&!p.isSpectator()&&p.getMainHandItem().isEmpty()
            &&p.getOffhandItem().is(ModItems.VOYAGER_VIOLIN.get());}
    public static void toggle(Player p){
        if(p.level().isClientSide)return;
        if(playing(p)){stop(p);return;}
        if(!eligible(p))return;
        var session=new Session(p);SESSIONS.put(p.getUUID(),session);send(p,session.track);
    }
    public static void stop(Player p){if(SESSIONS.remove(p.getUUID())!=null)send(p,-1);}
    private static void send(Player p,int track){
        if(p instanceof ServerPlayer server&&server.connection!=null)
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new ViolinPayload(p.getUUID(),track));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!event.getEntity().level().isClientSide)advance(event.getEntity());
    }
    public static void advance(Player p){
        var session=SESSIONS.get(p.getUUID());if(session==null)return;
        if(!eligible(p)||p.getOffhandItem()!=session.instrument){stop(p);return;}
        session.age++;
        // Regeneration II heals every 25 ticks; refreshing on the same cadence preserves its rhythm.
        if(session.age%25==1) {
            affect(p);
            for(var living:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(10),
                    e->e!=p&&e.isAlive()&&!e.isSpectator()&&e.distanceToSqr(p)<=100))affect(living);
        }
        if(session.age%20==0)repairCarried(p);
        if(--session.remaining<=0){session.track=1-session.track;session.remaining=TRACK_TICKS[session.track];send(p,session.track);}
    }
    private static void affect(LivingEntity e){
        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION,50,1,true,false,true));
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,50,1,true,false,true));
    }
    public static void repairCarried(Player p){
        Set<ItemStack> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        for(int i=0;i<p.getInventory().getContainerSize();i++)repair(p.getInventory().getItem(i),seen);
        repair(p.containerMenu.getCarried(),seen);
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).ifPresent(inv->inv.getCurios().values().forEach(handler->{
            for(int i=0;i<handler.getSlots();i++){
                repair(handler.getStacks().getStackInSlot(i),seen);repair(handler.getCosmeticStacks().getStackInSlot(i),seen);
            }
        }));
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();
    }
    private static void repair(ItemStack stack,Set<ItemStack> seen){
        repair(stack,seen,0);
    }
    private static void repair(ItemStack stack,Set<ItemStack> seen,int depth){
        if(stack.isEmpty()||!seen.add(stack)||depth>16)return;
        if(stack.isDamaged())stack.setDamageValue(Math.max(0,stack.getDamageValue()-5));
        var contents=stack.get(net.minecraft.core.component.DataComponents.CONTAINER);
        if(contents!=null){
            var items=contents.stream().toList();items.forEach(s->repair(s,seen,depth+1));
            stack.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(items));
        }
        var bundle=stack.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);
        if(bundle!=null){
            var items=bundle.itemCopyStream().toList();items.forEach(s->repair(s,seen,depth+1));
            stack.set(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS,new net.minecraft.world.item.component.BundleContents(items));
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){stop(e.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){stop(e.getEntity());}
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e){
        if(e.getTarget() instanceof Player p&&e.getEntity() instanceof ServerPlayer watcher){
            var s=SESSIONS.get(p.getUUID());if(s!=null)PacketDistributor.sendToPlayer(watcher,new ViolinPayload(p.getUUID(),s.track));
        }
    }
    @SubscribeEvent public static void shutdown(net.neoforged.neoforge.event.server.ServerStoppedEvent e){SESSIONS.clear();}
    @SubscribeEvent public static void blockUse(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e){
        if(!e.getLevel().isClientSide&&playing(e.getEntity())){
            stop(e.getEntity());e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }
}
