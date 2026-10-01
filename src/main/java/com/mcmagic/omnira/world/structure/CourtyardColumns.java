package com.mcmagic.omnira.world.structure;

import com.mcmagic.omnira.block.CrystalColumnBlock;
import com.mcmagic.omnira.registry.DreamContent;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Eight perimeter positions; fallen shafts stay on the floor and away from the torches. */
public final class CourtyardColumns {
    private static final int[][] ANCHORS={{4,4},{8,2},{12,4},{14,8},{12,12},{8,14},{4,12},{2,8}};
    private static final Direction[] FALL={Direction.SOUTH,Direction.WEST,Direction.WEST,Direction.NORTH,
            Direction.NORTH,Direction.EAST,Direction.EAST,Direction.SOUTH};
    private CourtyardColumns() {}
    public static Map<BlockPos,BlockState> plan(long seed) {
        var random=RandomSource.create(seed);Map<BlockPos,BlockState> cells=new LinkedHashMap<>();
        for(int i=0;i<ANCHORS.length;i++) {
            var base=new BlockPos(ANCHORS[i][0],7,ANCHORS[i][1]);int variant=random.nextInt(3);
            if(variant==2) {
                var direction=FALL[i];cells.put(base,end(direction));
                for(int j=1;j<=2;j++)cells.put(base.relative(direction,j),shaft(direction.getAxis()));
            } else {
                cells.put(base,end(Direction.UP));int height=variant==0?3:1+random.nextInt(2);
                for(int j=1;j<=height;j++)cells.put(base.above(j),shaft(Direction.Axis.Y));
                if(variant==0)cells.put(base.above(4),end(Direction.DOWN));
            }
        }
        return cells;
    }
    private static BlockState shaft(Direction.Axis axis) {
        return DreamContent.CRYSTAL_COLUMN.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis);
    }
    private static BlockState end(Direction tip) {
        return DreamContent.CRYSTAL_COLUMN_BASE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,tip.getAxis())
                .setValue(CrystalColumnBlock.REVERSED,tip.getAxisDirection()==Direction.AxisDirection.NEGATIVE)
                .setValue(CrystalColumnBlock.MANUAL,true);
    }
}
