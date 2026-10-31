package com.mcmagic.omnira.time;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** Local simulation suspension; living entities and their timers remain unchanged. */
public final class StilledTime {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:stilled_wastes"));
    public static boolean shipPlot(Level level,BlockPos pos){
        return net.neoforged.fml.ModList.get().isLoaded("sable")&&com.mcmagic.omnira.compat.SableLocalTime.inPlot(level,pos);
    }
    public static boolean stopped(Level level,BlockPos pos){
        if(shipPlot(level,pos)||!level.hasChunkAt(pos))return false;
        return level.getBiome(pos).is(BIOME);
    }
    /** Warp points and their remnants drive the biome's own hazard/regrowth, not machine time. */
    public static boolean blockTickStopped(Level level,BlockPos pos){
        if(!stopped(level,pos))return false;
        var block=level.getBlockState(pos).getBlock();
        return !(block instanceof com.mcmagic.omnira.spacetime.TimeWarpPointBlock)
                && !(block instanceof com.mcmagic.omnira.spacetime.TemporalMoteBlock);
    }
    public static boolean stopped(Entity entity){
        if(entity instanceof LivingEntity || slowedType(entity))return false;
        if(entity instanceof com.mcmagic.omnira.archaeology.DrillSeat)return false;
        if(net.neoforged.fml.ModList.get().isLoaded("create")&&com.mcmagic.omnira.compat.CreateLocalTime.assembled(entity))return false;
        return stopped(entity.level(),entity.blockPosition());
    }
    private static boolean slowedType(Entity entity){
        return entity instanceof net.minecraft.world.entity.item.ItemEntity
                || entity instanceof net.minecraft.world.entity.projectile.Projectile
                || entity instanceof com.mcmagic.omnira.spell.entity.SpellEntity spell
                && spell.kind()==com.mcmagic.omnira.spell.entity.SpellEntity.Kind.PROJECTILE;
    }
    /** Share biome/warp throttling without freezing pickup cooldowns or mutating velocity. */
    public static boolean skipTick(Entity entity){
        if(stopped(entity))return true;
        double factor=com.mcmagic.omnira.spacetime.TimeWarpEffects.tickFactor(entity);
        if(slowedType(entity)&&stopped(entity.level(),entity.blockPosition()))factor*=.05;
        if(factor>=1)return false;
        int steps=(int)Math.round(factor*200);
        if(Math.floorMod((entity.level().getGameTime()+entity.getId())*steps,200)<steps)return false;
        if(entity instanceof net.minecraft.world.entity.item.ItemEntity){
            var item=(com.mcmagic.omnira.mixin.StilledItemAccessor)entity;
            int delay=item.omnira$getPickupDelay();
            if(delay>0&&delay!=32767)item.omnira$setPickupDelay(delay-1);
        }
        // Avoid replaying the last active tick's interpolation while simulation is paused.
        entity.setOldPosAndRot();
        return true;
    }
    private StilledTime(){}
}
