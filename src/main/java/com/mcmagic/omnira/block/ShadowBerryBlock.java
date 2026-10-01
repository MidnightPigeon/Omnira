package com.mcmagic.omnira.block;

import com.mcmagic.omnira.registry.DreamContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Cocoa growth and placement, attached only to shadow wood. */
public final class ShadowBerryBlock extends CocoaBlock {
    public static final MapCodec<CocoaBlock> CODEC=simpleCodec(ShadowBerryBlock::new);
    public ShadowBerryBlock(Properties properties) {super(properties);}
    @Override public MapCodec<CocoaBlock> codec() {return CODEC;}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING))).is(DreamContent.SHADOW_LOG.get());
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return switch(state.getValue(FACING)) {
            case NORTH -> box(4,4,0,12,13,9);
            case EAST -> box(7,4,4,16,13,12);
            case WEST -> box(0,4,4,9,13,12);
            default -> box(4,4,7,12,13,16);
        };
    }
}
