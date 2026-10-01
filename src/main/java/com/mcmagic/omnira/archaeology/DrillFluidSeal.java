package com.mcmagic.omnira.archaeology;

import com.mcmagic.omnira.mire.MireContent;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Seal lateral fluid faces without replacing waterlogged solid blocks or the shaft floor. */
public final class DrillFluidSeal {
    public static BlockState frozen(BlockState state){
        if(!(state.getBlock() instanceof LiquidBlock))return null;
        var fluid=state.getFluidState();
        if(MireContent.fluid(fluid))return MireContent.SILT.get().defaultBlockState();
        if(fluid.is(FluidTags.LAVA))return Blocks.OBSIDIAN.defaultBlockState();
        if(fluid.is(FluidTags.WATER))return Blocks.BLUE_ICE.defaultBlockState();
        return null;
    }
    public static boolean seal(ServerLevel level,BlockPos center,Player owner){
        var changes=new java.util.LinkedHashMap<BlockPos,BlockState>();
        for(int dy=0;dy<=2;dy++)for(int offset=-1;offset<=1;offset++)for(var side:Direction.Plane.HORIZONTAL){
            var pos=center.above(dy).relative(side,2).relative(side.getClockWise(),offset);
            if(!level.hasChunkAt(pos))return false;
            var replacement=frozen(level.getBlockState(pos));if(replacement==null)continue;
            if(!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(owner,pos)
                    ||net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level,pos,level.getBlockState(pos),owner)).isCanceled()
                    ||net.neoforged.neoforge.event.EventHooks.onBlockPlace(owner,net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,pos),side))return false;
            changes.put(pos,replacement);
        }
        changes.forEach((pos,state)->level.setBlock(pos,state,3));return true;
    }
    private DrillFluidSeal(){}
}
