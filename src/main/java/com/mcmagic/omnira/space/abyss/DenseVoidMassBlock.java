package com.mcmagic.omnira.space.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public final class DenseVoidMassBlock extends Block {
    private static final VoxelShape SHAPE=Block.box(3,3,3,13,13,13);
    public DenseVoidMassBlock(Properties properties){super(properties);}
    @Override public VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        double angle=random.nextDouble()*Math.PI*2,dy=(random.nextDouble()-.5)*1.4,r=.7;
        double dx=Math.cos(angle)*r,dz=Math.sin(angle)*r;
        level.addParticle(new DustParticleOptions(new Vector3f(.45F,.28F,.65F),.7F),pos.getX()+.5+dx,pos.getY()+.5+dy,pos.getZ()+.5+dz,-dx*.08,-dy*.08,-dz*.08);
    }
}
