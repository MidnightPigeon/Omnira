package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.config.OmniraLootConfig;
import com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.Set;

@GameTestHolder("omnira_loot_config")
@PrefixGameTestTemplate(false)
public final class LootConfigGameTests {
    @GameTest(template="spell_arena") public static void independentFluidListsAndTags(GameTestHelper h){
        var waterTag=Set.of("#minecraft:water");
        var both=new OmniraLootConfig.FluidRules(true,true,waterTag,Set.of("minecraft:water"));
        h.assertTrue(!both.allows(Fluids.WATER)&&!both.allows(Fluids.LAVA),"Blacklist must override a whitelist tag");
        var dilute=com.mcmagic.omnira.alchemy.AlchemyMaterials.DILUTE.source.get();
        h.assertTrue(both.allows(dilute),"Water tag did not include registered dilute spacetime");
        var tagDenied=new OmniraLootConfig.FluidRules(true,true,Set.of("minecraft:water"),waterTag);
        h.assertTrue(!tagDenied.allows(Fluids.WATER),"Blacklist tag did not override explicit whitelist ID");
        h.assertTrue(new OmniraLootConfig.FluidRules(true,false,waterTag,waterTag).allows(Fluids.WATER),"Disabled blacklist still filters");
        var disabled=new OmniraLootConfig.FluidRules(false,false,Set.of(),waterTag);
        h.assertTrue(disabled.allows(Fluids.LAVA)&&disabled.allows(Fluids.WATER)&&!disabled.allows(Fluids.FLOWING_WATER)&&!disabled.allows(Fluids.EMPTY),"Disabled lists changed source-only restriction");
        var defaults=OmniraLootConfig.defaults();var rules=OmniraLootConfig.rules(defaults);
        h.assertTrue(rules.whitelistEnabled()&&rules.blacklistEnabled()&&!rules.allows(dilute)
                &&!rules.allows(com.mcmagic.omnira.alchemy.AlchemyMaterials.REFINED.source.get()),"New defaults mishandled spacetime fluids");
        var f=defaults.getAsJsonObject("fluidTreasure");f.remove("alchemyRevision");f.remove("whitelistEnabled");f.remove("blacklistEnabled");f.addProperty("mode","BLACKLIST");
        h.assertTrue(OmniraLootConfig.migrateFluidSwitches(defaults)&&!OmniraLootConfig.rules(defaults).whitelistEnabled()&&OmniraLootConfig.rules(defaults).blacklistEnabled(),"Old blacklist mode migration changed semantics");
        f.add("whitelist",new JsonArray());
        h.assertTrue(OmniraLootConfig.migrateAlchemyFluids(defaults)&&f.getAsJsonArray("whitelist").isEmpty(),"Migration must not add fluids");
        f.remove("fixedWhitelistRevision");f.getAsJsonArray("whitelist").add("omnira:dilute_spacetime");
        f.getAsJsonArray("whitelist").add("custom:fluid");
        h.assertTrue(OmniraLootConfig.migrateFixedFluidWhitelist(defaults)
                &&!f.getAsJsonArray("whitelist").contains(new JsonPrimitive("omnira:dilute_spacetime"))
                &&f.getAsJsonArray("whitelist").contains(new JsonPrimitive("custom:fluid")),"Fixed whitelist migration changed custom entries");
        f.getAsJsonArray("whitelist").add("omnira:dilute_spacetime");
        h.assertTrue(!OmniraLootConfig.migrateFixedFluidWhitelist(defaults)&&OmniraLootConfig.rules(defaults).allows(dilute),"Later manual fluid edits overwritten");
        f.add("whitelist",new JsonArray());h.assertTrue(!OmniraLootConfig.migrateAlchemyFluids(defaults)&&f.getAsJsonArray("whitelist").isEmpty(),"Later user edits overwritten");
        f.getAsJsonArray("whitelist").add("#invalid");boolean rejected=false;try{OmniraLootConfig.rules(defaults);}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Malformed tag accepted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void potionTreasureComponents(GameTestHelper h){
        var fluid=net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.parse("create:potion"));
        for(int i=0;i<32;i++){
            var stack=com.mcmagic.omnira.compat.TreasurePotionFluid.prepare(new net.neoforged.neoforge.fluids.FluidStack(fluid,24000),h.getLevel().random);
            var tank=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(24000);tank.setFluid(stack);
            var saved=tank.writeToNBT(h.getLevel().registryAccess(),new net.minecraft.nbt.CompoundTag());
            var copy=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(24000);copy.readFromNBT(h.getLevel().registryAccess(),saved);
            var bottle=com.simibubi.create.content.fluids.potion.PotionFluidHandler.fillBottle(new net.minecraft.world.item.ItemStack(Items.GLASS_BOTTLE),copy.getFluid());
            var contents=bottle.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
            h.assertTrue(contents!=null&&contents.hasEffects(),"Potion loot lost its effect on storage or extraction");
            h.assertTrue(copy.getFluidAmount()==24000,"Potion amount changed while serialized");
        }h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void configurationAndTreasure(GameTestHelper h)throws Exception{
        var white=new OmniraLootConfig.FluidRules(true,false,Set.of("minecraft:water","missing:fluid","minecraft:flowing_lava"),Set.of("minecraft:water"));
        h.assertTrue(white.allows(Fluids.WATER)&&!white.allows(Fluids.LAVA)&&!white.allows(Fluids.FLOWING_LAVA)&&!white.allows(Fluids.EMPTY),"Whitelist source filtering");
        var black=new OmniraLootConfig.FluidRules(false,true,Set.of(),Set.of("minecraft:lava"));
        h.assertTrue(black.allows(Fluids.WATER)&&!black.allows(Fluids.LAVA)&&!black.allows(Fluids.FLOWING_WATER),"Blacklist filtering");
        var path=OmniraLootConfig.path();var original=Files.readString(path);
        var level=h.getLevel();var key=ResourceLocation.parse("omnira:chests/kirisame_attic");
        var root=JsonParser.parseString(original).getAsJsonObject();
        h.assertTrue(OmniraLootConfig.rules(root).whitelistEnabled(),"Default whitelist switch");
        var candidates=LiquidCrystalBallBlockEntity.treasureFluids();
        var candidateIds=candidates.stream().map(f->net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(f).toString()).toList();
        var registered=Set.copyOf(OmniraLootConfig.sourceFluidIds());
        var expected=net.minecraft.core.registries.BuiltInRegistries.FLUID.stream().filter(OmniraLootConfig.rules(root)::allows)
                .map(f->net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(f).toString()).collect(java.util.stream.Collectors.toSet());
        h.assertTrue(!candidateIds.isEmpty()&&candidateIds.size()==Set.copyOf(candidateIds).size()
                &&Set.copyOf(candidateIds).equals(expected),"Actual treasure candidates differ from available whitelist sources: "+candidateIds);
        h.assertTrue(candidateIds.stream().filter("aeronautics:levitite_blend"::equals).count()==(expected.contains("aeronautics:levitite_blend")?1:0),"Levitite Blend candidate count differs from installed whitelist");
        com.mcmagic.omnira.Omnira.LOGGER.info("Verified loot fluid IDs: {}",OmniraLootConfig.sourceFluidIds());
        try{
            root.getAsJsonObject("fluidTreasure").add("whitelist",new JsonArray());
            root.getAsJsonObject("treasureTables").add(key.toString(),JsonParser.parseString("{\"type\":\"minecraft:chest\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:diamond\"}]}]}"));
            Files.writeString(path,root.toString());
            var event=new LootTableLoadEvent(level.registryAccess(),key,LootTable.EMPTY);OmniraLootConfig.lootTable(event);
            var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,h.absolutePos(new BlockPos(3,3,3)).getCenter()).create(LootContextParamSets.CHEST);
            var drops=event.getTable().getRandomItems(params);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(Items.DIAMOND),"Config table replacement not used");
            h.assertTrue(LiquidCrystalBallBlockEntity.treasureFluids().isEmpty(),"Empty whitelist must stay empty");
            var pos=h.absolutePos(new BlockPos(4,3,4));level.setBlockAndUpdate(pos,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
            var ball=(LiquidCrystalBallBlockEntity)level.getBlockEntity(pos);var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("FluidTreasure",true);ball.loadWithComponents(tag,level.registryAccess());
            var player=h.makeMockPlayer(GameType.SURVIVAL);ball.unpackLootTable(player);
            h.assertTrue(ball.hasPendingLoot()&&ball.tank.isEmpty(),"Empty choices must preserve unopened treasure");
            root.getAsJsonObject("fluidTreasure").getAsJsonArray("whitelist").add("minecraft:lava");
            Files.writeString(path,root.toString());OmniraLootConfig.lootTable(new LootTableLoadEvent(level.registryAccess(),key,LootTable.EMPTY));
            ball.unpackLootTable(player);
            h.assertTrue(!ball.hasPendingLoot()&&ball.tank.getFluid().getFluid()==Fluids.LAVA&&ball.tank.getFluidAmount()==ball.tank.getCapacity(),"Configured liquid not used");
            root.getAsJsonObject("fluidTreasure").addProperty("whitelistEnabled",false);
            root.getAsJsonObject("fluidTreasure").addProperty("blacklistEnabled",true);
            root.getAsJsonObject("fluidTreasure").getAsJsonArray("blacklist").add("minecraft:lava");
            Files.writeString(path,root.toString());OmniraLootConfig.lootTable(new LootTableLoadEvent(level.registryAccess(),key,LootTable.EMPTY));
            h.assertTrue(!LiquidCrystalBallBlockEntity.treasureFluids().contains(Fluids.LAVA)&&LiquidCrystalBallBlockEntity.treasureFluids().contains(Fluids.WATER),"Mode switch failed");
            ball.unpackLootTable(player);h.assertTrue(ball.tank.getFluid().getFluid()==Fluids.LAVA,"Opened treasure rerolled after config change");
        }finally{
            Files.writeString(path,original);OmniraLootConfig.lootTable(new LootTableLoadEvent(level.registryAccess(),key,LootTable.EMPTY));
        }
        h.succeed();
    }
}
