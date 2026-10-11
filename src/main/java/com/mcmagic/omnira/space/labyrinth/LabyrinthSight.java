package com.mcmagic.omnira.space.labyrinth;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Conservative sight test: fluids and transparent crystals never hide room displays. */
public final class LabyrinthSight {
    public static boolean clear(Level level,Vec3 camera,Vec3 target){
        return level.clip(new ClipContext(camera,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,CollisionContext.empty()){
            @Override public VoxelShape getBlockShape(BlockState state,BlockGetter world,BlockPos pos){
                return state.isSolidRender(world,pos)?state.getOcclusionShape(world,pos):Shapes.empty();
            }
        }).getType()==HitResult.Type.MISS;
    }
    public static boolean visible(Level level,Vec3 camera,AABB bounds){
        if(bounds.contains(camera)||clear(level,camera,bounds.getCenter()))return true;
        for(int corner=0;corner<8;corner++)
            if(clear(level,camera,new Vec3((corner&1)==0?bounds.minX:bounds.maxX,(corner&2)==0?bounds.minY:bounds.maxY,(corner&4)==0?bounds.minZ:bounds.maxZ)))return true;
        return false;
    }
    private LabyrinthSight(){}
}
