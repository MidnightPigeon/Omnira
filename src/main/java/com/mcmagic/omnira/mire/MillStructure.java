package com.mcmagic.omnira.mire;

import com.mojang.serialization.MapCodec;
import com.mcmagic.omnira.spacetime.*;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

public final class MillStructure extends Structure {
    public static final MapCodec<MillStructure> CODEC=simpleCodec(MillStructure::new);
    public static int placementY(int highestSurface){return com.mcmagic.omnira.world.structure.AuthoredStructurePlacement.originY(highestSurface);}
    public MillStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.MILL_TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        int x=c.chunkPos().getMinBlockX(),z=c.chunkPos().getMinBlockZ(),high=Integer.MIN_VALUE;
        for(int dx=MillPlan.BUILDING_MIN_X;dx<=MillPlan.BUILDING_MAX_X;dx++)
            for(int dz=MillPlan.BUILDING_MIN_Z;dz<=MillPlan.BUILDING_MAX_Z;dz++){
            if(!StrataBiomeSource.reversionMire(x+dx,z+dz))return Optional.empty();
            high=Math.max(high,CorridorLayout.timeSurface(x+dx,z+dz));
        }
        int base=placementY(high);
        if(base+MillPlan.MAX_Y>=c.heightAccessor().getMaxBuildHeight())return Optional.empty();
        var origin=new BlockPos(x,base,z);return Optional.of(new GenerationStub(origin.offset(27,2,19),b->b.addPiece(new MillPiece(origin))));
    }
}
