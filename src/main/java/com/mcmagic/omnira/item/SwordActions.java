package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModEntityTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import java.util.*;

@EventBusSubscriber(modid="omnira")
public final class SwordActions {
    private record Charge(ItemStack weapon,long start) {}
    private static final Map<ServerPlayer,Charge> CHARGES=new WeakHashMap<>();
    private static final ThreadLocal<ServerPlayer> POWER_STRIKE=new ThreadLocal<>();
    private static final ThreadLocal<ServerPlayer> SCRIPTED_STRIKE=new ThreadLocal<>();
    public static boolean isCharged(net.minecraft.world.entity.player.Player player) {return POWER_STRIKE.get()==player;}
    public static boolean weaponUnavailable(net.minecraft.world.entity.player.Player player) {
        return SCRIPTED_STRIKE.get()!=player && player.getMainHandItem().getItem() instanceof RitualSwordItem sword
                && sword.kind==RitualSwordItem.Kind.NEEDLE && FlyingNeedle.isOut(player);
    }
    public static double spellBonus(net.minecraft.world.entity.player.Player p) {return POWER_STRIKE.get()==p?.5:0;}
    public static double reachMultiplier(net.minecraft.world.entity.player.Player player) {
        return player.getMainHandItem().getItem() instanceof RitualSwordItem && player.getOffhandItem().getItem() instanceof StaffItem
                ?com.mcmagic.omnira.item.staff.StaffAssembly.of(player.getOffhandItem()).stats().rangeMultiplier():1;
    }
    public static float damage(net.minecraft.world.entity.player.Player p,float base) {return POWER_STRIKE.get()==p?base*2:base;}
    public static boolean eligible(ServerPlayer p) {return p.isAlive() && !p.isSpectator() && !p.isUsingItem() && !LifeEnderItem.held(p)
            && !weaponUnavailable(p) && p.containerMenu==p.inventoryMenu && p.getMainHandItem().getItem() instanceof RitualSwordItem;}
    public static void input(ServerPlayer p,int action) {
        if(action==0) {
            if(eligible(p))CHARGES.putIfAbsent(p,new Charge(p.getMainHandItem(),p.serverLevel().getGameTime()));
            return;
        }
        Charge charge=CHARGES.remove(p);
        if(action!=1 || charge==null || !eligible(p) || charge.weapon!=p.getMainHandItem())return;
        boolean ready=p.serverLevel().getGameTime()-charge.start>=20;
        // The initial press already performs the vanilla attack; a short release is not another hit.
        if(!ready)return;
        var sword=(RitualSwordItem)charge.weapon.getItem();
        if(sword.kind==RitualSwordItem.Kind.NEEDLE) {
            boolean active=FlyingNeedle.isOut(p);
            if(!active) {
                var needle=ModEntityTypes.FLYING_NEEDLE.get().create(p.serverLevel());
                if(needle!=null){needle.launch(p,charge.weapon);p.serverLevel().addFreshEntity(needle);}
            }
        } else {
            NailSlash.release(p);
        }
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
    }
    public static Entity pick(ServerPlayer p,double reach) {
        Vec3 from=p.getEyePosition(),end=from.add(p.getLookAngle().scale(reach));
        var block=p.level().clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        end=block.getLocation();
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p,from,end,
                p.getBoundingBox().expandTowards(end.subtract(from)).inflate(1),e->!e.isSpectator() && e.isPickable() && e!=p,from.distanceToSqr(end));
        return hit==null?null:hit.getEntity();
    }
    public static void strike(ServerPlayer p,Entity target,boolean charged) {
        var previous=POWER_STRIKE.get();
        var scripted=SCRIPTED_STRIKE.get();
        SCRIPTED_STRIKE.set(p);
        if(charged)POWER_STRIKE.set(p);
        try {p.attack(target);} finally {
            if(previous==null)POWER_STRIKE.remove();else POWER_STRIKE.set(previous);
            if(scripted==null)SCRIPTED_STRIKE.remove();else SCRIPTED_STRIKE.set(scripted);
        }
    }
    @SubscribeEvent public static void sweep(net.neoforged.neoforge.event.entity.player.SweepAttackEvent event) {
        if(isCharged(event.getEntity()))event.setSweeping(false);
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player)
                || event.getNewDamage()<=0 && event.getReduction(DamageContainer.Reduction.ABSORPTION)<=0) return;
        boolean charge=CHARGES.remove(player)!=null;
        boolean focus=SwordFocus.stop(player);
        if(focus) player.stopUsingItem();
        if(charge || focus) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new com.mcmagic.omnira.network.SwordInterruptedPayload((charge?1:0)|(focus?2:0)));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {CHARGES.remove(e.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {CHARGES.remove(e.getEntity());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) {CHARGES.remove(e.getEntity());}
    @SubscribeEvent public static void stop(ServerStoppedEvent e) {CHARGES.clear();POWER_STRIKE.remove();}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer p) {
            var charge=CHARGES.get(p);
            if(charge!=null && (!eligible(p) || p.getMainHandItem()!=charge.weapon || p.serverLevel().getGameTime()-charge.start>1200))CHARGES.remove(p);
        }
    }
}
