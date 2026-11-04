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
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return getShape(state,level,pos,context);}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING))).is(DreamContent.SHADOW_LOG.get());
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPES[state.getValue(AGE)][state.getValue(FACING).get2DDataValue()];
    }
    private static final net.minecraft.world.phys.shapes.VoxelShape[][] SHAPES=shapes();
    private static net.minecraft.world.phys.shapes.VoxelShape[][] shapes(){
        var shapes=new net.minecraft.world.phys.shapes.VoxelShape[3][4];
        for(int age=0;age<3;age++){
            double x=age==2?4:5,y=age==0?8:age==1?5:4,z=age==1?10:11;
            shapes[age][net.minecraft.core.Direction.SOUTH.get2DDataValue()]=box(x,y,z,16-x,13,16);
            shapes[age][net.minecraft.core.Direction.NORTH.get2DDataValue()]=box(x,y,0,16-x,13,16-z);
            shapes[age][net.minecraft.core.Direction.EAST.get2DDataValue()]=box(z,y,x,16,13,16-x);
            shapes[age][net.minecraft.core.Direction.WEST.get2DDataValue()]=box(0,y,x,16-z,13,16-x);
        }
        return shapes;
    }
}
