package com.mcmagic.omnira.client;

import com.mcmagic.omnira.time.EventideRuins;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Local, sparse surface dust; no weather changes or server-side particle traffic. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class EventideWind {
    private EventideWind(){}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var client=Minecraft.getInstance();var level=client.level;var camera=client.getCameraEntity();
        if(level==null||camera==null||client.isPaused()||level.getGameTime()%2!=0
                ||!level.dimension().equals(ModDimensions.SPACETIME_CORRIDOR)
                ||!level.getBiome(camera.blockPosition()).is(EventideRuins.BIOME))return;
        var quality=client.options.particles().get();
        if(quality==ParticleStatus.MINIMAL)return;
        int attempts=quality==ParticleStatus.DECREASED?2:5;
        var random=level.random;
        double breeze=.14+.035*Math.sin(level.getGameTime()*.018);
        for(int i=0;i<attempts;i++){
            double x=camera.getX()+(random.nextDouble()-.5)*28;
            double z=camera.getZ()+(random.nextDouble()-.5)*28;
            var column=BlockPos.containing(x,camera.getY(),z);
            if(!level.hasChunkAt(column))continue;
            int surface=level.getHeight(Heightmap.Types.MOTION_BLOCKING,column.getX(),column.getZ());
            double y=surface+.2+random.nextDouble()*3.5;
            var at=BlockPos.containing(x,y,z);
            if(Math.abs(y-camera.getY())>7||!level.getBiome(at).is(EventideRuins.BIOME)
                    ||!level.canSeeSky(at)||!level.isEmptyBlock(at)||!level.getFluidState(at.below()).isEmpty())continue;
            var dust=client.particleEngine.createParticle(ParticleTypes.ASH,x,y,z,0,0,0);
            if(dust==null)continue;
            boolean pale=random.nextInt(4)==0;
            dust.setColor(pale?.80F:.64F,pale?.81F:.66F,pale?.65F:.55F);
            dust.setParticleSpeed(breeze,(random.nextDouble()-.5)*.008,.04);
            dust.setLifetime(35+random.nextInt(25));
            dust.scale(.55F+random.nextFloat()*.35F);
        }
    }
}
