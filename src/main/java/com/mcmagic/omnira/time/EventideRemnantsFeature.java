package com.mcmagic.omnira.time;

import com.mcmagic.omnira.mire.MireContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** Sparse loose fragments, not an authored building or structure template. */
public final class EventideRemnantsFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] FACINGS={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    public EventideRemnantsFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var level=context.level();var random=context.random();var origin=context.origin();boolean placed=false;
        int baseX=origin.getX()&~15,baseZ=origin.getZ()&~15;
        for(int i=0;i<5;i++){
            int x=baseX+random.nextInt(16),z=baseZ+random.nextInt(16);
            var top=new BlockPos(x,level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z),z);
            if(!level.getBiome(top).is(EventideRuins.BIOME)||!level.isEmptyBlock(top))continue;
            var floor=level.getBlockState(top.below());
            if(!floor.is(MireContent.SILT.get())&&!floor.is(EventideMasonry.BRICKS.get())
                    &&!floor.is(EventideMasonry.CRACKED.get())&&!floor.is(EventideMasonry.SANDBOUND.get()))continue;
            var brick=random.nextInt(5)==0?EventideMasonry.SANDBOUND:random.nextInt(3)==0?EventideMasonry.CRACKED:EventideMasonry.BRICKS;
            level.setBlock(top.below(),brick.get().defaultBlockState(),2);
            if(i==0&&random.nextInt(3)==0){
                var direction=FACINGS[random.nextInt(4)];
                // Keep small broken wall strips within this chunk and off authored objects.
                for(int n=-1;n<=1;n++){
                    var p=top.relative(direction,n);
                    if((p.getX()>>4)!=(baseX>>4)||(p.getZ()>>4)!=(baseZ>>4)
                            ||!level.getBiome(p).is(EventideRuins.BIOME)||!level.isEmptyBlock(p)
                            ||!level.getBlockState(p.below()).isSolidRender(level,p.below()))continue;
                    level.setBlock(p,(n==0?EventideMasonry.CRACKED:EventideMasonry.BRICK_SLAB).get().defaultBlockState(),2);
                }
            }else if(random.nextInt(4)!=0){
                var fragment=random.nextInt(4)==0?EventideMasonry.BROKEN_PILLAR:EventideMasonry.DEBRIS;
                level.setBlock(top,fragment.get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING,FACINGS[random.nextInt(4)]),2);
            }
            placed=true;
        }
        return placed;
    }
}
