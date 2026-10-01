package com.mcmagic.omnira.spacetime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public final class ExcitedSpatialCrystalBlock extends TransparentBlock {
    private static final DustParticleOptions MOTE=new DustParticleOptions(new Vector3f(0.71F,0.76F,0.96F),0.45F);

    public ExcitedSpatialCrystalBlock(BlockBehaviour.Properties properties){super(properties);}

    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        for(int i=0;i<2;i++){
            double x=pos.getX()+0.25+random.nextDouble()*0.5;
            double y=pos.getY()+0.25+random.nextDouble()*0.5;
            double z=pos.getZ()+0.25+random.nextDouble()*0.5;
            level.addParticle(MOTE,x,y,z,(random.nextDouble()-.5)*.012,(random.nextDouble()-.5)*.012,(random.nextDouble()-.5)*.012);
        }
    }
}
