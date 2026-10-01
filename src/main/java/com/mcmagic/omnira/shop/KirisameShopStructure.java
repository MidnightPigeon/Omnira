package com.mcmagic.omnira.shop;

import com.mojang.serialization.MapCodec;
import com.mcmagic.omnira.spacetime.*;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

public final class KirisameShopStructure extends Structure {
    public static final MapCodec<KirisameShopStructure> CODEC=simpleCodec(KirisameShopStructure::new);
    public KirisameShopStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.KIRISAME_TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        int x=context.chunkPos().getMinBlockX(),z=context.chunkPos().getMinBlockZ(),low=256,high=0;
        for(int dx=0;dx<KirisameShopPlan.WIDTH;dx++)for(int dz=0;dz<KirisameShopPlan.DEPTH;dz++){
            if(!StrataBiomeSource.fleetingWoods(x+dx,z+dz))return Optional.empty();
            int y=CorridorLayout.timeSurface(x+dx,z+dz);low=Math.min(low,y);high=Math.max(high,y);
        }
        int base=com.mcmagic.omnira.world.structure.AuthoredStructurePlacement.originY(high);
        if(high-low>5||base+KirisameShopPlan.HEIGHT>context.heightAccessor().getMaxBuildHeight())return Optional.empty();
        var origin=new BlockPos(x,base,z);
        return Optional.of(new GenerationStub(origin.offset(15,1,14),builder->builder.addPiece(new KirisameShopPiece(origin))));
    }
}
