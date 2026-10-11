package com.mcmagic.omnira.client;

import com.mcmagic.omnira.space.labyrinth.*;
import java.util.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;

/** Skip room displays hidden behind opaque maze walls, not their transparent neighbours. */
public final class LabyrinthDisplayVisibility {
    private record Entry(Vec3 camera,long tick,boolean visible){}
    private static final Map<BlockEntity,Entry> CACHE=new WeakHashMap<>();
    public static boolean visible(BlockEntity be,Vec3 camera,AABB bounds){
        var level=be.getLevel();if(level==null)return true;
        long now=level.getGameTime();var previous=CACHE.get(be);
        if(previous!=null&&now>=previous.tick()&&now-previous.tick()<10&&previous.camera().distanceToSqr(camera)<.0625)return previous.visible();
        boolean result=!level.getBiome(be.getBlockPos()).is(LabyrinthContent.BIOME)||LabyrinthSight.visible(level,camera,bounds);
        CACHE.put(be,new Entry(camera,now,result));return result;
    }
    private LabyrinthDisplayVisibility(){}
}
