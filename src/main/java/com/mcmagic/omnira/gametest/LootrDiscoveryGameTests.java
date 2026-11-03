package com.mcmagic.omnira.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.neoforge.gametest.*;

/** Exercise Lootr's real discovery hooks: never enqueue containers directly. */
@GameTestHolder("omnira_lootr_discovery")
@PrefixGameTestTemplate(false)
public final class LootrDiscoveryGameTests {
    @GameTest(template="spell_arena",timeoutTicks=400) public static void vanillaStructureDiscoversChestBesideOrbs(GameTestHelper h) {
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(1,2,1));
        var template=level.getStructureManager().getOrCreate(ResourceLocation.parse("minecraft:igloo/bottom"));
        var settings=new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setIgnoreEntities(true);
        h.assertTrue(template.placeInWorld(level,origin,origin,settings,level.random,2),"Igloo template not placed");
        var chests=template.filterBlocks(origin,settings,Blocks.CHEST);
        h.assertTrue(!chests.isEmpty(),"Igloo chest fixture missing");
        // IglooPieces assigns this table from its data marker, not from template NBT.
        for(var chest:chests)((RandomizableContainerBlockEntity)level.getBlockEntity(chest.pos()))
                .setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.IGLOO_CHEST,42);
        // Both kinds of unopened Omnira treasure share the loaded area with vanilla containers.
        var item=origin.offset(12,0,0);var liquid=origin.offset(13,0,0);
        level.setBlockAndUpdate(item,com.mcmagic.omnira.registry.ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        ((RandomizableContainerBlockEntity)level.getBlockEntity(item)).setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.parse("omnira:chests/apprentice_hut")),42);
        level.setBlockAndUpdate(liquid,com.mcmagic.omnira.registry.ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        ((com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)level.getBlockEntity(liquid)).markFluidTreasure();
        h.succeedWhen(()->{
            for(var chest:chests)h.assertTrue(com.mcmagic.omnira.compat.LootrOrbs.blocksAutomation(level.getBlockEntity(chest.pos())),
                    "Natural igloo chest did not convert at "+chest.pos());
        });
    }

    @GameTest(template="spell_arena",timeoutTicks=400) public static void savedContainerNbtConvertsNaturally(GameTestHelper h) {
        var level=h.getLevel();var positions=new java.util.ArrayList<BlockPos>();
        for(int i=0;i<3;i++){
            var pos=h.absolutePos(new BlockPos(2+i*3,2,3));positions.add(pos);
            level.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
            level.removeBlockEntity(pos);
            var tag=new net.minecraft.nbt.CompoundTag();
            tag.putString("id","minecraft:chest");tag.putInt("x",pos.getX());tag.putInt("y",pos.getY());tag.putInt("z",pos.getZ());
            tag.putString("LootTable",java.util.List.of("minecraft:chests/simple_dungeon",
                    "lootr_test:chests/foreign_structure","omnira:chests/apprentice_hut").get(i));
            tag.putLong("LootTableSeed",42);
            level.getChunkAt(pos).setBlockEntityNbt(tag);
            h.assertTrue(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity,"Pending NBT not loaded");
        }
        h.succeedWhen(()->{
            for(var pos:positions)h.assertTrue(com.mcmagic.omnira.compat.LootrOrbs.blocksAutomation(level.getBlockEntity(pos)),
                    "NBT-loaded chest did not convert at "+pos);
        });
    }

    @GameTest(template="spell_arena",timeoutTicks=400) public static void newlyAssignedTablesConvertNaturally(GameTestHelper h) {
        h.assertTrue(net.neoforged.fml.ModList.get().isLoaded("lootr"),"This test requires Lootr");
        var blocks=java.util.List.of(Blocks.CHEST,Blocks.TRAPPED_CHEST,Blocks.BARREL,Blocks.SHULKER_BOX);
        var ids=java.util.List.of("minecraft:chests/simple_dungeon","lootr_test:chests/foreign_structure","omnira:chests/apprentice_hut");
        var positions=new java.util.ArrayList<BlockPos>();
        for(int i=0;i<blocks.size();i++)for(int j=0;j<ids.size();j++){
            var pos=h.absolutePos(new BlockPos(2+i*3,2,2+j*3));positions.add(pos);
            h.getLevel().setBlockAndUpdate(pos,blocks.get(i).defaultBlockState());
            var be=(RandomizableContainerBlockEntity)h.getLevel().getBlockEntity(pos);
            be.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse(ids.get(j))),42);
        }
        h.succeedWhen(()->{
            for(var pos:positions)h.assertTrue(com.mcmagic.omnira.compat.LootrOrbs.blocksAutomation(h.getLevel().getBlockEntity(pos)),
                    "Natural conversion missed "+pos+": "+h.getLevel().getBlockState(pos));
        });
    }
}
