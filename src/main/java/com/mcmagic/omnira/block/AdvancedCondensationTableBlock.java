package com.mcmagic.omnira.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AdvancedCondensationTableBlock extends SimpleCondensationTableBlock {
    public static final MapCodec<AdvancedCondensationTableBlock> CODEC=simpleCodec(AdvancedCondensationTableBlock::new);
    public AdvancedCondensationTableBlock(Properties properties) {super(properties);}
    @Override protected MapCodec<? extends CrystalProcessingTableBlock> codec() {return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new com.mcmagic.omnira.block.entity.AdvancedCondensationTableBlockEntity(pos,state);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if(random.nextInt(3)!=0)return;
        for(int ring=0;ring<3;ring++) {
            double a=level.getGameTime()*.045*(ring%2==0?1:-1)+ring*2.1;
            var color=ring==1?new org.joml.Vector3f(.75F,.55F,1):new org.joml.Vector3f(.7F,.95F,1);
            level.addParticle(new DustParticleOptions(color,.55F),pos.getX()+.5+Math.cos(a)*.25,pos.getY()+.2+ring*.11+Math.sin(a*2)*.035,pos.getZ()+.5+Math.sin(a)*.25,0,0,0);
        }
    }
}
