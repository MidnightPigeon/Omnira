package com.mcmagic.omnira.time;

import com.mcmagic.omnira.spacetime.CorridorLayout;
import com.mcmagic.omnira.spacetime.StrataBiomeSource;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class LookingGlassGardenStructure extends Structure {
    public static final MapCodec<LookingGlassGardenStructure> CODEC=simpleCodec(LookingGlassGardenStructure::new);
    public LookingGlassGardenStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.GARDEN_TYPE.get();}

    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        int x=context.chunkPos().getMinBlockX(),z=context.chunkPos().getMinBlockZ();
        for(int dx=0;dx<LookingGlassGardenPlan.WIDTH;dx++)for(int dz=0;dz<LookingGlassGardenPlan.DEPTH;dz++){
            int px=x+dx,pz=z+dz;
            if(!StrataBiomeSource.recurrenceGarden(px,pz)||CorridorLayout.timeSurface(px,pz)!=224)
                return Optional.empty();
        }
        int base=224;
        if(base+LookingGlassGardenPlan.HEIGHT>=context.heightAccessor().getMaxBuildHeight())return Optional.empty();
        var origin=new BlockPos(x,base,z);
        return Optional.of(new GenerationStub(origin.offset(20,1,20),builder->builder.addPiece(new LookingGlassGardenPiece(origin))));
    }
}
