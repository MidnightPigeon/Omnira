package com.mcmagic.omnira.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Resolve moving-body output cells without loading chunks or exposing Sable to common code. */
public final class SablePulseFeet {
    public static Set<BlockPos> positions(ServerPlayer player){
        var result=new HashSet<BlockPos>();
        var container=SubLevelContainer.getContainer(player.serverLevel());if(container==null)return result;
        var point=player.position();
        var bounds=new dev.ryanhcode.sable.companion.math.BoundingBox3d(new AABB(point,point).inflate(1));
        for(var candidate:container.queryIntersecting(bounds)){
            if(!(candidate instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel body)||body.isRemoved())continue;
            var local=body.logicalPose().transformPositionInverse(point);
            if(body.getPlot().contains(local))result.add(BlockPos.containing(local));
        }
        return result;
    }
    private SablePulseFeet(){}
}
