package com.mcmagic.omnira.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ShadowBookshelfBlock extends Block {
    public ShadowBookshelfBlock(Properties properties) {super(properties);}
    @Override public float getEnchantPowerBonus(BlockState state,LevelReader level,BlockPos pos) {return 2;}
}
