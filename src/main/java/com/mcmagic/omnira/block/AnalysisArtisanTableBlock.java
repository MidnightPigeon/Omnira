package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.AnalysisArtisanTableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AnalysisArtisanTableBlock extends CrystalProcessingTableBlock {
    public static final MapCodec<AnalysisArtisanTableBlock> CODEC = simpleCodec(AnalysisArtisanTableBlock::new);
    public AnalysisArtisanTableBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends CrystalProcessingTableBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AnalysisArtisanTableBlockEntity(pos, state); }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return WorkstationGeometry.analysis(state.getValue(FACING));
    }
}
