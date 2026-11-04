package com.mcmagic.omnira.space.geodes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class DeflectionBudBlock extends AmethystClusterBlock {
    DeflectionBudBlock(int height,int inset){
        super(height,inset,Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).requiresCorrectToolForDrops().lightLevel(s->5));
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(level instanceof ServerLevel server)DeflectionSources.update(server,pos,DeflectionContent.radius(state));
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        super.onRemove(state,level,pos,next,moving);
        if(!next.is(this)&&level instanceof ServerLevel server)DeflectionSources.update(server,pos,0);
    }
}
