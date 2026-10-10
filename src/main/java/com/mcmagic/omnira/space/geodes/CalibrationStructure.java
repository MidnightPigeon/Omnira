package com.mcmagic.omnira.space.geodes;

import com.mcmagic.omnira.world.structure.RuinsStructure;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

public final class CalibrationStructure extends Structure {
    public static final MapCodec<CalibrationStructure> CODEC=simpleCodec(CalibrationStructure::new);
    public CalibrationStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.CALIBRATION_TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        int x=context.chunkPos().getMiddleBlockX()-24,z=context.chunkPos().getMiddleBlockZ()-19;
        int y=DislocatedTerrain.floor(x+16,z);
        for(int dx=3;dx<=44;dx+=2)for(int dz=0;dz<=38;dz+=2)
            if(!DislocatedTerrain.biome(x+dx,z+dz)||DislocatedTerrain.roof(x+dx,z+dz)<y+12)return Optional.empty();
        var origin=new BlockPos(x,y,z);
        return Optional.of(new GenerationStub(origin,b->b.addPiece(new CalibrationPiece(origin))));
    }
}
