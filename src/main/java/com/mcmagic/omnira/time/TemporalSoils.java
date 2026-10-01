package com.mcmagic.omnira.time;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class TemporalSoils {
    public static final TagKey<Block> SILTS=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("omnira","temporal_silts"));
    public static final TagKey<Block> SHADOW=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("omnira","shadow_rocks"));
    public static boolean ordinary(BlockState state){return !state.is(SHADOW)&&(state.is(BlockTags.DIRT)||state.is(Blocks.END_STONE)||state.is(Blocks.FARMLAND));}
    public static boolean timeTree(BlockState state){return state.is(SILTS)||ordinary(state);}
    public static boolean shadowTree(BlockState state){return state.is(SHADOW)||state.is(SILTS)||ordinary(state);}
    private TemporalSoils(){}
}
