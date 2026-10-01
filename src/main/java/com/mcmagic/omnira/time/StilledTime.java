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
    public static boolean stopped(Entity entity){
        if(entity instanceof LivingEntity)return false;
        if(entity instanceof com.mcmagic.omnira.archaeology.DrillSeat)return false;
        if(net.neoforged.fml.ModList.get().isLoaded("create")&&com.mcmagic.omnira.compat.CreateLocalTime.assembled(entity))return false;
        return stopped(entity.level(),entity.blockPosition());
    }
    private StilledTime(){}
}
