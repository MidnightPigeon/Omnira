package com.mcmagic.omnira.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

public final class RadiantCourtyardPiece extends TemplateStructurePiece {
    private final long columnSeed;
    private final boolean randomColumns;
    public static final ResourceLocation TEMPLATE=ResourceLocation.fromNamespaceAndPath("omnira","authored/radiant_courtyard");
    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(8,0,8))
                .setIgnoreEntities(true).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
    }
    public RadiantCourtyardPiece(StructureTemplateManager manager,BlockPos pos,Rotation rotation) {
        this(manager,pos,rotation,pos.asLong());
    }
    public RadiantCourtyardPiece(StructureTemplateManager manager,BlockPos pos,Rotation rotation,long columnSeed) {
        super(RuinsStructure.COURTYARD_PIECE.get(),0,manager,TEMPLATE,TEMPLATE.toString(),settings(rotation),pos);
        this.columnSeed=columnSeed;this.randomColumns=true;
    }
    public RadiantCourtyardPiece(StructurePieceSerializationContext context,CompoundTag tag) {
        super(RuinsStructure.COURTYARD_PIECE.get(),tag,context.structureTemplateManager(),
                id->settings(Rotation.valueOf(tag.getString("Rotation"))));
        columnSeed=tag.getLong("ColumnSeed");randomColumns=tag.contains("ColumnSeed");
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        super.addAdditionalSaveData(context,tag);tag.putString("Rotation",getRotation().name());
        if(randomColumns)tag.putLong("ColumnSeed",columnSeed);
    }
    @Override public void postProcess(net.minecraft.world.level.WorldGenLevel level,net.minecraft.world.level.StructureManager manager,
                                      net.minecraft.world.level.chunk.ChunkGenerator generator,RandomSource random,BoundingBox clip,
                                      net.minecraft.world.level.ChunkPos chunk,BlockPos pivot) {
        super.postProcess(level,manager,generator,random,clip,chunk,pivot);
        if(!randomColumns)return; // Old partially generated structures retain their original layout.
        for(var block:java.util.List.of(com.mcmagic.omnira.registry.DreamContent.CRYSTAL_COLUMN.get(),
                com.mcmagic.omnira.registry.DreamContent.CRYSTAL_COLUMN_BASE.get(),com.mcmagic.omnira.registry.DreamContent.CRYSTAL_COLUMN_CAPITAL.get()))
            for(var cell:template.filterBlocks(templatePosition,placeSettings,block))
                if(clip.isInside(cell.pos()))level.setBlock(cell.pos(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        CourtyardColumns.plan(columnSeed).forEach((local,state)->{
            var world=StructureTemplate.calculateRelativePosition(placeSettings,local).offset(templatePosition);
            if(clip.isInside(world))level.setBlock(world,state.rotate(getRotation()),2);
        });
    }
    @Override protected void handleDataMarker(String name,BlockPos pos,ServerLevelAccessor level,RandomSource random,BoundingBox box) {}
}
