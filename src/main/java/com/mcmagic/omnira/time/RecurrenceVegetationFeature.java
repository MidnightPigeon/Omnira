package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mcmagic.omnira.registry.DreamContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class RecurrenceVegetationFeature extends Feature<NoneFeatureConfiguration> {
    public RecurrenceVegetationFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var level=context.level();var random=context.random();boolean placed=false;
        for(int i=0;i<22;i++){
            int x=context.origin().getX()+random.nextInt(16),z=context.origin().getZ()+random.nextInt(16);
            var pos=new BlockPos(x,level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z),z);
            if(!level.getBiome(pos).is(RecurrenceGarden.BIOME)||!TimePlantBlock.canGrowAt(level,pos))continue;
            level.setBlock(pos,(random.nextBoolean()?TimeNatureContent.FLOWER:TimeNatureContent.GRASS).get().defaultBlockState(),3);
            placed=true;
        }
        if(random.nextInt(3)==0){
            int x=context.origin().getX()+random.nextInt(15),z=context.origin().getZ()+random.nextInt(15);
            var direction=random.nextBoolean()?Direction.EAST:Direction.SOUTH;
            var first=new BlockPos(x,level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z),z);
            var second=first.relative(direction);
            if(canDecorate(level,first)&&canDecorate(level,second)){
                level.setBlock(first,TimeNatureContent.FENCE.get().defaultBlockState(),3);
                level.setBlock(second,TimeNatureContent.FENCE.get().defaultBlockState(),3);
                placed=true;
            }
        }
        if(random.nextInt(4)==0){
            int x=context.origin().getX()+random.nextInt(16),z=context.origin().getZ()+random.nextInt(16);
            var pos=new BlockPos(x,level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z),z);
            if(canDecorate(level,pos)&&level.isEmptyBlock(pos.above())
                    &&level.getBiome(pos.above()).is(RecurrenceGarden.BIOME)){
                var lower=DreamContent.LIGHT_CRYSTAL_TORCH.get().defaultBlockState();
                level.setBlock(pos,lower.setValue(DoublePlantBlock.HALF,DoubleBlockHalf.LOWER),3);
                level.setBlock(pos.above(),lower.setValue(DoublePlantBlock.HALF,DoubleBlockHalf.UPPER),3);
                placed=true;
            }
        }
        return placed;
    }

    private static boolean canDecorate(net.minecraft.world.level.WorldGenLevel level,BlockPos pos){
        return level.getBiome(pos).is(RecurrenceGarden.BIOME)&&TimePlantBlock.canGrowAt(level,pos);
    }
}
