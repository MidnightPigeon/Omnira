package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** An extra simulation step advances local timers, while movement runs only once. */
public final class FleetingTime {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("omnira","fleeting_woods"));
    private static final ThreadLocal<Entity> BONUS=new ThreadLocal<>();
    public static boolean accelerated(Level level,BlockPos pos){return level.hasChunkAt(pos)&&level.getBiome(pos).is(BIOME);}
    public static boolean bonus(Entity entity){return BONUS.get()==entity;}
    public static boolean inBonus(){return BONUS.get()!=null;}
    public static void extra(Entity entity,Runnable step,boolean incrementAge){
        if(inBonus()||entity.isRemoved()||!accelerated(entity.level(),entity.blockPosition()))return;
        var velocity=entity.getDeltaMovement();float fall=entity.fallDistance,yaw=entity.getYRot(),pitch=entity.getXRot();
        var previous=BONUS.get();BONUS.set(entity);
        try{
            if(incrementAge)entity.tickCount++;
            // Projectiles integrate velocity directly instead of calling Entity.move.
            if(entity instanceof net.minecraft.world.entity.projectile.Projectile)entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            step.run();
            if(entity instanceof ServerPlayer player&&incrementAge){
                long now=player.level().getGameTime();
                for(var stack:player.getInventory().items){long end=stack.getOrDefault(ModDataComponents.SPELL_READY_AT,0L);if(end>now)stack.set(ModDataComponents.SPELL_READY_AT,end-1);}
                for(var stack:player.getInventory().offhand){long end=stack.getOrDefault(ModDataComponents.SPELL_READY_AT,0L);if(end>now)stack.set(ModDataComponents.SPELL_READY_AT,end-1);}
            }
        }finally{entity.setDeltaMovement(velocity);entity.fallDistance=fall;entity.setYRot(yaw);entity.setXRot(pitch);if(previous==null)BONUS.remove();else BONUS.set(previous);}
    }
    private FleetingTime(){}
}
