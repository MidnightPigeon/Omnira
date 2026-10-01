package com.mcmagic.omnira.block;

import com.mcmagic.omnira.registry.DreamContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;

public final class LightCrystalCoreBlock extends Block {
    public static final MapCodec<LightCrystalCoreBlock> CODEC=simpleCodec(LightCrystalCoreBlock::new);
    public static final BooleanProperty GROWING=BooleanProperty.create("growing");
    public LightCrystalCoreBlock(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(GROWING,false));}
    @Override protected MapCodec<? extends Block> codec() {return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(GROWING);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        level.setBlock(pos,state.setValue(GROWING,true),3);
    }
    @Override public boolean isRandomlyTicking(BlockState state) {return state.getValue(GROWING);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        for(int attempt=0;attempt<4;attempt++) {
            int x=random.nextInt(11)-5,y=random.nextInt(11)-5,z=random.nextInt(11)-5;
            if(x*x+y*y+z*z<=1 || x*x+y*y+z*z>25) continue;
            if(growAt(level,pos,pos.offset(x,y,z),random.nextInt(3)==0)) return;
        }
    }
    public static boolean growAt(ServerLevel level,BlockPos source,BlockPos target,boolean lightSource) {
        double distance=source.distSqr(target);
        if(distance<=1 || distance>25 || !level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target)
                || level.isOutsideBuildHeight(target) || !level.isEmptyBlock(target) || !level.getEntities(null,new AABB(target)).isEmpty()) return false;
        return level.setBlockAndUpdate(target,(lightSource?DreamContent.LIGHT_SOURCE_CRYSTAL:DreamContent.LIGHT_CONDENSATE).get().defaultBlockState());
    }
}
