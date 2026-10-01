package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.world.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_courtyard_columns")
@PrefixGameTestTemplate(false)
public final class CourtyardColumnsGameTests {
    @GameTest(template="spell_arena",timeoutTicks=300)
    public static void variedColumnsPreserveAuthoredContentsAcrossChunks(GameTestHelper h) {
        var level=h.getLevel();var template=level.getStructureManager().getOrCreate(RadiantCourtyardPiece.TEMPLATE);
        var tag=template.save(new net.minecraft.nbt.CompoundTag());var palette=tag.getList("palette",10);
        var protectedCells=new java.util.HashSet<BlockPos>();var oldColumns=new java.util.HashSet<BlockPos>();
        for(var value:tag.getList("blocks",10)) {
            var cell=(net.minecraft.nbt.CompoundTag)value;var xyz=cell.getList("pos",3);
            var pos=new BlockPos(xyz.getInt(0),xyz.getInt(1),xyz.getInt(2));
            var name=palette.getCompound(cell.getInt("state")).getString("Name");
            if(name.startsWith("omnira:crystal_column"))oldColumns.add(pos);
            else if(!name.equals("minecraft:air"))protectedCells.add(pos);
        }
        var unique=new java.util.HashSet<Integer>();boolean complete=false,broken=false,fallen=false;
        for(int seed=0;seed<100;seed++) {
            var cells=CourtyardColumns.plan(seed);unique.add(cells.hashCode());
            for(var entry:cells.entrySet()) {
                var p=entry.getKey();h.assertTrue(!protectedCells.contains(p),"Column overwrites authored decoration");
                if(p.getY()==7)h.assertTrue(protectedCells.contains(p.below()),"Fallen column unsupported");
            }
            complete|=cells.containsKey(new BlockPos(4,11,4));
            broken|=!cells.containsKey(new BlockPos(4,11,4)) && cells.containsKey(new BlockPos(4,8,4));
            fallen|=cells.containsKey(new BlockPos(4,7,5));
        }
        h.assertTrue(unique.size()>90 && complete && broken && fallen,"Column variants are fixed or incomplete");
        for(var rotation:Rotation.values()) {
            long seed=723L+rotation.ordinal();var origin=h.absolutePos(new BlockPos(50,20+20*rotation.ordinal(),50));
            var piece=new RadiantCourtyardPiece(level.getStructureManager(),origin,rotation,seed);var box=piece.getBoundingBox();
            var context=StructurePieceSerializationContext.fromLevel(level);var saved=piece.createTag(context);
            for(var clip:new BoundingBox[]{
                    new BoundingBox(box.minX()+8,box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ()),
                    new BoundingBox(box.minX(),box.minY(),box.minZ(),box.minX()+7,box.maxY(),box.maxZ())}) {
                var loaded=new RadiantCourtyardPiece(context,saved);
                h.assertTrue(saved.equals(loaded.createTag(context)),"Column seed lost on reload");
                loaded.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),RandomSource.create(123),clip,new ChunkPos(origin),origin);
            }
            var settings=new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(8,0,8));
            var cells=CourtyardColumns.plan(seed);
            cells.forEach((local,state)->h.assertTrue(level.getBlockState(StructureTemplate.calculateRelativePosition(settings,local).offset(origin))==state.rotate(rotation),"Clipped/rotated variant differs"));
            for(var local:oldColumns)if(!cells.containsKey(local))
                h.assertTrue(level.getBlockState(StructureTemplate.calculateRelativePosition(settings,local).offset(origin)).isAir(),"Old fixed column remains");
            var ball=level.getBlockEntity(origin.offset(8,5,8));
            h.assertTrue(ball!=null && ball.saveWithoutMetadata(level.registryAccess()).getString("LootTable").equals("omnira:chests/radiant_courtyard"),"Courtyard loot lost");
        }
        h.succeed();
    }
}
