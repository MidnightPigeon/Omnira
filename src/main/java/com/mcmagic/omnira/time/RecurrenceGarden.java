package com.mcmagic.omnira.time;

import com.mcmagic.omnira.item.CrystalGridItem;
import com.mcmagic.omnira.item.PrimarySpellAccessory;
import com.mcmagic.omnira.mire.DecayingTemporalSiltBlock;
import com.mcmagic.omnira.mire.MireContent;
import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class RecurrenceGarden {
    public static final int PERIOD=TimeBiomePulse.PERIOD;
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:recurrence_garden"));
    static void advanceGrid(net.minecraft.world.entity.player.Player player){
        var grid=PrimarySpellAccessory.equipped(player,"crystal_grid");
        if(grid.getItem() instanceof CrystalGridItem item)
            grid.set(ModDataComponents.GRID_CURSOR,(grid.getOrDefault(ModDataComponents.GRID_CURSOR,0)+1)%item.capacity);
    }

    public static void pulseChunk(ServerLevel level,int cx,int cz){
        var matrices=new ArrayList<BlockPos>();
        pulseChunk(level,cx,cz,matrices);
        for(var pos:matrices)growEmptyFaces(level,pos);
    }

    static void pulseChunk(ServerLevel level,int cx,int cz,ArrayList<BlockPos> matrices){
        var pos=new BlockPos.MutableBlockPos();
        for(int x=cx<<4;x<(cx<<4)+16;x++)for(int z=cz<<4;z<(cz<<4)+16;z++){
            pos.set(x,level.dimension().equals(ModDimensions.SPACETIME_CORRIDOR)?224:-60,z);
            if(!level.getBiome(pos).is(BIOME))continue;
            int floor=level.dimension().equals(ModDimensions.SPACETIME_CORRIDOR)?171:level.getMinBuildHeight();
            for(int y=floor;y<level.getMaxBuildHeight();y++){
                pos.setY(y);
                var state=level.getBlockState(pos);
                if(state.is(com.mcmagic.omnira.registry.ModBlocks.WONDERLAND_POKER_STAND.get())
                        &&state.getValue(WonderlandPokerStandBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                        &&level.getBlockEntity(pos) instanceof WonderlandPokerStandBlockEntity stand)stand.advanceGarden();
                var next=next(level,pos,state);
                if(next!=null)level.setBlock(pos,next,2);
                else if(isMatrix(state))matrices.add(pos.immutable());
            }
        }
    }

    static void finish(ServerLevel level,ArrayList<BlockPos> matrices){
        for(var pos:matrices)growEmptyFaces(level,pos);
    }

    private static boolean isMatrix(BlockState state){
        return state.is(DreamContent.DREAM_CRYSTAL_BEDROCK.get())||state.is(TimeNatureContent.SPATIAL_MATRIX.get())||state.is(Blocks.BUDDING_AMETHYST);
    }

    private static void growEmptyFaces(ServerLevel level,BlockPos pos){
        var matrix=level.getBlockState(pos);
        if(!isMatrix(matrix))return;
        for(var face:Direction.values()){
            var at=pos.relative(face);
            if(level.isOutsideBuildHeight(at)||!level.hasChunkAt(at)||!level.getBiome(at).is(BIOME))continue;
            var old=level.getBlockState(at);
            if(!old.isAir()&&!old.getFluidState().is(Fluids.WATER))continue;
            if(matrix.is(DreamContent.DREAM_CRYSTAL_BEDROCK.get()))com.mcmagic.omnira.block.DreamCrystalMatrixBlock.grow(level,pos,face);
            else if(matrix.is(TimeNatureContent.SPATIAL_MATRIX.get()))SpatialMatrixBlock.grow(level,pos,face);
            else level.setBlockAndUpdate(at,Blocks.SMALL_AMETHYST_BUD.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING,face)
                    .setValue(AmethystClusterBlock.WATERLOGGED,old.getFluidState().is(Fluids.WATER)));
        }
    }

    public static BlockState next(ServerLevel level,BlockPos pos,BlockState state){
        if(state.is(DreamContent.TEMPORAL_SILT.get()))return TimeNatureContent.SILT.get().defaultBlockState();
        if(state.is(DreamContent.LIVING_TEMPORAL_SILT.get()))return TimeNatureContent.LIVING_SILT.get().defaultBlockState();
        if(state.is(MireContent.SILT.get()))return DreamContent.TEMPORAL_SILT.get().defaultBlockState();
        if((state.is(TimeNatureContent.SILT.get())||state.is(TimeNatureContent.LIVING_SILT.get()))
                &&DecayingTemporalSiltBlock.touching(level,pos))return MireContent.SILT.get().defaultBlockState();
        if(state.is(TimeNatureContent.SILT.get()))return DreamContent.TEMPORAL_SILT.get().defaultBlockState();
        if(state.is(TimeNatureContent.LIVING_SILT.get()))return DreamContent.LIVING_TEMPORAL_SILT.get().defaultBlockState();
        if(state.is(TimeNatureContent.GRASS.get()))return TimeNatureContent.FLOWER.get().defaultBlockState();
        if(state.is(TimeNatureContent.FLOWER.get()))return TimeNatureContent.GRASS.get().defaultBlockState();
        if(state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state))return crop.getStateForAge(0);
        if(state.getBlock() instanceof NetherWartBlock&&state.getValue(NetherWartBlock.AGE)==3)return state.setValue(NetherWartBlock.AGE,0);
        if(state.getBlock() instanceof CocoaBlock&&state.getValue(CocoaBlock.AGE)==2)return state.setValue(CocoaBlock.AGE,0);
        if(state.getBlock() instanceof SweetBerryBushBlock&&state.getValue(SweetBerryBushBlock.AGE)==3)return state.setValue(SweetBerryBushBlock.AGE,0);
        if(state.getBlock() instanceof AmethystClusterBlock){
            Block[] dream={DreamContent.SMALL_DREAM_BUD.get(),DreamContent.MEDIUM_DREAM_BUD.get(),DreamContent.LARGE_DREAM_BUD.get(),DreamContent.DREAM_CRYSTAL.get()};
            Block[] spatial={TimeNatureContent.SMALL_SPATIAL_BUD.get(),TimeNatureContent.MEDIUM_SPATIAL_BUD.get(),TimeNatureContent.LARGE_SPATIAL_BUD.get(),TimeNatureContent.SPATIAL_CLUSTER.get()};
            Block[] vanilla={Blocks.SMALL_AMETHYST_BUD,Blocks.MEDIUM_AMETHYST_BUD,Blocks.LARGE_AMETHYST_BUD,Blocks.AMETHYST_CLUSTER};
            for(var stages:new Block[][]{dream,spatial,vanilla})for(int i=0;i<stages.length;i++)if(state.is(stages[i]))
                return stages[(i+1)%stages.length].defaultBlockState()
                        .setValue(AmethystClusterBlock.FACING,state.getValue(AmethystClusterBlock.FACING))
                        .setValue(AmethystClusterBlock.WATERLOGGED,state.getValue(AmethystClusterBlock.WATERLOGGED));
        }
        return null;
    }

    private RecurrenceGarden(){}
}
