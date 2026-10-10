package com.mcmagic.omnira.space.abyss;

import com.mcmagic.omnira.space.FoldedTerrain;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

public final class SkyreachStructure extends Structure {
    public static final MapCodec<SkyreachStructure> CODEC=simpleCodec(SkyreachStructure::new);
    public SkyreachStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.SKYREACH_TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        int x=context.chunkPos().getMiddleBlockX()-10,z=context.chunkPos().getMiddleBlockZ()-10;
        for(int dx=0;dx<=20;dx+=4)for(int dz=0;dz<=20;dz+=4)if(!AbyssTerrain.biome(x+dx,z+dz))return Optional.empty();
        int base=FoldedTerrain.floor(x+10,z+10),height=FoldedTerrain.roof(x+10,z+10)-base;
        var origin=new BlockPos(x,base,z);
        return Optional.of(new GenerationStub(origin,b->b.addPiece(new SkyreachPiece(origin,height))));
    }
}
