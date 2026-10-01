package com.mcmagic.omnira.world.structure;

import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import java.util.*;

/** Resolve template corners before export; offline NBT placement does not emit neighbor updates. */
final class RuinStairShapes {
    static void resolve(Map<BlockPos,RuinsPiece.Cell> cells) {
        for(var entry:new ArrayList<>(cells.entrySet())) {
            var c=entry.getValue();if(!(c.state().getBlock() instanceof StairBlock))continue;
            cells.put(entry.getKey(),new RuinsPiece.Cell(c.state().setValue(StairBlock.SHAPE,shape(cells,entry.getKey(),c.state())),c.loot(),c.data()));
        }
    }
    private static BlockState at(Map<BlockPos,RuinsPiece.Cell> cells,BlockPos p) {
        var c=cells.get(p);return c==null?Blocks.AIR.defaultBlockState():c.state();
    }
    private static boolean compatible(BlockState a,BlockState b) {
        return b.getBlock() instanceof StairBlock&&a.getValue(StairBlock.HALF)==b.getValue(StairBlock.HALF);
    }
    private static boolean clear(Map<BlockPos,RuinsPiece.Cell> cells,BlockPos p,BlockState s,Direction side) {
        var other=at(cells,p.relative(side));return !compatible(s,other)||s.getValue(StairBlock.FACING)!=other.getValue(StairBlock.FACING);
    }
    private static StairsShape shape(Map<BlockPos,RuinsPiece.Cell> cells,BlockPos p,BlockState s) {
        var facing=s.getValue(StairBlock.FACING);var front=at(cells,p.relative(facing));
        if(compatible(s,front)) {
            var turn=front.getValue(StairBlock.FACING);
            if(turn.getAxis()!=facing.getAxis()&&clear(cells,p,s,turn.getOpposite()))return turn==facing.getCounterClockWise()?StairsShape.OUTER_LEFT:StairsShape.OUTER_RIGHT;
        }
        var back=at(cells,p.relative(facing.getOpposite()));
        if(compatible(s,back)) {
            var turn=back.getValue(StairBlock.FACING);
            if(turn.getAxis()!=facing.getAxis()&&clear(cells,p,s,turn))return turn==facing.getCounterClockWise()?StairsShape.INNER_LEFT:StairsShape.INNER_RIGHT;
        }
        return StairsShape.STRAIGHT;
    }
}
