package com.mcmagic.omnira.archaeology;

import com.mojang.serialization.MapCodec;
import com.mcmagic.omnira.spacetime.*;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

public final class DrillingPlatformStructure extends Structure {
    public static final MapCodec<DrillingPlatformStructure> CODEC=simpleCodec(DrillingPlatformStructure::new);
    public DrillingPlatformStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.DRILL_PLATFORM_TYPE.get();}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        int x=context.chunkPos().getMinBlockX(),z=context.chunkPos().getMinBlockZ(),high=0;
        for(int dx=0;dx<17;dx++)for(int dz=0;dz<17;dz++){
            if(!StrataBiomeSource.epochalCliffs(x+dx,z+dz))return Optional.empty();
            high=Math.max(high,CorridorLayout.timeSurface(x+dx,z+dz));
        }
        if(high+15>=context.heightAccessor().getMaxBuildHeight())return Optional.empty();
        var origin=new BlockPos(x,high,z);
        return Optional.of(new GenerationStub(origin,b->b.addPiece(new DrillingPlatformPiece(origin))));
    }
}
