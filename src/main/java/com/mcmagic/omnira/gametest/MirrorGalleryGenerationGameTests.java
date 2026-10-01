package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.world.structure.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_gallery_generation")
@PrefixGameTestTemplate(false)
public final class MirrorGalleryGenerationGameTests {
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("omnira",path);}
    @GameTest(template="spell_arena",timeoutTicks=1200) public static void actualMiddleLayerGeneration(GameTestHelper h) throws Exception {
        var level=h.getLevel();var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id("mirror_gallery"));
        h.assertTrue(structure instanceof MirrorGalleryStructure,"Missing gallery registration");
        h.assertTrue(structure.biomes().size()==1 && structure.biomes().get(0).is(ResourceKey.create(Registries.BIOME,id("mirror_dream_border"))),"Wrong gallery biome");
        net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator gen;
        try(var reader=new java.io.InputStreamReader(MirrorGalleryGenerationGameTests.class.getResourceAsStream("/data/omnira/dimension/dream_realm.json"))) {
            gen=(net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator)net.minecraft.world.level.chunk.ChunkGenerator.CODEC.parse(
                    RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().get("generator")).getOrThrow();
        }
        var randomState=net.minecraft.world.level.levelgen.RandomState.create(gen.generatorSettings().value(),level.registryAccess().registryOrThrow(Registries.NOISE).asLookup(),level.getSeed());
        var height=net.minecraft.world.level.LevelHeightAccessor.create(0,256);int found=0;
        for(int i=0;i<2048 && found<3;i++) {
            var start=structure.generate(level.registryAccess(),gen,gen.getBiomeSource(),randomState,level.getStructureManager(),level.getSeed(),
                    new net.minecraft.world.level.ChunkPos(i*32-2048,i*17-1024),0,height,structure.biomes()::contains);
            if(!start.isValid())continue;
            found++;var box=start.getBoundingBox();h.assertTrue(box.minY()>=93&&box.maxY()<146,"Gallery placed outside middle ground layer");
            var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
            var piece=(MirrorGalleryPiece)start.getPieces().getFirst();var tag=piece.createTag(context);
            h.assertTrue(tag.equals(new MirrorGalleryPiece(context,tag).createTag(context)),"Variant or rotation lost during reload");
        }
        h.assertTrue(found>0,"No viable gallery locations in actual dream terrain");h.succeed();
    }
    @GameTest(template="spell_arena") public static void variantsPreservePlayerLampsAndLoot(GameTestHelper h) {
        var fingerprints=new java.util.HashSet<Integer>();
        for(int i=0;i<4;i++) {
            var template=h.getLevel().getStructureManager().get(id("authored/mirror_gallery_"+i)).orElseThrow();
            var tag=template.save(new net.minecraft.nbt.CompoundTag());fingerprints.add(tag.hashCode());
            var palette=tag.getList("palette",10);int balls=0,lamps=0;
            for(var value:tag.getList("blocks",10)) {
                var b=(net.minecraft.nbt.CompoundTag)value;var state=palette.getCompound(b.getInt("state"));
                if(state.getString("Name").equals("omnira:crystal_ball")) {
                    balls++;h.assertTrue(b.getCompound("nbt").contains("LootTable")&&!b.getCompound("nbt").contains("Items"),"Rolled loot leaked into template");
                }
                if(state.getString("Name").equals("omnira:light_crystal_torch") && state.getCompound("Properties").getString("half").equals("lower")) {
                    var p=b.getList("pos",3);lamps++;
                    h.assertTrue((p.getInt(0)==5||p.getInt(0)==29)&&(p.getInt(2)==13||p.getInt(2)==21),"Player lamp edit lost");
                }
            }
            h.assertTrue(balls==3 && lamps==4,"Variant lost loot or lighting");
            for(var rotation:Rotation.values()) {
                var piece=new MirrorGalleryPiece(h.getLevel().getStructureManager(),net.minecraft.core.BlockPos.ZERO,rotation,i);
                h.assertTrue(piece.getBoundingBox().getXSpan()==35 && piece.getBoundingBox().getZSpan()==35,"Rotated template footprint shifted");
            }
        }
        h.assertTrue(fingerprints.size()==4,"Damage variants identical");h.succeed();
    }
    @GameTest(template="spell_arena") public static void swordsKeepVanillaCobwebTools(GameTestHelper h) {
        for(var item:java.util.List.of(com.mcmagic.omnira.registry.ModItems.CRYSTALLIZED_NAIL.get(),com.mcmagic.omnira.registry.ModItems.ARCANE_NEEDLE.get())) {
            var stack=new net.minecraft.world.item.ItemStack(item);var web=net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState();
            h.assertTrue(item instanceof net.minecraft.world.item.SwordItem && stack.getDestroySpeed(web)>=15,"Ritual sword lost cobweb mining speed");
            h.assertTrue(stack.isCorrectToolForDrops(web),"Ritual sword lost cobweb drops");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=400) public static void clippedVariantsKeepLootAndLamps(GameTestHelper h) {
        var level=h.getLevel();var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
        for(int i=0;i<4;i++) {
            var origin=h.absolutePos(new net.minecraft.core.BlockPos(50,15+i*12,50));var rotation=Rotation.values()[i];
            var piece=new MirrorGalleryPiece(level.getStructureManager(),origin,rotation,i);var box=piece.getBoundingBox();var saved=piece.createTag(context);
            for(var clip:java.util.List.of(
                    new net.minecraft.world.level.levelgen.structure.BoundingBox(box.minX()+16,box.minY()-4,box.minZ(),box.maxX(),box.maxY(),box.maxZ()),
                    new net.minecraft.world.level.levelgen.structure.BoundingBox(box.minX(),box.minY()-4,box.minZ(),box.minX()+15,box.maxY(),box.maxZ())))
                new MirrorGalleryPiece(context,saved).postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(42),clip,new net.minecraft.world.level.ChunkPos(origin),origin);
            var settings=new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setRotation(rotation).setRotationPivot(new net.minecraft.core.BlockPos(17,0,17));
            for(var local:java.util.List.of(new net.minecraft.core.BlockPos(5,2,17),new net.minecraft.core.BlockPos(17,3,17),new net.minecraft.core.BlockPos(29,2,17))) {
                var pos=net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.calculateRelativePosition(settings,local).offset(origin);
                var entity=level.getBlockEntity(pos);h.assertTrue(entity!=null && entity.saveWithoutMetadata(level.registryAccess()).contains("LootTable"),"Clipped gallery lost loot");
            }
            for(int x:new int[]{5,29})for(int z:new int[]{13,21}) {
                var pos=net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.calculateRelativePosition(settings,new net.minecraft.core.BlockPos(x,2,z)).offset(origin);
                h.assertTrue(level.getBlockState(pos).is(com.mcmagic.omnira.registry.DreamContent.LIGHT_CRYSTAL_TORCH.get()) && level.getBlockState(pos.above()).is(com.mcmagic.omnira.registry.DreamContent.LIGHT_CRYSTAL_TORCH.get()),"Clipped gallery lost relocated lamp");
            }
        }
        h.succeed();
    }
}
