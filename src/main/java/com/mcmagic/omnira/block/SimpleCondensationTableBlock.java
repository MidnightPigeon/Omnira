package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.SimpleCondensationTableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public class SimpleCondensationTableBlock extends CrystalProcessingTableBlock {
    public static final MapCodec<SimpleCondensationTableBlock> CODEC=simpleCodec(SimpleCondensationTableBlock::new);
    private static final VoxelShape SHAPE=Shapes.or(
            box(1,9,1,15,12,15),box(2,0,2,4,9,4),box(12,0,2,14,9,4),
            box(2,0,12,4,9,14),box(12,0,12,14,9,14),
            box(4,3,2,12,5,4),box(4,3,12,12,5,14),
            box(5,12,5,11,13,6),box(5,12,10,11,13,11),
            box(5,12,6,6,13,10),box(10,12,6,11,13,10)).optimize();
    private static final VoxelShape EAST_WEST=rotateShape();
    private static VoxelShape rotateShape() {
        VoxelShape[] shape={Shapes.empty()};
        SHAPE.forAllBoxes((x0,y0,z0,x1,y1,z1)->shape[0]=Shapes.or(shape[0],Shapes.box(1-z1,y0,x0,1-z0,y1,x1)));
        return shape[0].optimize();
    }
    public SimpleCondensationTableBlock(Properties properties) {super(properties);}
    @Override protected MapCodec<? extends CrystalProcessingTableBlock> codec() {return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new SimpleCondensationTableBlockEntity(pos,state);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return state.getValue(FACING).getAxis()==net.minecraft.core.Direction.Axis.X?EAST_WEST:SHAPE;}
}
