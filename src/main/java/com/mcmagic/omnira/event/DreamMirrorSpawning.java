package com.mcmagic.omnira.event;

import com.mcmagic.omnira.entity.DreamMirror;
import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Rare middle islands are hidden by the upper islands' heightmap and shared creature cap. */
@EventBusSubscriber(modid="omnira")
public final class DreamMirrorSpawning {
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(ModDimensions.DREAM_REALM)
                || level.getGameTime()%400!=0 || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        for(var player:level.players()) {
            if(player.isSpectator() || level.getEntitiesOfClass(DreamMirror.class,player.getBoundingBox().inflate(96),e->e.isAlive()).size()>=4) continue;
            for(int attempt=0;attempt<20;attempt++) {
                int x=player.getBlockX()+level.random.nextInt(113)-56,z=player.getBlockZ()+level.random.nextInt(113)-56;
                if(!level.hasChunk(x>>4,z>>4)) continue;
                boolean placed=false;
                for(int y=140;y>=94;y--) {
                    var pos=new BlockPos(x,y,z);
                    if(player.distanceToSqr(x+.5,y,z+.5)<24*24 || player.distanceToSqr(x+.5,y,z+.5)>80*80) continue;
                    if(level.getNearestPlayer(x+.5,y,z+.5,24,p->!p.isSpectator())!=null) continue;
                    if(!DreamMirror.canSpawn(ModEntityTypes.DREAM_MIRROR.get(),level,MobSpawnType.NATURAL,pos,level.random)) continue;
                    if(level.getEntitiesOfClass(DreamMirror.class,new net.minecraft.world.phys.AABB(pos).inflate(24),e->e.isAlive()).size()>=2) break;
                    var mirror=ModEntityTypes.DREAM_MIRROR.get().create(level);
                    mirror.moveTo(x+.5,y,z+.5,level.random.nextFloat()*360,0);
                    if(level.noCollision(mirror,mirror.getBoundingBox())) {
                        mirror.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.NATURAL,null);
                        placed=level.addFreshEntity(mirror);
                    }
                    break;
                }
                if(placed) break;
            }
        }
    }
}
