package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.config.OmniraLootConfig;
import com.mcmagic.omnira.registry.ModItems;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_pure_mirror")
@PrefixGameTestTemplate(false)
public final class PureMirrorGameTests {
    private static Item rock(){return BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:mirror_rock"));}
    @GameTest(template="spell_arena") public static void reactionAndUnpacking(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,2,4));
        level.setBlockAndUpdate(pos,com.mcmagic.omnira.registry.ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var vessel=(RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);
        var recipe=(ReactionRecipe)level.getRecipeManager().byKey(ResourceLocation.parse("omnira:reaction/pure_mirror")).orElseThrow().value();
        h.assertTrue(recipe.support().isEmpty()&&recipe.fluid().getAmount()==1000,"Unexpected catalyst or fluid amount");
        vessel.items.setStackInSlot(0,new ItemStack(rock()));vessel.tank.setFluid(recipe.fluid().copy());
        h.assertTrue(vessel.start(recipe),"Pure mirror reaction rejected");
        h.assertTrue(vessel.items.getStackInSlot(0).isEmpty(),"Mirror rock not consumed at start");
        for(int i=0;i<2000;i++)RoughAlchemyCauldronBlockEntity.tick(level,pos,vessel.getBlockState(),vessel);
        h.assertTrue(vessel.items.getStackInSlot(0).is(ModItems.PURE_MIRROR.get())&&vessel.items.getStackInSlot(0).getCount()==1&&vessel.tank.isEmpty(),"Wrong reaction yield or consumption");
        var input=CraftingInput.of(1,1,List.of(new ItemStack(ModItems.PURE_MIRROR.get())));
        var result=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input,level.registryAccess());
        h.assertTrue(result.is(rock())&&result.getCount()==9,"Pure mirror must unpack to nine rocks");h.succeed();
    }
    @GameTest(template="spell_arena") public static void catalyticGrowthPreservesSubstrateAndSeed(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,2,4));
        h.assertTrue(CatalysisBlock.place(level,pos,Direction.NORTH),"Chamber placement failed");
        var be=CatalysisBlock.controller(level,pos);
        be.items.setStackInSlot(0,new ItemStack(ModItems.PURE_MIRROR.get()));be.items.setStackInSlot(1,new ItemStack(rock()));
        h.assertTrue(CatalysisGrowth.acceptsSubstrate(level,be.items.getStackInSlot(0)),"Non-block substrate rejected");
        h.assertTrue(CatalysisNaturalGrowth.resolve(level,be.items.getStackInSlot(0),be.items.getStackInSlot(1))==null,"Dry rock production invented");
        be.tank.setFluid(new FluidStack(Fluids.WATER,4000));
        for(int i=0;i<800;i++)be.tick();
        h.assertTrue(be.items.getStackInSlot(2).is(rock())&&be.items.getStackInSlot(2).getCount()==16,"Catalytic yield must be sixteen");
        h.assertTrue(be.items.getStackInSlot(0).getCount()==1&&be.items.getStackInSlot(1).getCount()==1&&be.tank.getFluidAmount()==3000,"Substrate/seed consumed or wrong stage advancement cost");h.succeed();
    }
    @GameTest(template="spell_arena") public static void migrationAndRewardBalance(GameTestHelper h)throws IOException {
        JsonObject old;
        try(var stream=Objects.requireNonNull(PureMirrorGameTests.class.getResourceAsStream("/loot_migrations/spatial_diversity_v1.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            old=JsonParser.parseReader(reader).getAsJsonObject();
        }
        var custom=old.deepCopy();String key="omnira:chests/void_lighthouse";
        custom.getAsJsonObject(key).addProperty("random_sequence","pack:custom");var saved=custom.get(key).deepCopy();
        var defaults=OmniraLootConfig.defaults().getAsJsonObject("treasureTables");
        for(var entry:old.entrySet()){
            var before=entry.getValue().getAsJsonObject().getAsJsonArray("pools");var after=defaults.getAsJsonObject(entry.getKey()).getAsJsonArray("pools");
            h.assertTrue(before.size()==after.size(),"Independent reward pools changed");
            for(int i=1;i<before.size();i++)h.assertTrue(before.get(i).equals(after.get(i)),"Relic probability changed");
            int delta=entry.getKey().contains("fold_observatory")?1:0;
            h.assertTrue(weight(after.get(0).getAsJsonObject())==weight(before.get(0).getAsJsonObject())+delta,"Unrelated pool weights changed");
            h.assertTrue(after.get(0).toString().contains("omnira:pure_mirror"),"Missing pure mirror reward");
            if(delta==1)h.assertTrue(after.get(0).toString().contains("omnira:stable_fold_gate"),"Observatory gate missing");
        }
        h.assertTrue(OmniraLootConfig.migrateDefaultSpatialDiversity(old)&&!OmniraLootConfig.migrateDefaultSpatialDiversity(old),"Migration missing or repeated");
        for(var entry:old.entrySet())h.assertTrue(entry.getValue().equals(defaults.get(entry.getKey())),"Wrong migrated table");
        OmniraLootConfig.migrateDefaultSpatialDiversity(custom);h.assertTrue(custom.get(key).equals(saved),"Custom loot overwritten");h.succeed();
    }
    private static int weight(JsonObject pool){int total=0;for(var raw:pool.getAsJsonArray("entries")){var e=raw.getAsJsonObject();total+=e.has("weight")?e.get("weight").getAsInt():1;}return total;}
    private PureMirrorGameTests(){}
}
