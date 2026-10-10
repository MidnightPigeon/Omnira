package com.mcmagic.omnira.space.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid="omnira")
public final class VoidAttraction {
    public static Vec3 acceleration(Level level,Vec3 point){
        Vec3 force=Vec3.ZERO;
        int minX=Mth.floor(point.x-5),maxX=Mth.floor(point.x+5),minZ=Mth.floor(point.z-5),maxZ=Mth.floor(point.z+5);
        int minY=Math.max(level.getMinBuildHeight(),Mth.floor(point.y-5)),maxY=Math.min(level.getMaxBuildHeight()-1,Mth.floor(point.y+5));
        for(int cx=minX>>4;cx<=maxX>>4;cx++)for(int cz=minZ>>4;cz<=maxZ>>4;cz++){
            var chunk=level.getChunkSource().getChunk(cx,cz,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);if(chunk==null)continue;
            for(int sy=minY>>4;sy<=maxY>>4;sy++){
                var section=chunk.getSection(chunk.getSectionIndex(sy<<4));
                if(!section.maybeHas(s->s.is(AbyssContent.MASS.get())))continue;
                for(int x=Math.max(minX,cx<<4);x<=Math.min(maxX,(cx<<4)+15);x++)for(int z=Math.max(minZ,cz<<4);z<=Math.min(maxZ,(cz<<4)+15);z++)
                    for(int y=Math.max(minY,sy<<4);y<=Math.min(maxY,(sy<<4)+15);y++){
                        if(!section.getBlockState(x&15,y&15,z&15).is(AbyssContent.MASS.get()))continue;
                        var center=Vec3.atCenterOf(new BlockPos(x,y,z));var delta=center.subtract(point);
                        if(delta.lengthSqr()>25||delta.lengthSqr()<.01)continue;
                        force=force.add(delta.normalize().scale(AbyssGravity.direction(level,center)==0?.025:.005));
                    }
            }
        }
        return force;
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        var entity=event.getEntity();if(!entity.isAlive()||entity.isSpectator()||entity.isPassenger())return;
        if(AbyssGravity.direction(entity)==0)entity.fallDistance=0;
        // Players predict locally; the server uses the same force for other entities.
        if(entity instanceof net.minecraft.world.entity.player.Player){if(!entity.level().isClientSide||!entity.isControlledByLocalInstance())return;}
        else if(entity.level().isClientSide)return;
        var force=acceleration(entity.level(),entity.getBoundingBox().getCenter());
        if(force.lengthSqr()>0){entity.setDeltaMovement(entity.getDeltaMovement().add(force));if(AbyssGravity.direction(entity)==0)entity.fallDistance=0;}
        else if(AbyssGravity.direction(entity)==0)entity.fallDistance=0;
    }
    private VoidAttraction(){}
}
