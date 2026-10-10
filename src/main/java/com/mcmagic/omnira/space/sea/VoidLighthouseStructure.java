package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.space.FoldedTerrain;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

public final class VoidLighthouseStructure extends Structure {
    public static final MapCodec<VoidLighthouseStructure> CODEC = simpleCodec(VoidLighthouseStructure::new);
    public VoidLighthouseStructure(StructureSettings settings) { super(settings); }
    @Override public StructureType<?> type() { return RuinsStructure.LIGHTHOUSE_TYPE.get(); }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext c) {
        int x=c.chunkPos().getMiddleBlockX(), z=c.chunkPos().getMiddleBlockZ();
        for (int dx=-12; dx<=12; dx++) for (int dz=-12; dz<=12; dz++) {
            if (!VoidSeaTerrain.biome(x+dx,z+dz) || VoidSeaTerrain.floor(x+dx,z+dz)>=VoidSeaTerrain.SEA_LEVEL) return Optional.empty();
            if (Math.abs(dx)<=6 && Math.abs(dz)<=6 && FoldedTerrain.roof(x+dx,z+dz)<=VoidLighthousePiece.BASE_Y+22) return Optional.empty();
        }
        BlockPos origin = new BlockPos(x-12,VoidLighthousePiece.BASE_Y,z-12);
        return Optional.of(new GenerationStub(origin,b -> b.addPiece(new VoidLighthousePiece(origin))));
    }
}
