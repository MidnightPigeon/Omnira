package com.mcmagic.omnira.world.dimension;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Eight unordered elemental offerings; fluid is an offering, not stored inventory. */
public final class GenesisRitual {
    public static final int REQUIRED_FLUID=10000;
    public static final int ABSORPTION_TICKS=40;
    public static final int FORMATION_TICKS=20;
    private GenesisRitual() {}
    public static java.util.List<Item> ingredients() {
        return java.util.List.of(ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),
                ModItems.FIRE_MICROCORE.get(),ModItems.AIR_MICROCORE.get(),ModItems.LIGHT_MICROCORE.get(),
                ModItems.DARK_MICROCORE.get(),ModItems.TIME_MICROCORE.get(),ModItems.SPACE_MICROCORE.get());
    }
    public static boolean loaded(Level level,BlockPos core) {
        for(var offset:DreamRitual.ANCHORS)
            if(!level.hasChunkAt(core.offset(offset[0],0,offset[1])))return false;
        return true;
    }
    public static java.util.List<CrystalPedestalBlockEntity> pedestals(Level level,BlockPos core) {
        if(!level.getBlockState(core).is(ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get()) || !loaded(level,core))return java.util.List.of();
        var missing=new java.util.HashSet<>(ingredients());
        var result=new java.util.ArrayList<CrystalPedestalBlockEntity>();
        for(var offset:DreamRitual.ANCHORS) {
            var at=core.offset(offset[0],0,offset[1]);
            if(!level.getBlockState(at).is(ModBlocks.CRYSTAL_PEDESTAL.get())
                    || !(level.getBlockEntity(at) instanceof CrystalPedestalBlockEntity pedestal)
                    || pedestal.getItem(0).getCount()!=1 || !missing.remove(pedestal.getItem(0).getItem()))return java.util.List.of();
            result.add(pedestal);
        }
        return missing.isEmpty()?result:java.util.List.of();
    }
}
