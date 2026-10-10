package com.mcmagic.omnira.space.phase;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.AABB;

/** Capture once before replacement, so newly spawned drops are not impaled by the same growth. */
public final class PhaseImpact {
    public static List<Entity> occupants(ServerLevel level,Collection<BlockPos> cells){
        if(cells.isEmpty())return List.of();
        var bounds=cells.stream().map(AABB::new).reduce(AABB::minmax).orElseThrow();
        return level.getEntities((Entity)null,bounds,e->e.isAlive()&&!e.isSpectator()
                &&cells.stream().anyMatch(p->new AABB(p).intersects(e.getBoundingBox())));
    }
    public static void impale(List<Entity> occupants){
        for(var entity:occupants){
            if(entity instanceof LivingEntity living)com.mcmagic.omnira.space.sea.VoidSeaEffects.trueDamage(living,10,living.damageSources().genericKill());
            else entity.hurt(entity.damageSources().genericKill(),10);
        }
    }
    private PhaseImpact(){}
}
