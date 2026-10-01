package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.world.structure.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_overworld_ruins")
@PrefixGameTestTemplate(false)
public final class OverworldRuinsGameTests {
    @GameTest(template="spell_arena")
    public static void naturalPlacementEnabled(GameTestHelper h) throws java.io.IOException {
        try(var input=OverworldRuinsGameTests.class.getResourceAsStream("/data/omnira/worldgen/structure_set/overworld_ruins.json")) {
            var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(java.util.Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            h.assertTrue(json.getAsJsonObject("placement").get("frequency").getAsDouble()==1,"Natural ruins are disabled");
            h.assertTrue(json.getAsJsonArray("structures").size()==3,"Missing a natural ruin variant");
        }
        h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(ResourceLocation.fromNamespaceAndPath("omnira","overworld_ruins"))!=null,"Structure set failed to load");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void layouts(GameTestHelper h) throws java.io.IOException {
        var preview=new com.google.gson.JsonArray();
        for(int variant=0;variant<3;variant++) {
            long sampleSeed=42;
            if(variant==2) {
                for(sampleSeed=0;sampleSeed<1000;sampleSeed++) {
                    var sample=RuinsPiece.blueprint(2,sampleSeed);
                    if(sample.get(new BlockPos(5,17,3)).state().is(ModBlocks.ARCANE_ASSEMBLY_TABLE.get())
                            &&sample.get(new BlockPos(7,17,3)).state().is(ModBlocks.CRYSTAL_PROCESSING_TABLE.get()))break;
                }
                h.assertTrue(sampleSeed<1000,"No complete tower sample found");
            }
            var plan=RuinsPiece.blueprint(variant,sampleSeed);
            h.assertTrue(plan.equals(RuinsPiece.blueprint(variant,sampleSeed)),"Blueprint changed on repeat");
            long chests=plan.values().stream().filter(c->c.loot()!=null).count();
            h.assertTrue(chests==variant+1,"Wrong chest count: "+variant);
            if(variant==0) h.assertTrue(plan.values().stream().filter(c->c.state().is(ModBlocks.SIMPLE_CONDENSATION_TABLE.get())).count()==1,"Missing fixed condensation table");
            if(variant==2) {
                h.assertTrue(plan.values().stream().filter(c->c.state().is(ModBlocks.ANALYSIS_ARTISAN_TABLE.get())).count()==1,"Missing fixed analysis table");
                h.assertTrue(plan.entrySet().stream().filter(e->"wizard_tower_materials".equals(e.getValue().loot())).allMatch(e->e.getKey().getY()==13),"Material chest not in research room");
                h.assertTrue(plan.entrySet().stream().filter(e->"wizard_tower_research".equals(e.getValue().loot())).allMatch(e->e.getKey().getY()==17),"Research ball not in lowered attic");
                int[][] ring={{7,5},{7,6},{7,7},{6,7},{5,7},{5,6},{5,5},{6,5}};
                for(int i=0;i<24;i++) {
                    int[] p=ring[i%8];int y=1+i/2;
                    h.assertTrue(plan.get(new BlockPos(p[0],y,p[1])).state().getBlock() instanceof SlabBlock,"Missing spiral tread "+i);
                    for(int dy=1;dy<=2;dy++) h.assertTrue(plan.get(new BlockPos(p[0],y+dy,p[1])).state().isAir(),"Spiral headroom obstructed "+i);
                }
                for(int y=13;y<=15;y++) h.assertTrue(plan.get(new BlockPos(6,y,9)).state().is(Blocks.LADDER),"Attic ladder missing");
                h.assertTrue(plan.get(new BlockPos(6,16,9)).state().getBlock() instanceof TrapDoorBlock,"Lowered attic hatch missing");
                for(int y=13;y<=15;y++)h.assertTrue(plan.get(new BlockPos(6,y,6)).state().isAir(),"Research room headroom obstructed");
                h.assertTrue(!plan.get(new BlockPos(6,16,6)).state().isAir(),"Empty course was not removed");
                h.assertTrue(plan.values().stream().noneMatch(c->c.state().is(Blocks.STONE)||c.state().is(Blocks.STONE_SLAB)||c.state().is(Blocks.STONE_STAIRS)),"Stone filler not converted");
                h.assertTrue(plan.values().stream().filter(c->c.loot()!=null).allMatch(c->c.state().is(ModBlocks.CRYSTAL_BALL.get())),"Tower loot still uses chests");
                assertRoofClosed(h,plan);
            }
            var cells=new com.google.gson.JsonArray();
            plan.forEach((p,c)->{if(!c.state().isAir()) {
                var cell=new com.google.gson.JsonObject();cell.addProperty("x",p.getX());cell.addProperty("y",p.getY());cell.addProperty("z",p.getZ());
                cell.addProperty("block",BuiltInRegistries.BLOCK.getKey(c.state().getBlock()).toString());
                var properties=new com.google.gson.JsonObject();
                c.state().getValues().forEach((property,value)->properties.addProperty(property.getName(),value.toString().toLowerCase(java.util.Locale.ROOT)));
                cell.add("properties",properties);
                if(c.loot()!=null) cell.addProperty("loot","omnira:chests/"+c.loot());
                if(c.data()!=null)cell.addProperty("block_entity",c.data().toString());
                else if(c.state().getBlock() instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
                    var be=entityBlock.newBlockEntity(p,c.state());
                    if(be!=null) cell.addProperty("block_entity",be.saveWithFullMetadata(h.getLevel().registryAccess()).toString());
                }
                cells.add(cell);
            }});preview.add(cells);
            exportNbt(h,variant,plan);
        }
        int tables=0,assembly=0,both=0;
        for(int seed=0;seed<200;seed++) {
            var plan=RuinsPiece.blueprint(2,seed);
            boolean p=plan.get(new BlockPos(7,17,3)).state().is(ModBlocks.CRYSTAL_PROCESSING_TABLE.get());
            boolean a=plan.get(new BlockPos(5,17,3)).state().is(ModBlocks.ARCANE_ASSEMBLY_TABLE.get());
            if(p)tables++;if(a)assembly++;if(p&&a)both++;
            h.assertTrue(plan.get(new BlockPos(6,13,3)).state().is(ModBlocks.ANALYSIS_ARTISAN_TABLE.get()),"Fixed analysis table became random");
        }
        h.assertTrue(tables>25&&tables<80&&assembly>25&&assembly<80&&both>3&&both<30,"Attic independent 25% rolls incorrect: "+tables+","+assembly+","+both);
        var output=java.nio.file.Path.of("../../build/reports/ruins/overworld_ruins_blueprints.json");
        java.nio.file.Files.createDirectories(output.toAbsolutePath().getParent());java.nio.file.Files.writeString(output,preview.toString());
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void worldgenAndPersistence(GameTestHelper h) {
        var level=h.getLevel();var generator=level.getChunkSource().getGenerator();
        for(String name:new String[]{"abandoned_apprentice_hut","ruined_crystal_workshop","abandoned_wizard_tower"}) {
            var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.fromNamespaceAndPath("omnira",name));
            h.assertTrue(structure instanceof RuinsStructure,"Structure data failed to load: "+name);
            var start=structure.generate(level.registryAccess(),generator,generator.getBiomeSource(),level.getChunkSource().randomState(),
                    level.getStructureManager(),level.getSeed(),new ChunkPos(h.absolutePos(BlockPos.ZERO)),0,level,b->true);
            h.assertTrue(start.isValid(),"Generation point rejected level: "+name);
        }
        var context=StructurePieceSerializationContext.fromLevel(level);
        for(var direction:Direction.Plane.HORIZONTAL) {
            var piece=new RuinsPiece(2,0,64,0,direction,42);
            var tag=piece.createTag(context);var loaded=new RuinsPiece(context,tag);
            h.assertTrue(tag.equals(loaded.createTag(context)),"Piece seed or orientation lost on save");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void clippedPlacement(GameTestHelper h) {
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(3,2,3));
        var piece=new RuinsPiece(2,origin.getX(),origin.getY(),origin.getZ(),Direction.SOUTH,42);
        var full=piece.getBoundingBox();
        var first=new BoundingBox(full.minX(),level.getMinBuildHeight(),full.minZ(),full.minX()+5,level.getMaxBuildHeight()-1,full.maxZ());
        var second=new BoundingBox(full.minX()+6,level.getMinBuildHeight(),full.minZ(),full.maxX(),level.getMaxBuildHeight()-1,full.maxZ());
        for(var clip:new BoundingBox[]{second,first}) piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),RandomSource.create(4),clip,new ChunkPos(origin),origin);
        int chests=0;
        for(var entry:RuinsPiece.blueprint(2,42).entrySet()) if(entry.getValue().loot()!=null) {
            var p=origin.offset(entry.getKey());
            h.assertTrue(level.getBlockEntity(p) instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity,"Missing ball after clipped generation");
            var data=level.getBlockEntity(p).saveWithoutMetadata(level.registryAccess());
            h.assertTrue(data.getString("LootTable").equals("omnira:chests/"+entry.getValue().loot()),"Chest loot table lost");chests++;
        }
        h.assertTrue(chests==3 && level.getBlockState(origin.offset(6,13,3)).is(ModBlocks.ANALYSIS_ARTISAN_TABLE.get()),"Fixed workstation lost");
        for(var entry:RuinsPiece.blueprint(2,42).entrySet())if(entry.getValue().state().getBlock() instanceof StairBlock) {
            var p=origin.offset(entry.getKey());var state=level.getBlockState(p);
            h.assertTrue(state.getValue(StairBlock.SHAPE)==Block.updateFromNeighbourShapes(state,level,p).getValue(StairBlock.SHAPE),"Exported corner differs from vanilla at "+entry.getKey());
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void authoredFurnitureAndVariants(GameTestHelper h) {
        int marks=0,engines=0,intact=0;
        for(int seed=0;seed<200;seed++) {
            var hut=RuinsPiece.blueprint(0,seed);var pedestal=hut.get(new BlockPos(8,1,12));
            h.assertTrue(pedestal.state().is(ModBlocks.CRYSTAL_PEDESTAL.get())&&pedestal.data().getList("Items",10).getCompound(0).getString("id").equals("omnira:guide_book"),"Lost authored book");
            h.assertTrue(hut.values().stream().filter(c->c.loot()!=null).allMatch(c->c.state().is(ModBlocks.CRYSTAL_BALL.get())),"Hut still uses chest");
            if(hut.values().stream().noneMatch(c->c.state().is(Blocks.COBWEB)))intact++;
            var workshop=RuinsPiece.blueprint(1,seed);
            long halves=workshop.values().stream().filter(c->c.state().is(ModBlocks.WAYMARK.get())).count();
            h.assertTrue(halves==0||halves==2,"Partial waymark generated");
            if(halves==2)marks++;
            if(workshop.values().stream().anyMatch(c->c.state().is(ModBlocks.MANA_ENGINE.get())))engines++;
            for(var c:workshop.values())if(c.data()!=null&&c.state().is(ModBlocks.WAYMARK.get()))h.assertTrue(!c.data().contains("Id"),"Authored UUID leaked into generation");
        }
        h.assertTrue(intact>65&&intact<135&&marks>25&&marks<80&&engines>25&&engines<80,"Variant rates incorrect: "+intact+","+marks+","+engines);
        var origin=h.absolutePos(new BlockPos(2,2,2));var piece=new RuinsPiece(0,origin.getX(),origin.getY(),origin.getZ(),Direction.SOUTH,42);
        piece.postProcess(h.getLevel(),h.getLevel().structureManager(),h.getLevel().getChunkSource().getGenerator(),RandomSource.create(42),piece.getBoundingBox(),new ChunkPos(origin),origin);
        var pedestal=(com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity)h.getLevel().getBlockEntity(origin.offset(8,1,12));
        h.assertTrue(pedestal.getItem(0).is(ModItems.GUIDE_BOOK.get())&&pedestal.getItem(0).getCount()==1,"Placed book missing");
        var ball=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)h.getLevel().getBlockEntity(origin.offset(9,1,12));
        h.assertTrue(ball.getLootTable()!=null&&ball.getLootTable().location().getPath().equals("chests/apprentice_hut"),"Placed ball lost loot metadata");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void shadowStairNeighborUpdates(GameTestHelper h) {
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(8,3,8));
        for(var half:net.minecraft.world.level.block.state.properties.Half.values())for(var direction:Direction.Plane.HORIZONTAL) {
            for(var side:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(p.relative(side),Blocks.AIR.defaultBlockState());
            var state=DreamContent.SHADOW_ROCK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING,direction).setValue(StairBlock.HALF,half);
            level.setBlockAndUpdate(p,state);
            level.setBlockAndUpdate(p.relative(direction),state.setValue(StairBlock.FACING,direction.getClockWise()));
            h.assertTrue(level.getBlockState(p).getValue(StairBlock.SHAPE)==net.minecraft.world.level.block.state.properties.StairsShape.OUTER_RIGHT,"Outer corner did not connect");
            level.setBlockAndUpdate(p.relative(direction),Blocks.AIR.defaultBlockState());
            h.assertTrue(level.getBlockState(p).getValue(StairBlock.SHAPE)==net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT,"Removed neighbor left stale corner");
            level.setBlockAndUpdate(p.relative(direction.getOpposite()),state.setValue(StairBlock.FACING,direction.getClockWise()));
            h.assertTrue(level.getBlockState(p).getValue(StairBlock.SHAPE)==net.minecraft.world.level.block.state.properties.StairsShape.INNER_RIGHT,"Inner corner did not connect");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void lootAndBuildingMaterials(GameTestHelper h) {
        var level=h.getLevel();var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,h.absoluteVec(net.minecraft.world.phys.Vec3.ZERO)).create(LootContextParamSets.CHEST);
        int ink=0,picks=0,infused=0,cores=0;
        var hut=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath("omnira","chests/apprentice_hut")));
        var workshop=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath("omnira","chests/crystal_workshop")));
        for(int i=0;i<2000;i++) {
            for(var stack:hut.getRandomItems(params)) {
                h.assertTrue(!stack.is(ModItems.ARCANE_DUST.get()),"Apprentice hut still contains arcane dust");
                if(stack.is(ModItems.SPELL_INK.get())) {ink++;h.assertTrue(stack.isDamaged() && stack.getDamageValue()<stack.getMaxDamage(),"Ink is full or exhausted");}
            }
            var drops=workshop.getRandomItems(params);
            h.assertTrue(drops.stream().filter(s->s.is(ModItems.SPIRITUAL_CRYSTAL.get())).mapToInt(s->s.getCount()).sum()>=8,"Workshop crystals missing");
            for(var stack:drops) {if(stack.is(ModItems.CRYSTAL_PICKAXE.get())) picks++;if(stack.is(ModItems.INFUSED_CRYSTAL_PICKAXE.get())) infused++;if(stack.is(ModItems.TEST_SPELL_CORE.get()))cores++;}
        }
        h.assertTrue(ink>180 && ink<420 && picks>100 && picks<300 && infused>10 && infused<90,"Rare loot weights incorrect");
        h.assertTrue(cores>10&&cores<85,"Core chance differs from expected 2%: "+cores);
        for(var block:new Block[]{DreamContent.SHADOW_ROCK_SLAB.get(),DreamContent.SHADOW_ROCK_STAIRS.get()}) {
            h.assertTrue(block.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE),"Missing pickaxe tag");
            String name=BuiltInRegistries.BLOCK.getKey(block).getPath();
            h.assertTrue(level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira",name+"_from_stonecutting")).isPresent(),"Missing stonecutting recipe");
        }
        h.succeed();
    }
    private static void assertRoofClosed(GameTestHelper h,java.util.Map<BlockPos,RuinsPiece.Cell> plan) {
        var seen=new java.util.HashSet<BlockPos>();var queue=new java.util.ArrayDeque<BlockPos>();queue.add(new BlockPos(6,18,6));
        while(!queue.isEmpty()) {
            var p=queue.remove();if(!seen.add(p))continue;
            h.assertTrue(p.getX()>0&&p.getX()<12&&p.getZ()>0&&p.getZ()<12&&p.getY()<24,"Roof opens to outside at "+p);
            for(var d:Direction.values()) {
                var next=p.relative(d);if(next.getY()<17)continue;
                var c=plan.get(next);if(c==null||c.state().isAir())queue.add(next);
            }
        }
    }
    private static net.minecraft.nbt.ListTag vector(int x,int y,int z) {
        var list=new net.minecraft.nbt.ListTag();
        for(int v:new int[]{x,y,z})list.add(net.minecraft.nbt.IntTag.valueOf(v));return list;
    }
    private static void exportNbt(GameTestHelper h,int variant,java.util.Map<BlockPos,RuinsPiece.Cell> plan) throws java.io.IOException {
        var root=new net.minecraft.nbt.CompoundTag();var palette=new net.minecraft.nbt.ListTag();var blocks=new net.minecraft.nbt.ListTag();
        var ids=new java.util.HashMap<net.minecraft.world.level.block.state.BlockState,Integer>();
        for(var entry:plan.entrySet()) {
            var p=entry.getKey();var c=entry.getValue();
            int id=ids.computeIfAbsent(c.state(),state->{palette.add(net.minecraft.nbt.NbtUtils.writeBlockState(state));return palette.size()-1;});
            var block=new net.minecraft.nbt.CompoundTag();block.put("pos",vector(p.getX(),p.getY(),p.getZ()));block.putInt("state",id);
            if(c.data()!=null)block.put("nbt",c.data().copy());
            else if(c.state().getBlock() instanceof EntityBlock factory) {
                var be=factory.newBlockEntity(p,c.state());
                if(be!=null)block.put("nbt",be.saveWithFullMetadata(h.getLevel().registryAccess()));
            }
            if(c.loot()!=null)block.getCompound("nbt").putString("LootTable","omnira:chests/"+c.loot());
            blocks.add(block);
        }
        root.putInt("DataVersion",net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        root.put("size",vector(RuinsPiece.width(variant),RuinsPiece.height(variant),RuinsPiece.depth(variant)));
        root.put("palette",palette);root.put("blocks",blocks);root.put("entities",new net.minecraft.nbt.ListTag());
        String name=new String[]{"abandoned_apprentice_hut","ruined_crystal_workshop","abandoned_wizard_tower"}[variant];
        var path=java.nio.file.Path.of("../../build/reports/ruins/"+name+".nbt");
        java.nio.file.Files.createDirectories(path.getParent());net.minecraft.nbt.NbtIo.writeCompressed(root,path);
    }
}
