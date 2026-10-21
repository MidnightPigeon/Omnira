package com.mcmagic.omnira.weaver;

import com.mcmagic.omnira.entity.WeaverSpirit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Bounded, loaded-only exit search; never makes a phased body solid inside terrain. */
public final class WeaverPhasing {
    private static final int[] HEIGHTS={0,1,-1};
    public static boolean clear(WeaverSpirit spirit,Vec3 position){
        var box=spirit.getBoundingBox().move(position.subtract(spirit.position()));
        var level=spirit.level();
        return box.minY>=level.getMinBuildHeight()&&box.maxY<=level.getMaxBuildHeight()
                &&level.getWorldBorder().isWithinBounds(box)
                &&level.hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))
                &&level.noCollision(spirit,box);
    }
    public static Vec3 exit(WeaverSpirit spirit){
        for(int radius=2;radius<=32;radius+=2)
            for(int y:HEIGHTS)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                if(x==0&&y==0&&z==0)continue;
                var point=spirit.position().add(x*radius,y*radius,z*radius);
                if(clear(spirit,point))return point;
            }
        return null;
    }
    private WeaverPhasing(){}
}
