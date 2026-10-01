package com.mcmagic.omnira.time;

import com.mcmagic.omnira.spacetime.CorridorLayout;
import com.mcmagic.omnira.spacetime.StrataBiomeSource;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class DoomsdayRiteStructure extends Structure {
    public static final MapCodec<DoomsdayRiteStructure> CODEC=simpleCodec(DoomsdayRiteStructure::new);
    public DoomsdayRiteStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.DOOMSDAY_TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        int x=context.chunkPos().getMinBlockX(),z=context.chunkPos().getMinBlockZ();
        for(int dx=0;dx<DoomsdayRitePlan.WIDTH;dx+=4)for(int dz=0;dz<DoomsdayRitePlan.DEPTH;dz+=4)
            if(!StrataBiomeSource.eventideRuins(x+dx,z+dz)||CorridorLayout.timeSurface(x+dx,z+dz)!=224)return Optional.empty();
        if(224+DoomsdayRitePlan.HEIGHT>=context.heightAccessor().getMaxBuildHeight())return Optional.empty();
        var origin=new BlockPos(x,224,z);
        return Optional.of(new GenerationStub(origin.offset(16,1,16),builder->builder.addPiece(new DoomsdayRitePiece(origin))));
    }
}
