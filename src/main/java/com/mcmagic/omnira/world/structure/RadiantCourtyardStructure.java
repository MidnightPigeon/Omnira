package com.mcmagic.omnira.world.structure;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** A freestanding authored island, never projected onto the topmost heightmap. */
public final class RadiantCourtyardStructure extends Structure {
    public static final MapCodec<RadiantCourtyardStructure> CODEC=simpleCodec(RadiantCourtyardStructure::new);
    public static final int MIN_FLOOR_Y=174, MAX_FLOOR_Y=218, FLOOR_OFFSET=6;
    public RadiantCourtyardStructure(StructureSettings settings) {super(settings);}
    @Override public StructureType<?> type() {return RuinsStructure.COURTYARD_TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var random=context.random();
        int floor=MIN_FLOOR_Y+random.nextInt(MAX_FLOOR_Y-MIN_FLOOR_Y+1);
        var origin=new BlockPos(context.chunkPos().getMinBlockX(),floor-FLOOR_OFFSET,context.chunkPos().getMinBlockZ());
        var rotation=Rotation.getRandom(random);
        var piece=new RadiantCourtyardPiece(context.structureTemplateManager(),origin,rotation,random.nextLong());
        var box=piece.getBoundingBox();
        if(box.minY()<context.heightAccessor().getMinBuildHeight() || box.maxY()>=context.heightAccessor().getMaxBuildHeight())
            return Optional.empty();
        // Rare candidates only: reject occupied terrain instead of cutting a courtyard into an island.
        for(int x=box.minX();x<=box.maxX();x++) for(int z=box.minZ();z<=box.maxZ();z++) {
            var column=context.chunkGenerator().getBaseColumn(x,z,context.heightAccessor(),context.randomState());
            for(int y=box.minY();y<=box.maxY();y++) if(!column.getBlock(y).isAir()) return Optional.empty();
        }
        return Optional.of(new GenerationStub(new BlockPos(origin.getX()+8,floor,origin.getZ()+8),
                builder->builder.addPiece(piece)));
    }
}
