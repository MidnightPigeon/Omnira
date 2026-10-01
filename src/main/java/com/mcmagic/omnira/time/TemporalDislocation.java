package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.ModEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Position-only rewind, independent of Time Passage's health/food/mana snapshot. */
@EventBusSubscriber(modid="omnira")
public final class TemporalDislocation extends MobEffect {
    public static final String RECORD="OmniraTemporalDislocation",FLIGHT="OmniraDislocationFlight";
    private static final ResourceLocation FLIGHT_ID=ResourceLocation.parse("omnira:temporal_dislocation");
    public TemporalDislocation(){super(MobEffectCategory.BENEFICIAL,0xA3E7CF);}
    public static boolean phased(Entity entity){return entity instanceof Player p&&p.hasEffect(ModEffects.TEMPORAL_DISLOCATION);}
    public static boolean throughWalls(Player player){var effect=player.getEffect(ModEffects.TEMPORAL_DISLOCATION);return effect!=null&&effect.getAmplifier()>0;}
    @SubscribeEvent public static void added(MobEffectEvent.Added event){
        if(!(event.getEntity() instanceof ServerPlayer p)||!event.getEffectInstance().is(ModEffects.TEMPORAL_DISLOCATION))return;
        if(event.getOldEffectInstance()==null){
            var n=new CompoundTag();n.putString("Dimension",p.level().dimension().location().toString());
            n.putDouble("X",p.getX());n.putDouble("Y",p.getY());n.putDouble("Z",p.getZ());
            p.getPersistentData().put(RECORD,n);
        }
        if(event.getEffectInstance().getAmplifier()>0)startFlight(p);
    }
    private static void startFlight(ServerPlayer p){
        var attribute=p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if(!attribute.hasModifier(FLIGHT_ID))attribute.addTransientModifier(new AttributeModifier(FLIGHT_ID,1,AttributeModifier.Operation.ADD_VALUE));
        if(p.getPersistentData().contains(FLIGHT))return;
        var saved=new CompoundTag();saved.putBoolean("MayFly",p.getAbilities().mayfly);p.getPersistentData().put(FLIGHT,saved);
        // Sync permission together with flying, before the client can reject flight.
        p.getAbilities().mayfly=true;p.getAbilities().flying=true;
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.onUpdateAbilities();
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event){
        var p=event.getEntity();
        if(!(p instanceof ServerPlayer server))return;
        if(throughWalls(p)&&p.isAlive()){
            startFlight(server);
            p.fallDistance=0;
        }else restoreFlight(server);
        if(!phased(p)&&p.getPersistentData().contains(RECORD))finish(server);
    }
    public static void restoreFlight(ServerPlayer p){
        p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).removeModifier(FLIGHT_ID);
        if(!p.getPersistentData().contains(FLIGHT))return;
        boolean previousPermission=p.getPersistentData().getCompound(FLIGHT).getBoolean("MayFly");
        p.getPersistentData().remove(FLIGHT);
        p.getAbilities().mayfly=previousPermission||p.isCreative()||p.isSpectator();
        p.noPhysics=p.isSpectator();p.onUpdateAbilities();
    }
    public static void finish(ServerPlayer p){
        restoreFlight(p);
        var n=p.getPersistentData().getCompound(RECORD).copy();p.getPersistentData().remove(RECORD);
        if(!p.isAlive()||!n.contains("Dimension"))return;
        var destination=p.server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(n.getString("Dimension"))));
        if(destination==null)return;
        p.stopRiding();p.teleportTo(destination,n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"),p.getYRot(),p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
    }
    @SubscribeEvent public static void melee(AttackEntityEvent event){if(phased(event.getTarget()))event.setCanceled(true);}
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event){
        if(!phased(event.getEntity()))return;
        var source=event.getSource();
        if(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                ||source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)
                ||source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK_NO_AGGRO))event.setCanceled(true);
    }
    @SubscribeEvent public static void died(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer p){p.getPersistentData().remove(RECORD);restoreFlight(p);}
    }
}
