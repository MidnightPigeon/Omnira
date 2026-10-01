package com.mcmagic.omnira.world.structure;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.structure.*;

public final class ShadowLibraryStructure extends Structure {
    public static final MapCodec<ShadowLibraryStructure> CODEC=simpleCodec(ShadowLibraryStructure::new);
    public ShadowLibraryStructure(StructureSettings settings) {super(settings);}
    @Override public StructureType<?> type() {return RuinsStructure.LIBRARY_TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        long seed=context.random().nextLong();var layout=new ShadowLibraryLayout(seed);
        int x=context.chunkPos().getMiddleBlockX(),z=context.chunkPos().getMiddleBlockZ();
        // The dimension has three vertical layers: never use the upper islands' heightmap.
        var entrance=context.chunkGenerator().getBaseColumn(x-3,z-1,context.heightAccessor(),context.randomState());
        int surface=Math.min(93,context.heightAccessor().getMaxBuildHeight()-4);
        while(surface>context.heightAccessor().getMinBuildHeight()+12 && !solid(entrance,surface))surface--;
        if(!solid(entrance,surface) || !entrance.getBlock(surface+1).isAir())return Optional.empty();
        int floor=surface-12-context.random().nextInt(7);
        if(floor<context.heightAccessor().getMinBuildHeight()+2)return Optional.empty();
        var origin=new BlockPos(x,floor,z);
        // Require a solid floor and a buried roof everywhere, including connecting corridors.
        for(var p:layout.footprint()) {
            var column=context.chunkGenerator().getBaseColumn(x+p.getX(),z+p.getZ(),context.heightAccessor(),context.randomState());
            for(int y=floor-1;y<=floor+7;y++)if(!solid(column,y))return Optional.empty();
        }
        for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var column=context.chunkGenerator().getBaseColumn(x-3+d.getStepX(),z-1+d.getStepZ(),context.heightAccessor(),context.randomState());
            if(!solid(column,surface-1))return Optional.empty();
        }
        var piece=new ShadowLibraryPiece(origin,surface,seed);
        return Optional.of(new GenerationStub(origin,builder->builder.addPiece(piece)));
    }
    private static boolean solid(NoiseColumn column,int y) {
        var state=column.getBlock(y);return !state.isAir() && state.getFluidState().isEmpty();
    }
}
