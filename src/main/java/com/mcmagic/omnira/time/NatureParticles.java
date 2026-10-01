package com.mcmagic.omnira.time;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public final class NatureParticles {
    private static final DustParticleOptions GREEN=new DustParticleOptions(new Vector3f(.42F,.86F,.52F),.45F);
    private static final DustParticleOptions WHITE=new DustParticleOptions(new Vector3f(.94F,1F,.92F),.4F);
    private static final DustParticleOptions BLUE=new DustParticleOptions(new Vector3f(.25F,.52F,.98F),.45F);
    private static final DustParticleOptions BLACK=new DustParticleOptions(new Vector3f(.055F,.065F,.12F),.5F);
    public static void time(Level level,BlockPos pos,RandomSource r,boolean flower){
        level.addParticle(flower?WHITE:(r.nextInt(4)==0?WHITE:GREEN),pos.getX()+r.nextDouble(),pos.getY()+.25+r.nextDouble()*.7,pos.getZ()+r.nextDouble(),0,.015,0);
    }
    public static void space(Level level,BlockPos pos,RandomSource r){
        level.addParticle(r.nextBoolean()?BLUE:BLACK,pos.getX()+r.nextDouble(),pos.getY()+r.nextDouble(),pos.getZ()+r.nextDouble(),(r.nextDouble()-.5)*.02,(r.nextDouble()-.5)*.02,(r.nextDouble()-.5)*.02);
    }
    private NatureParticles(){}
}
