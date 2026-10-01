package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.DreamContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_radiant_courtyard")
@PrefixGameTestTemplate(false)
public final class RadiantCourtyardGameTests {
    @GameTest(template="spell_arena",timeoutTicks=1200) public static void naturalCourtyardAndPersistence(GameTestHelper h) throws Exception {
        var dream=h.getLevel();
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","radiant_courtyard");
        var structure=dream.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(id);
        h.assertTrue(structure instanceof com.mcmagic.omnira.world.structure.RadiantCourtyardStructure,"Worldgen codec missing");
        h.assertTrue(structure.biomes().stream().allMatch(b->b.is(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","dawn_crystal_fields")))),"Wrong courtyard biome");
        // GameTest's flat preset does not load custom dimensions; decode their real generator.
        net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator gen;
        try(var reader=new java.io.InputStreamReader(RadiantCourtyardGameTests.class.getResourceAsStream("/data/omnira/dimension/dream_realm.json"),java.nio.charset.StandardCharsets.UTF_8)) {
            var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().get("generator");
            gen=(net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator)net.minecraft.world.level.chunk.ChunkGenerator.CODEC.parse(
                    net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,dream.registryAccess()),json).getOrThrow();
        }
        var randomState=net.minecraft.world.level.levelgen.RandomState.create(gen.generatorSettings().value(),
                dream.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.NOISE).asLookup(),dream.getSeed());
        var height=net.minecraft.world.level.LevelHeightAccessor.create(0,256);int found=0;
        for(int i=0;i<32 && found<2;i++) {
            var start=structure.generate(dream.registryAccess(),gen,gen.getBiomeSource(),randomState,
                    dream.getStructureManager(),dream.getSeed(),new net.minecraft.world.level.ChunkPos(i*32-512,i*17-256),0,height,structure.biomes()::contains);
            if(!start.isValid())continue;
            found++;var box=start.getBoundingBox();
            h.assertTrue(box.minY()+6>=174 && box.minY()+6<=218,"Courtyard outside light-island band");
            var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(dream);
            var piece=(com.mcmagic.omnira.world.structure.RadiantCourtyardPiece)start.getPieces().getFirst();
            var tag=piece.createTag(context);
            h.assertTrue(tag.equals(new com.mcmagic.omnira.world.structure.RadiantCourtyardPiece(context,tag).createTag(context)),"Template position/rotation lost");
        }
        h.assertTrue(found>0,"No viable courtyard candidates in real dream terrain");h.succeed();
    }
    @GameTest(template="spell_arena") public static void courtyardClippedTemplatePlacement(GameTestHelper h) {
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(0,2,0));
        var piece=new com.mcmagic.omnira.world.structure.RadiantCourtyardPiece(level.getStructureManager(),origin,Rotation.NONE);
        var box=piece.getBoundingBox();
        for(var clip:new net.minecraft.world.level.levelgen.structure.BoundingBox[]{
                new net.minecraft.world.level.levelgen.structure.BoundingBox(box.minX()+8,box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ()),
                new net.minecraft.world.level.levelgen.structure.BoundingBox(box.minX(),box.minY(),box.minZ(),box.minX()+7,box.maxY(),box.maxZ())})
            piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(2),clip,new net.minecraft.world.level.ChunkPos(origin),origin);
        var ball=level.getBlockEntity(origin.offset(8,5,8));
        h.assertTrue(ball instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity,"Generated ball lost");
        h.assertTrue(ball.saveWithoutMetadata(level.registryAccess()).getString("LootTable").equals("omnira:chests/radiant_courtyard"),"Pending courtyard loot lost");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200) public static void courtyardLootAndAuthoredContents(GameTestHelper h) {
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","chests/radiant_courtyard"));
        var table=h.getLevel().getServer().reloadableRegistries().getLootTable(key);
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        var counts=new java.util.HashMap<String,Integer>();
        for(int seed=1;seed<=5000;seed++) {
            var loot=table.getRandomItems(params,seed);h.assertTrue(loot.size()>=3 && loot.size()<=7,"Ball can be empty or overflow");
            for(var stack:loot)counts.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(),1,Integer::sum);
        }
        int light=counts.getOrDefault("light_microcore",0),dark=counts.getOrDefault("dark_microcore",0);
        h.assertTrue(light>150 && dark>150 && light+dark>400 && light+dark<600,"Combined microcore chance differs from 10 percent");
        for(String rare:new String[]{"arcane_crystal_grid","dream_spell_core"})h.assertTrue(counts.getOrDefault(rare,0)>50 && counts.get(rare)<150,"Rare loot chance differs from 2 percent");
        for(String normal:new String[]{"spiritual_crystal","light_crystal_torch","crude_light_core","light_crystal_core"})h.assertTrue(counts.containsKey(normal),"Missing common reward "+normal);
        var template=h.getLevel().getStructureManager().getOrCreate(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","authored/radiant_courtyard"));
        var tag=template.save(new net.minecraft.nbt.CompoundTag());int balls=0;boolean core=false,workstation=false;
        for(var v:tag.getList("blocks",10)) {
            var nbt=((net.minecraft.nbt.CompoundTag)v).getCompound("nbt");
            if(nbt.getString("id").equals("omnira:crystal_ball")) {
                balls++;h.assertTrue(nbt.getString("LootTable").equals(key.location().toString()) && !nbt.contains("Items"),"Authored ball missing pending loot");
            }
            core|=nbt.getString("id").equals("omnira:spell_core");
            workstation|=nbt.getString("id").equals("omnira:advanced_condensation_table");
        }
        h.assertTrue(balls==1 && core && workstation,"Authored furniture lost on export");h.succeed();
    }
    @GameTest(template="spell_arena") public static void wrenchCyclesAndKeepsManualDirection(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        var block=DreamContent.CRYSTAL_COLUMN_BASE.get();level.setBlockAndUpdate(pos,block.defaultBlockState());
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"column-wrench"));
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0,0,2));
        var wrench=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","wrench")));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wrench);
        var seen=java.util.EnumSet.noneOf(net.minecraft.core.Direction.class);
        for(int i=0;i<6;i++) {
            var state=level.getBlockState(pos);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
            state.useItemOn(wrench,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            var changed=level.getBlockState(pos);
            seen.add(com.mcmagic.omnira.block.CrystalColumnBlock.tipDirection(changed));
            h.assertTrue(changed.getValue(com.mcmagic.omnira.block.CrystalColumnBlock.MANUAL),"Manual orientation not retained");
        }
        h.assertTrue(seen.size()==6 && wrench.getCount()==1,"Wrench must reach all directions without consumption");
        var state=level.getBlockState(pos);
        level.setBlockAndUpdate(pos.east(),DreamContent.CRYSTAL_COLUMN.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,net.minecraft.core.Direction.Axis.X));
        h.assertTrue(level.getBlockState(pos)==state,"Automatic alignment overrode wrench choice");h.succeed();
    }
    @GameTest(template="spell_arena") public static void columnEndsJoinAllSixDirections(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        var end=DreamContent.CRYSTAL_COLUMN_BASE.get();
        for(var direction:net.minecraft.core.Direction.values()) {
            for(var d:net.minecraft.core.Direction.values())level.setBlockAndUpdate(pos.relative(d),Blocks.AIR.defaultBlockState());
            var context=new net.minecraft.world.item.context.DirectionalPlaceContext(level,pos,net.minecraft.core.Direction.NORTH,new ItemStack(end),direction);
            var free=end.getStateForPlacement(context);
            h.assertTrue(com.mcmagic.omnira.block.CrystalColumnBlock.tipDirection(free)==direction,"Free placement direction wrong");
            level.setBlockAndUpdate(pos.relative(direction),DreamContent.CRYSTAL_COLUMN.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,direction.getAxis()));
            var joined=end.getStateForPlacement(context);
            h.assertTrue(com.mcmagic.omnira.block.CrystalColumnBlock.tipDirection(joined)==direction,"End did not face shaft");
            h.assertTrue(DreamContent.CRYSTAL_COLUMN_CAPITAL.get().getStateForPlacement(context).is(end),"Legacy capital was not merged on placement");
            var rotated=joined.rotate(Rotation.CLOCKWISE_90);
            h.assertTrue(com.mcmagic.omnira.block.CrystalColumnBlock.tipDirection(rotated)==Rotation.CLOCKWISE_90.rotate(direction),"Rotation lost end direction");
            level.setBlockAndUpdate(pos,end.defaultBlockState());
            var updated=end.defaultBlockState().updateShape(direction,level.getBlockState(pos.relative(direction)),level,pos,pos.relative(direction));
            h.assertTrue(com.mcmagic.omnira.block.CrystalColumnBlock.tipDirection(updated)==direction,"Late shaft did not orient end");
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void crystalMasonryShapesAndLight(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(5,2,5));
        for(var holder:java.util.List.of(DreamContent.CRYSTAL_COLUMN,DreamContent.CRYSTAL_COLUMN_BASE,DreamContent.CRYSTAL_COLUMN_CAPITAL)) {
            for(var axis:net.minecraft.core.Direction.Axis.values()) {
                var state=holder.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis);
                var shape=state.getShape(h.getLevel(),p);
                h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.block().equals(shape),"Column must not be a full cube");
                h.assertTrue(shape.min(axis)==0 && shape.max(axis)==1,"Column longitudinal shape must span one block");
                h.assertTrue(state.getLightEmission(h.getLevel(),p)==10,"Crystal column lost material light");
            }
        }
        for(var holder:java.util.List.of(DreamContent.LIGHT_CONDENSATE,DreamContent.LIGHT_CONDENSATE_BRICKS))
            h.assertTrue(holder.get().defaultBlockState().getLightEmission(h.getLevel(),p)==7,"Light condensate must emit level 7");
        for(var holder:java.util.List.of(DreamContent.LIGHT_SOURCE_CRYSTAL,DreamContent.LIGHT_SOURCE_BRICKS,DreamContent.SPIRITUAL_CRYSTAL_BLOCK))
            h.assertTrue(holder.get().defaultBlockState().getLightEmission(h.getLevel(),p)==10,"Light source crystal must emit level 10");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void torchHalvesLightAndSupport(GameTestHelper h) {
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,4));
        var block=DreamContent.LIGHT_CRYSTAL_TORCH.get();
        level.setBlockAndUpdate(p.below(),DreamContent.LIGHT_CONDENSATE_BRICKS.get().defaultBlockState());
        var state=block.defaultBlockState();
        h.assertTrue(state.canSurvive(level,p),"Torch cannot stand on crystal bricks");
        level.setBlockAndUpdate(p,state);block.setPlacedBy(level,p,state,null,new ItemStack(block));
        h.assertTrue(level.getBlockState(p.above()).is(block),"Missing upper torch");
        h.assertTrue(level.getBlockState(p.above()).getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.UPPER,"Wrong half");
        h.assertTrue(state.getLightEmission(level,p)==15,"Torch light must be 15");
        h.assertTrue(Math.abs(level.getBlockState(p.above()).getShape(level,p.above()).max(net.minecraft.core.Direction.Axis.Y)-.6)<.001,"Torch height must be 1.6");
        level.destroyBlock(p.above(),false);
        h.assertTrue(level.getBlockState(p).isAir(),"Upper destruction leaves lower torch");
        level.setBlockAndUpdate(p,state);block.setPlacedBy(level,p,state,null,new ItemStack(block));
        level.destroyBlock(p.below(),false);
        h.assertTrue(level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir(),"Support removal leaves torch");
        h.succeed();
    }
}
