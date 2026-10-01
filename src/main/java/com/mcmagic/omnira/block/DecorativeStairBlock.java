package com.mcmagic.omnira.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Keep vanilla mixed-material corners; reconcile after batch/template placement finishes. */
public final class DecorativeStairBlock extends StairBlock {
    public DecorativeStairBlock(BlockState base,Properties properties){super(base,properties);}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving) {
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide)level.scheduleTick(pos,this,1);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        var connected=Block.updateFromNeighbourShapes(state,level,pos);
        if(connected!=state)level.setBlock(pos,connected,Block.UPDATE_ALL);
    }
}
