package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.time.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SaplingBlock.class)
abstract class TemporalSaplingMixin extends BushBlock {
    @Shadow @Final protected TreeGrower treeGrower;
    protected TemporalSaplingMixin(Properties p){super(p);}
    @Override protected boolean mayPlaceOn(BlockState soil,BlockGetter level,BlockPos pos){return soil.is(TemporalSoils.SILTS)||super.mayPlaceOn(soil,level,pos);}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){
        super.onPlace(state,level,pos,old,moved);
        if(!level.isClientSide&&!old.is(state.getBlock())&&!(state.getBlock() instanceof TimeSaplingBlock)&&level.getBlockState(pos.below()).is(TemporalSoils.SILTS))level.scheduleTick(pos,state.getBlock(),1);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(state.getBlock() instanceof TimeSaplingBlock)return;
        var soil=level.getBlockState(pos.below());
        if(!soil.is(TemporalSoils.SILTS)||!random.nextBoolean())return;
        level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),2);
        if(!treeGrower.growTree(level,level.getChunkSource().getGenerator(),pos,state,random))level.setBlockAndUpdate(pos.below(),soil);
    }
    @Inject(method="advanceTree",at=@At("HEAD"),cancellable=true)
    private void omnira$dormant(ServerLevel level,BlockPos pos,BlockState state,RandomSource random,CallbackInfo ci){
        if(!(state.getBlock() instanceof TimeSaplingBlock)&&level.getBlockState(pos.below()).is(TemporalSoils.SILTS))ci.cancel();
    }
}
