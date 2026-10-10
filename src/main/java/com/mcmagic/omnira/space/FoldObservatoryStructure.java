package com.mcmagic.omnira.space;

import com.mcmagic.omnira.world.structure.RuinsStructure;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

public final class FoldObservatoryStructure extends Structure {
    public static final MapCodec<FoldObservatoryStructure> CODEC=simpleCodec(FoldObservatoryStructure::new);
    public FoldObservatoryStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.FOLD_OBSERVATORY_TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        int x=context.chunkPos().getMinBlockX(),z=context.chunkPos().getMinBlockZ();
        int floor=0,ceiling=103;
        for(int dx=0;dx<=62;dx++)for(int dz=0;dz<=28;dz++){
            if(!FoldedTerrain.biome(x+dx,z+dz))return Optional.empty();
            if(dx<31)floor=Math.max(floor,FoldedTerrain.floor(x+dx,z+dz));
            else ceiling=Math.min(ceiling,FoldedTerrain.roof(x+dx,z+dz));
        }
        var lower=new BlockPos(x,floor,z);var upper=new BlockPos(x+32,ceiling-8,z+8);
        return Optional.of(new GenerationStub(lower,b->{
            b.addPiece(new FoldObservatoryPiece(lower,upper.offset(FoldObservatoryPlan.GATE),false));
            b.addPiece(new FoldObservatoryPiece(upper,lower.offset(FoldObservatoryPlan.GATE),true));
        }));
    }
}
