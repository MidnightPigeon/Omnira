package com.mcmagic.omnira.mire;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.time.TemporalSoils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class MireVegetationFeature extends Feature<NoneFeatureConfiguration> {
    public MireVegetationFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        var l=c.level();var random=c.random();boolean placed=false;
        for(int i=0;i<24;i++){
            int x=c.origin().getX()+random.nextInt(16),z=c.origin().getZ()+random.nextInt(16);
            var p=new BlockPos(x,l.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z),z);
            if(!l.getBiome(p).is(MireCycle.BIOME)||!l.isEmptyBlock(p))continue;
            if(MireContent.fluid(l.getFluidState(p.below()))){
                l.setBlock(p,MireContent.DECAYED.get().defaultBlockState(),3);placed=true;
            }else if(com.mcmagic.omnira.time.TimePlantBlock.canGrowAt(l,p)){
                if(i<10){l.setBlock(p,TimeNatureContent.GRASS.get().defaultBlockState(),3);placed=true;}
            }
        }
        if(random.nextInt(3)==0)for(int i=0;i<8;i++){
            int x=c.origin().getX()+random.nextInt(16),z=c.origin().getZ()+random.nextInt(16);
            var p=new BlockPos(x,l.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z),z);
            if(!l.getBiome(p).is(MireCycle.BIOME)||!com.mcmagic.omnira.time.TimePlantBlock.canGrowAt(l,p))continue;
            if(DreamContent.TIME_TREE_FEATURE.get().place(new FeaturePlaceContext<>(java.util.Optional.empty(),l,c.chunkGenerator(),random,p,NoneFeatureConfiguration.INSTANCE))){placed=true;break;}
        }
        return placed;
    }
}
