package com.mcmagic.omnira.aggregation;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;

public final class AggregationStructure {
    private static final java.util.Set<GlobalPos> CHANGING=new java.util.HashSet<>();
    public static boolean required(int part,BlockState state){
        if(part==4)return state.is(ModBlocks.CRYSTAL_PROCESSING_TABLE.get());
        if(part==1||part==7)return state.is(DreamContent.EXCITED_SPATIAL_CRYSTAL.get())||state.is(AggregationContent.SPATIAL_BLOCK.get());
        if(part==3||part==5)return state.is(DreamContent.LIVING_TEMPORAL_SILT.get())||state.is(TimeNatureContent.LIVING_SILT.get());
        return state.is(ModBlocks.INFUSED_CRYSTAL_CASING.get());
    }
    public static boolean activate(Level level,BlockPos center,Player player,ItemStack held){
        if(player.isShiftKeyDown()||player.isSpectator()||!held.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()))return false;
        for(Direction facing:new Direction[]{player.getDirection().getOpposite(),player.getDirection().getClockWise()}){
            boolean valid=true;
            for(int i=0;i<9;i++){
                var pos=AggregationLayout.part(center,facing,i);
                if(!level.hasChunkAt(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,held)
                        ||!required(i,level.getBlockState(pos))||(level.getBlockEntity(pos) instanceof Container c&&!c.isEmpty())){valid=false;break;}
            }
            if(!valid)continue;
            if(level.isClientSide)return true;
            if(!assemble(level,center,facing))return false;
            held.shrink(1);
            level.playSound(null,center,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,1,.7F);
            return true;
        }
        return false;
    }
    public static boolean assemble(Level level,BlockPos center,Direction facing){
        if(level.isClientSide)return false;
        var saved=new java.util.ArrayList<BlockSnapshot>();
        for(int i=0;i<9;i++)saved.add(BlockSnapshot.create(level.dimension(),level,AggregationLayout.part(center,facing,i)));
        var key=GlobalPos.of(level.dimension(),center);CHANGING.add(key);
        try{
            for(int i=0;i<9;i++)if(!level.setBlock(AggregationLayout.part(center,facing,i),AggregationContent.BLOCK.get().defaultBlockState()
                    .setValue(AggregationRingBlock.FACING,facing).setValue(AggregationRingBlock.PART,i),18)){
                for(var snapshot:saved)snapshot.restore(18);return false;
            }
            if(!(level.getBlockEntity(center) instanceof AggregationRingBlockEntity)||!intact(level,center,level.getBlockState(center))){
                for(var snapshot:saved)snapshot.restore(18);return false;
            }
            for(int i=0;i<9;i++)level.updateNeighborsAt(AggregationLayout.part(center,facing,i),AggregationContent.BLOCK.get());
            return true;
        }finally{CHANGING.remove(key);}
    }
    public static java.util.Set<BlockPos> parts(Level level,BlockPos center,BlockState controller){
        if(!controller.is(AggregationContent.BLOCK.get()))return java.util.Set.of();
        var facing=controller.getValue(AggregationRingBlock.FACING);var result=new java.util.HashSet<BlockPos>();
        for(int i=0;i<9;i++){
            var pos=AggregationLayout.part(center,facing,i);if(!level.hasChunkAt(pos))return java.util.Set.of();
            var state=level.getBlockState(pos);
            if(!state.is(AggregationContent.BLOCK.get())||state.getValue(AggregationRingBlock.PART)!=i
                    ||state.getValue(AggregationRingBlock.FACING)!=facing)return java.util.Set.of();
            result.add(pos);
        }
        return result;
    }
    public static boolean intact(Level level,BlockPos center,BlockState state){return parts(level,center,state).size()==9;}
    public static void remove(Level level,BlockPos clicked,BlockState state){
        if(level.isClientSide)return;
        var center=AggregationLayout.center(clicked,state);var key=GlobalPos.of(level.dimension(),center);
        if(CHANGING.contains(key)||!(level.getBlockEntity(center) instanceof AggregationRingBlockEntity machine))return;
        CHANGING.add(key);
        try{
            Containers.dropContents(level,center,machine);machine.clearContent();
            var facing=state.getValue(AggregationRingBlock.FACING);
            for(int i=0;i<9;i++){
                var pos=AggregationLayout.part(center,facing,i);var part=level.getBlockState(pos);
                if(part.is(AggregationContent.BLOCK.get())&&part.getValue(AggregationRingBlock.PART)==i
                        &&part.getValue(AggregationRingBlock.FACING)==facing){level.removeBlockEntity(pos);level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);}
            }
            for(int i=0;i<9;i++)level.updateNeighborsAt(AggregationLayout.part(center,facing,i),Blocks.AIR);
        }finally{CHANGING.remove(key);}
    }
    private AggregationStructure(){}
}
