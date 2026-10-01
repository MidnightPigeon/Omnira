package com.mcmagic.omnira.world.structure;

import com.mcmagic.omnira.registry.DreamContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

public final class MirrorGalleryPiece extends TemplateStructurePiece {
    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(17,0,17))
                .setIgnoreEntities(true).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }
    public MirrorGalleryPiece(StructureTemplateManager manager,BlockPos origin,Rotation rotation,int variant) {
        super(RuinsStructure.GALLERY_PIECE.get(),0,manager,ResourceLocation.fromNamespaceAndPath("omnira","authored/mirror_gallery_"+variant),
                "omnira:authored/mirror_gallery_"+variant,settings(rotation),origin);
    }
    public MirrorGalleryPiece(StructurePieceSerializationContext context,CompoundTag tag) {
        super(RuinsStructure.GALLERY_PIECE.get(),tag,context.structureTemplateManager(),id->settings(Rotation.valueOf(tag.getString("Rotation"))));
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        super.addAdditionalSaveData(context,tag);tag.putString("Rotation",getRotation().name());
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,net.minecraft.world.level.chunk.ChunkGenerator generator,
                                      RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        super.postProcess(level,manager,generator,random,clip,chunk,pivot);
        // Only shallow footings under the authored floor; never fill the courtyard or a void to bedrock.
        for(var block:java.util.List.of(DreamContent.ENGRAVED_MIRROR_ROCK.get(),DreamContent.SPIRITUAL_CRYSTAL_BLOCK.get(),DreamContent.DREAM_CRYSTAL_BLOCK.get()))
            for(var cell:template.filterBlocks(templatePosition,placeSettings,block)) {
                if(cell.pos().getY()!=templatePosition.getY()+1)continue;
                for(int depth=1;depth<=4;depth++) {
                    var pos=cell.pos().below(depth);
                    if(!clip.isInside(pos) || !level.isEmptyBlock(pos))break;
                    level.setBlock(pos,DreamContent.MIRROR_ROCK.get().defaultBlockState(),2);
                }
            }
    }
    @Override protected void handleDataMarker(String name,BlockPos pos,ServerLevelAccessor level,RandomSource random,BoundingBox box){}
}
