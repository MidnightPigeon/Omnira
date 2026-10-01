package com.mcmagic.omnira.world.structure;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.*;

public final class MirrorGalleryStructure extends Structure {
    public static final MapCodec<MirrorGalleryStructure> CODEC=simpleCodec(MirrorGalleryStructure::new);
    public MirrorGalleryStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return RuinsStructure.GALLERY_TYPE.get();}
    public static int surface(NoiseColumn column) {
        for(int y=138;y>=94;y--)if(!column.getBlock(y).isAir() && column.getBlock(y).getFluidState().isEmpty() && column.getBlock(y+1).isAir())return y;
        return -1;
    }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x=context.chunkPos().getMinBlockX(),z=context.chunkPos().getMinBlockZ();
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        // Scan the middle stratum, not the heightmap hidden by the dawn islands.
        for(int dx=2;dx<=32;dx+=5)for(int dz=2;dz<=32;dz+=5) {
            if(Math.hypot(dx-17,dz-17)>16)continue;
            var column=context.chunkGenerator().getBaseColumn(x+dx,z+dz,context.heightAccessor(),context.randomState());
            int y=surface(column);if(y<0)return Optional.empty();
            low=Math.min(low,y);high=Math.max(high,y);
            if(high-low>3)return Optional.empty();
        }
        // Local template floor is y=1; align it with the terrain's exposed surface.
        var origin=new BlockPos(x,high-1,z);
        var random=context.random();
        var piece=new MirrorGalleryPiece(context.structureTemplateManager(),origin,Rotation.getRandom(random),random.nextInt(4));
        return Optional.of(new GenerationStub(origin.offset(17,1,17),builder->builder.addPiece(piece)));
    }
}
