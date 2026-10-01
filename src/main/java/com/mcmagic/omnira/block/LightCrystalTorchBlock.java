package com.mcmagic.omnira.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two occupied cells, with a 25.6-model-unit crystal lamp above a solid support. */
public final class LightCrystalTorchBlock extends DoublePlantBlock {
    public static final MapCodec<LightCrystalTorchBlock> CODEC=simpleCodec(LightCrystalTorchBlock::new);
    public LightCrystalTorchBlock(Properties properties) {super(properties);}
    @Override public MapCodec<? extends DoublePlantBlock> codec() {return CODEC;}
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos) {
        return state.isFaceSturdy(level,pos,Direction.UP);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(HALF)==DoubleBlockHalf.UPPER?box(5.5,0,5.5,10.5,9.6,10.5):
                Shapes.or(box(4,0,4,12,2,12),box(7,2,7,9,16,9));
    }
}
