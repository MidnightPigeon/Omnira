package com.mcmagic.omnira.archaeology;

import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mcmagic.omnira.time.TimePlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Sparse surface grass, never underground foliage or trees. */
public final class EpochalGrassFeature extends Feature<NoneFeatureConfiguration> {
    public EpochalGrassFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var level=context.level();var random=context.random();boolean placed=false;
        for(int i=0;i<4;i++){
            int x=context.origin().getX()+random.nextInt(16),z=context.origin().getZ()+random.nextInt(16);
            var pos=new BlockPos(x,level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z),z);
            if(!level.getBiome(pos).is(net.minecraft.resources.ResourceLocation.parse("omnira:epochal_cliffs"))
                    ||!level.isEmptyBlock(pos)||!level.getFluidState(pos.below()).isEmpty())continue;
            if(level.getBlockState(pos.below()).is(ArchaeologyContent.SAND.get()))
                level.setBlock(pos.below(),TimeNatureContent.SILT.get().defaultBlockState(),2);
            if(TimePlantBlock.canGrowAt(level,pos)){
                level.setBlock(pos,TimeNatureContent.GRASS.get().defaultBlockState(),2);placed=true;
            }
        }
        return placed;
    }
}
