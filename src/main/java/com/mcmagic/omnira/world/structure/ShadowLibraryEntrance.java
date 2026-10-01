package com.mcmagic.omnira.world.structure;

import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.registry.ShadowWoodContent;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/** Supported ladder shaft ending in a five-block surface hatch. */
public final class ShadowLibraryEntrance {
    private ShadowLibraryEntrance() {}
    public static Map<BlockPos,BlockState> plan(BlockPos roofHatch,int surfaceY) {
        if(surfaceY<roofHatch.getY()) throw new IllegalArgumentException("Surface below room roof");
        Map<BlockPos,BlockState> cells=new LinkedHashMap<>();
        var wall=DreamContent.SHADOW_ROCK_BRICKS.get().defaultBlockState();
        for(int y=roofHatch.getY();y<=surfaceY;y++) {
            var center=new BlockPos(roofHatch.getX(),y,roofHatch.getZ());
            for(var direction:Direction.Plane.HORIZONTAL)
                cells.put(center.relative(direction),y==surfaceY?DreamContent.CHISELED_SHADOW_ROCK.get().defaultBlockState():wall);
            cells.put(center,y==surfaceY?ShadowWoodContent.TRAPDOOR.get().defaultBlockState()
                    .setValue(TrapDoorBlock.FACING,Direction.WEST).setValue(TrapDoorBlock.HALF,Half.TOP)
                    :Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.EAST));
        }
        return cells;
    }
}
