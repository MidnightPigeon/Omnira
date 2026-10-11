package com.mcmagic.omnira.space.phase;

import com.mcmagic.omnira.space.FoldedTerrain;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

public final class PhaseGreenhouseStructure extends Structure {
    public static final MapCodec<PhaseGreenhouseStructure> CODEC=simpleCodec(PhaseGreenhouseStructure::new);
    public PhaseGreenhouseStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.PHASE_GREENHOUSE_TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        int x=context.chunkPos().getMiddleBlockX()-22,z=context.chunkPos().getMiddleBlockZ()-9;
        int y=FoldedTerrain.floor(x+22,z+9);
        for(var cell:PhaseGreenhousePiece.CELLS){
            int px=x+cell.pos().getX(),pz=z+cell.pos().getZ();
            if(!PhaseTerrain.biome(px,pz)||FoldedTerrain.roof(px,pz)<=y+10)return Optional.empty();
        }
        var origin=new BlockPos(x,y,z);
        return Optional.of(new GenerationStub(origin,b->b.addPiece(new PhaseGreenhousePiece(origin))));
    }
}
