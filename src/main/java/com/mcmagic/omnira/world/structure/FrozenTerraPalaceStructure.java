package com.mcmagic.omnira.world.structure;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import com.mcmagic.omnira.spacetime.CorridorLayout;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class FrozenTerraPalaceStructure extends Structure {
    public static final MapCodec<FrozenTerraPalaceStructure> CODEC=simpleCodec(FrozenTerraPalaceStructure::new);
    public FrozenTerraPalaceStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.TERRA_PALACE_TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        int x=context.chunkPos().getMinBlockX(),z=context.chunkPos().getMinBlockZ();
        int y=Integer.MIN_VALUE;
        for(int dx=0;dx<FrozenTerraPalacePlan.WIDTH;dx++)for(int dz=0;dz<FrozenTerraPalacePlan.DEPTH;dz++){
            if(!com.mcmagic.omnira.spacetime.StrataBiomeSource.frozenTerra(x+dx,z+dz))return Optional.empty();
            y=Math.max(y,CorridorLayout.timeSurface(x+dx,z+dz));
        }
        y=AuthoredStructurePlacement.originY(y);
        if(y+FrozenTerraPalacePlan.HEIGHT>context.heightAccessor().getMaxBuildHeight())return Optional.empty();
        BlockPos origin=new BlockPos(x,y,z);
        return Optional.of(new GenerationStub(origin.offset(24,1,32),builder->builder.addPiece(new FrozenTerraPalacePiece(origin))));
    }
}
