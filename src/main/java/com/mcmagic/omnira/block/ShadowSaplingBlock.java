package com.mcmagic.omnira.block;

import com.mcmagic.omnira.registry.DreamContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

public final class ShadowSaplingBlock extends SaplingBlock {
    public static final TreeGrower GROWER=new TreeGrower("omnira:shadow",
            Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("omnira","shadow_mega_tree"))),
            Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("omnira","shadow_tree"))),Optional.empty());
    public static final MapCodec<ShadowSaplingBlock> CODEC=simpleCodec(ShadowSaplingBlock::new);
    public ShadowSaplingBlock(Properties properties) {super(GROWER,properties);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        var offset=state.getOffset(level,pos);
        return box(2,0,2,14,state.getValue(STAGE)==0?12:15,14).move(offset.x,offset.y,offset.z);
    }
    @Override public MapCodec<? extends SaplingBlock> codec() {return CODEC;}
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos) {return com.mcmagic.omnira.time.TemporalSoils.shadowTree(state);}
    @Override protected void randomTick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if(level.getBlockState(pos.below()).is(com.mcmagic.omnira.time.TemporalSoils.SILTS))return;
        if(level.getBlockState(pos.below()).is(com.mcmagic.omnira.time.TemporalSoils.SHADOW)||random.nextBoolean())super.randomTick(state,level,pos,random);
    }
    @Override public boolean isBonemealSuccess(net.minecraft.world.level.Level level,net.minecraft.util.RandomSource random,BlockPos pos,BlockState state) {
        var soil=level.getBlockState(pos.below());return !soil.is(com.mcmagic.omnira.time.TemporalSoils.SILTS)
                &&(soil.is(com.mcmagic.omnira.time.TemporalSoils.SHADOW)||random.nextBoolean())&&super.isBonemealSuccess(level,random,pos,state);
    }
    @Override public void advanceTree(net.minecraft.server.level.ServerLevel level,BlockPos pos,BlockState state,net.minecraft.util.RandomSource random){
        var soil=level.getBlockState(pos.below());super.advanceTree(level,pos,state,random);
        if(soil.is(com.mcmagic.omnira.time.TemporalSoils.SHADOW)&&!level.getBlockState(pos).is(this))level.setBlockAndUpdate(pos.below(),soil);
    }
}
