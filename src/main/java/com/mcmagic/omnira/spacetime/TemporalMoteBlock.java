package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.*;

public final class TemporalMoteBlock extends Block {
    public static final int REGROW_TICKS=6000;
    private static final VoxelShape SHAPE=Block.box(6,6,6,10,10,10);
    public TemporalMoteBlock(Properties properties){super(properties);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(level instanceof ServerLevel server && !old.is(this))server.scheduleTick(pos,this,REGROW_TICKS);
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(level.getBlockState(pos).is(this))level.setBlock(pos,ModBlocks.TIME_WARP_POINT.get().defaultBlockState(),Block.UPDATE_ALL);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        double time=level.getGameTime()*.19;
        for(int i=0;i<2;i++){
            double phase=time+i*Math.PI;
            level.addParticle(ModParticles.TIME_WARP_SPARK.get(),pos.getX()+.5+Math.cos(phase)*.18,
                    pos.getY()+.5+Math.sin(phase*1.4)*.13,pos.getZ()+.5+Math.sin(phase)*.18,0,.003,0);
        }
    }
}
