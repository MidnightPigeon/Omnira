package com.mcmagic.omnira.space.phase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** Local motes only: large formations inherit the individual blocks' effect. */
final class PhaseParticles {
    static void inside(Level level,BlockPos pos,RandomSource random,float phase){
        if(random.nextInt(3)!=0)return;
        var color=new Vector3f(.81F,.65F,1F).lerp(new Vector3f(.43F,.16F,.76F),phase);
        level.addParticle(new DustParticleOptions(color,.55F),pos.getX()+.1+random.nextDouble()*.8,
                pos.getY()+.1+random.nextDouble()*.8,pos.getZ()+.1+random.nextDouble()*.8,0,.006,0);
    }
    static void around(Level level,BlockPos pos,RandomSource random){
        double angle=random.nextDouble()*Math.PI*2;
        level.addParticle(new DustParticleOptions(new Vector3f(.81F,.65F,1F),.55F),
                pos.getX()+.5+Math.cos(angle)*.47,pos.getY()+random.nextDouble()*.8,
                pos.getZ()+.5+Math.sin(angle)*.47,-Math.sin(angle)*.008,.005,Math.cos(angle)*.008);
    }
    private PhaseParticles(){}
}
