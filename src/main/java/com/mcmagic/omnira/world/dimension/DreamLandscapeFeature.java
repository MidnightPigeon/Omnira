package com.mcmagic.omnira.world.dimension;

import com.mcmagic.omnira.registry.DreamContent;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Scan each altitude band separately; the topmost heightmap hides lower islands. */
public final class DreamLandscapeFeature extends Feature<NoneFeatureConfiguration> {
    public DreamLandscapeFeature() {super(NoneFeatureConfiguration.CODEC);}
    private static BlockPos surface(WorldGenLevel level,int x,int z,int low,int high,Block block) {
        for(int y=high;y>=low;y--) {
            var pos=new BlockPos(x,y,z);
            if(level.getBlockState(pos).is(block) && level.isEmptyBlock(pos.above())) return pos;
        }
        return null;
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level=context.level();var random=context.random();var origin=context.origin();
        int x=origin.getX()+random.nextInt(16),z=origin.getZ()+random.nextInt(16);
        if(random.nextInt(18)==0) {
            var ground=surface(level,x,z,146,235,DreamContent.LIGHT_CONDENSATE.get());
            if(ground!=null) lightSphere(level,ground,random);
        }
        int treeAttempts=random.nextBoolean()?1:0;
        for(int attempt=0;attempt<treeAttempts;attempt++) {
            x=origin.getX()+random.nextInt(16);z=origin.getZ()+random.nextInt(16);
            var ground=surface(level,x,z,12,76,DreamContent.MOSSY_SHADOW_ROCK.get());
            if(ground==null) continue;
            // Vanilla jungle tree placement keeps its branching and leaf-distance handling.
            var key=ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("omnira",random.nextInt(4)==0?"shadow_mega_tree":"shadow_tree"));
            var tree=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolderOrThrow(key);
            if(tree.value().place(level,context.chunkGenerator(),random,ground.above())) {
                for(int y=1;y<=5;y++) for(Direction face:Direction.Plane.HORIZONTAL) {
                    var fruit=ground.above(y).relative(face);
                    if(random.nextInt(3)==0 && level.isEmptyBlock(fruit)) {
                        var state=DreamContent.SHADOW_BERRIES.get().defaultBlockState().setValue(CocoaBlock.FACING,face.getOpposite()).setValue(CocoaBlock.AGE,2);
                        if(state.canSurvive(level,fruit)) level.setBlock(fruit,state,2);
                    }
                }
            }
        }
        for(int attempt=0;attempt<2;attempt++) {
            x=origin.getX()+random.nextInt(16);z=origin.getZ()+random.nextInt(16);
            var ground=surface(level,x,z,94,138,DreamContent.MIRROR_ROCK.get());
            if(ground==null) continue;
            boolean isolated=true;
            for(var p:BlockPos.betweenClosed(ground.offset(-2,-2,-2),ground.offset(2,2,2)))
                if(level.getBlockState(p).is(DreamContent.DREAM_CRYSTAL_BEDROCK.get())) isolated=false;
            if(!isolated) continue;
            level.setBlock(ground,DreamContent.DREAM_CRYSTAL_BEDROCK.get().defaultBlockState(),2);
            var stage=switch(random.nextInt(4)) {
                case 0 -> DreamContent.SMALL_DREAM_BUD; case 1 -> DreamContent.MEDIUM_DREAM_BUD;
                case 2 -> DreamContent.LARGE_DREAM_BUD; default -> DreamContent.DREAM_CRYSTAL;
            };
            level.setBlock(ground.above(),stage.get().defaultBlockState(),2);
        }
        return true;
    }
    private static void lightSphere(WorldGenLevel level,BlockPos ground,RandomSource random) {
        int roll=random.nextInt(20),core=roll==0?3:roll<6?2:1,radius=core==3?8:core==2?5:3;
        // Require a supported footprint, avoiding spheres stranded over an island edge.
        for(Direction face:Direction.Plane.HORIZONTAL) {
            var probe=ground.relative(face,Math.max(2,radius/2));
            if(surface(level,probe.getX(),probe.getZ(),ground.getY()-4,ground.getY()+4,DreamContent.LIGHT_CONDENSATE.get())==null) return;
        }
        var center=ground.above(Math.max(2,radius*2/3));
        double offset=core%2==0?.5:0;
        int min=-core/2,max=min+core;
        for(int dx=-radius;dx<=radius;dx++) for(int dy=-radius;dy<=radius;dy++) for(int dz=-radius;dz<=radius;dz++) {
            double distance=(dx+offset)*(dx+offset)+(dy+offset)*(dy+offset)+(dz+offset)*(dz+offset);
            if(distance>radius*radius) continue;
            var pos=center.offset(dx,dy,dz);
            var old=level.getBlockState(pos);
            if(!old.isAir() && !old.is(DreamContent.LIGHT_CONDENSATE.get())) continue;
            boolean inside=dx>=min && dx<max && dy>=min && dy<max && dz>=min && dz<max;
            if(inside) level.setBlock(pos,DreamContent.LIGHT_CRYSTAL_CORE.get().defaultBlockState(),2);
            else if(distance>=Math.pow(radius-(radius>=5?2.5:1.5),2)) level.setBlock(pos,DreamContent.LIGHT_SOURCE_CRYSTAL.get().defaultBlockState(),2);
            else if(old.is(DreamContent.LIGHT_CONDENSATE.get())) level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
    }
}
