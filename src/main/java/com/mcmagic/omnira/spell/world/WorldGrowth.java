package com.mcmagic.omnira.spell.world;

import com.mcmagic.omnira.block.CrystalFamilies;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class WorldGrowth {
    private static final class Crystals {
        static final java.util.Map<Block,Block[]> FAMILIES=new java.util.IdentityHashMap<>();
        static {for(var family:CrystalFamilies.all())for(int i=0;i<4;i++)FAMILIES.put(family[i],family);}
    }
    public static void mature(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);var block=state.getBlock();
        if(state.isAir())return;
        var family=Crystals.FAMILIES.get(block);
        if(family!=null){
            if(block==family[0]){for(var face:Direction.values())crystal(level,pos.relative(face),face,family);}
            else level.setBlockAndUpdate(pos,family[4].withPropertiesOf(state));return;
        }
        if(block instanceof SugarCaneBlock||block instanceof CactusBlock||block instanceof KelpBlock||block instanceof KelpPlantBlock){column(level,pos,state);return;}
        if(block instanceof CropBlock crop){level.setBlockAndUpdate(pos,crop.getStateForAge(crop.getMaxAge()));return;}
        if(block instanceof NetherWartBlock){level.setBlockAndUpdate(pos,state.setValue(NetherWartBlock.AGE,NetherWartBlock.MAX_AGE));return;}
        if(block instanceof CaveVines&&state.hasProperty(CaveVines.BERRIES)){level.setBlockAndUpdate(pos,state.setValue(CaveVines.BERRIES,true));return;}
        if(!(block instanceof BonemealableBlock)||!(block instanceof SaplingBlock||block instanceof MushroomBlock||block instanceof FungusBlock
                ||block instanceof StemBlock||block instanceof CocoaBlock||block instanceof SweetBerryBushBlock
                ||block instanceof PitcherCropBlock||state.is(net.minecraft.tags.BlockTags.CROPS)))return;
        if(!level.hasChunksAt(pos.offset(-8,-1,-8),pos.offset(8,16,8)))return;
        for(int i=0;i<32;i++){
            state=level.getBlockState(pos);if(!(state.getBlock() instanceof BonemealableBlock grow)||!grow.isValidBonemealTarget(level,pos,state))break;
            grow.performBonemeal(level,level.random,pos,state);
        }
    }
    private static void crystal(ServerLevel level,BlockPos pos,Direction face,Block[] family){
        if(!level.hasChunkAt(pos))return;var state=level.getBlockState(pos);
        if(!BuddingAmethystBlock.canClusterGrowAtState(state)&&(!state.hasProperty(AmethystClusterBlock.FACING)||state.getValue(AmethystClusterBlock.FACING)!=face))return;
        if(!BuddingAmethystBlock.canClusterGrowAtState(state)&&java.util.Arrays.stream(family).noneMatch(state::is))return;
        var mature=family[4].defaultBlockState().setValue(AmethystClusterBlock.FACING,face).setValue(AmethystClusterBlock.WATERLOGGED,state.getFluidState().is(Fluids.WATER));
        if(mature.canSurvive(level,pos))level.setBlockAndUpdate(pos,mature);
    }
    private static void column(ServerLevel level,BlockPos pos,BlockState state){
        boolean kelp=state.getBlock() instanceof KelpBlock||state.getBlock() instanceof KelpPlantBlock;
        var bottom=pos;
        for(int i=0;i<32;i++){
            var below=level.getBlockState(bottom.below());if(kelp?(below.is(Blocks.KELP)||below.is(Blocks.KELP_PLANT)):below.getBlock()==state.getBlock())bottom=bottom.below();else break;
        }
        for(int i=1;i<4;i++){
            var at=bottom.above(i);if(!level.hasChunkAt(at))break;var old=level.getBlockState(at);
            boolean same=kelp?(old.is(Blocks.KELP)||old.is(Blocks.KELP_PLANT)):old.getBlock()==state.getBlock();
            if(same)continue;
            if(kelp?!old.is(Blocks.WATER):!old.isAir())break;
            var grown=kelp?Blocks.KELP.defaultBlockState():state;
            if(!grown.canSurvive(level,at))break;
            if(kelp)level.setBlockAndUpdate(at.below(),Blocks.KELP_PLANT.defaultBlockState());
            level.setBlockAndUpdate(at,grown);
        }
    }
    private WorldGrowth(){}
}
