package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.config.OmniraLootConfig;
import com.mojang.serialization.JsonOps;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spatial_fishing")
@PrefixGameTestTemplate(false)
public final class SpatialFishingGameTests {
    private static final String FISH="omnira:fishing/starflow_fish";
    private static JsonObject read(String path)throws IOException {
        try(var stream=Objects.requireNonNull(SpatialFishingGameTests.class.getResourceAsStream(path));
                var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    @GameTest(template="spell_arena")
    public static void migrationOnlyReplacesUneditedDefaults(GameTestHelper h)throws IOException {
        var old=read("/loot_migrations/spatial_fishing_v1.json");
        var custom=old.deepCopy();custom.getAsJsonObject(FISH).addProperty("random_sequence","pack:custom");
        var saved=custom.deepCopy();
        h.assertTrue(OmniraLootConfig.migrateDefaultSpatialFishing(old),"Old fishing default not migrated");
        h.assertTrue(old.get(FISH).equals(OmniraLootConfig.defaults().getAsJsonObject("treasureTables").get(FISH)),"Incorrect migration");
        h.assertTrue(!OmniraLootConfig.migrateDefaultSpatialFishing(old),"Migration repeated");
        h.assertTrue(!OmniraLootConfig.migrateDefaultSpatialFishing(custom)&&custom.equals(saved),"Custom fishing overwritten");
        h.assertTrue(!OmniraLootConfig.migrateDefaultSpatialFishing(new JsonObject()),"Missing fishing table fabricated");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void nativeTablesMatchConfigAndSpaceMaterialsAreCatchable(GameTestHelper h)throws IOException {
        var tables=OmniraLootConfig.defaults().getAsJsonObject("treasureTables");
        var ops=RegistryOps.create(JsonOps.INSTANCE,h.getLevel().registryAccess());
        var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,h.absoluteVec(Vec3.ZERO))
                .withParameter(LootContextParams.TOOL,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FISHING_ROD))
                .create(LootContextParamSets.FISHING);
        for(var entry:tables.entrySet()) {
            h.assertTrue(read("/data/omnira/loot_table/"+entry.getKey().substring("omnira:".length())+".json").equals(entry.getValue()),"Native/config mismatch: "+entry.getKey());
            LootTable.DIRECT_CODEC.parse(ops,entry.getValue()).getOrThrow();
        }
        var fish=LootTable.DIRECT_CODEC.parse(ops,tables.get(FISH)).getOrThrow();
        var caught=new HashSet<String>();
        for(int seed=1;seed<=512;seed++)for(var stack:fish.getRandomItems(params,seed))caught.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        h.assertTrue(caught.equals(Set.of("omnira:star_jellyfish","omnira:infused_spiritual_crystal","omnira:dream_crystal_shard",
                "omnira:spatial_crystal_shard","omnira:resonance_crystal")),"Missing or unexpected spatial catch: "+caught);
        var categories=tables.getAsJsonObject("omnira:fishing/void_starflow").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        int[] weights={85,10,5},quality={-1,-2,2};
        for(int i=0;i<3;i++)h.assertTrue(categories.get(i).getAsJsonObject().get("weight").getAsInt()==weights[i]
                &&categories.get(i).getAsJsonObject().get("quality").getAsInt()==quality[i],"Category or luck balance changed");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void castFromAboveBothPools(GameTestHelper h){
        var level=h.getLevel();
        var center=h.absolutePos(new net.minecraft.core.BlockPos(7,5,7));
        var owner=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"pond-angler"));
        var rod=com.mcmagic.omnira.space.sea.StarlightContent.ROD.get().getDefaultInstance();
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,rod);
        for(var liquid:List.of(com.mcmagic.omnira.mire.MireContent.LIQUID.get(),com.mcmagic.omnira.space.sea.VoidSeaContent.LIQUID.get())){
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=0;y++)
                level.setBlockAndUpdate(center.offset(x,y,z),liquid.defaultBlockState());
            owner.setPos(center.getCenter().add(2,24,2));
            rod.use(level,owner,net.minecraft.world.InteractionHand.MAIN_HAND);
            var hook=owner.fishing;
            hook.setPos(center.getCenter().add(0,1.3,0));
            hook.setDeltaMovement(0,-.22,0);
            hook.getRandom().setSeed(42L);
            var stages=new HashSet<Integer>();
            for(int i=0;i<1800 && !hook.isRemoved() && !stages.contains(3);i++){
                level.tickNonPassenger(hook);
                stages.add(((com.mcmagic.omnira.space.sea.StarlightFloat)hook).omnira$fishingPhase());
            }
            h.assertTrue(!hook.isRemoved()&&stages.containsAll(List.of(1,2,3))&&hook.position().distanceToSqr(center.getCenter())<4,"Float did not remain in pool through a natural bite: "+stages);
            owner.setPos(center.getCenter().add(0,extendedRange()?324:36,0));
            level.tickNonPassenger(hook);
            h.assertTrue(hook.isRemoved(),"Overextended line did not retract");
        }
        h.succeed();
    }
    private static boolean extendedRange(){return net.neoforged.fml.ModList.get().isLoaded("omnira_fishing_distance_fixture");}
    @GameTest(template="spell_arena") public static void lineRangeAndOwnerValidity(GameTestHelper h)throws Exception{
        var level=h.getLevel();
        var owner=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"range-angler"));
        var stop=net.minecraft.world.entity.projectile.FishingHook.class.getDeclaredMethod("shouldStopFishing",net.minecraft.world.entity.player.Player.class);stop.setAccessible(true);
        var origin=h.absoluteVec(new Vec3(5,5,5));owner.setPos(origin);
        for(boolean star:new boolean[]{false,true})for(var hand:net.minecraft.world.InteractionHand.values()){
            owner.setItemInHand(hand,star?com.mcmagic.omnira.space.sea.StarlightContent.ROD.get().getDefaultInstance():net.minecraft.world.item.Items.FISHING_ROD.getDefaultInstance());
            owner.setItemInHand(hand==net.minecraft.world.InteractionHand.MAIN_HAND?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            double limit=extendedRange()?320:32;
            for(double distance:new double[]{32,32.01,64,64.01,320,320.01}){
                var hook=new net.minecraft.world.entity.projectile.FishingHook(net.minecraft.world.entity.EntityType.FISHING_BOBBER,level);
                if(star)((com.mcmagic.omnira.space.sea.StarlightFloat)hook).omnira$markStarlight();
                hook.setPos(origin.add(0,distance,0));
                h.assertTrue((boolean)stop.invoke(hook,owner)==(distance>limit),"Incorrect range: star="+star+" hand="+hand+" distance="+distance+" expected limit="+limit);
            }
        }
        var hook=new net.minecraft.world.entity.projectile.FishingHook(net.minecraft.world.entity.EntityType.FISHING_BOBBER,level);
        ((com.mcmagic.omnira.space.sea.StarlightFloat)hook).omnira$markStarlight();hook.setPos(origin);
        for(var hand:net.minecraft.world.InteractionHand.values())owner.setItemInHand(hand,net.minecraft.world.item.ItemStack.EMPTY);
        h.assertTrue((boolean)stop.invoke(hook,owner),"Empty hands retained star float");
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,com.mcmagic.omnira.space.sea.StarlightContent.ROD.get().getDefaultInstance());
        owner.setHealth(0);
        h.assertTrue((boolean)stop.invoke(hook,owner),"Dead owner retained star float");h.succeed();
    }
    private SpatialFishingGameTests(){}
    @GameTest(template="spell_arena") public static void specialFluidsUseVanillaOpenWater(GameTestHelper h)throws Exception{
        var level=h.getLevel();var center=h.absolutePos(new net.minecraft.core.BlockPos(7,5,7));
        var hook=new net.minecraft.world.entity.projectile.FishingHook(net.minecraft.world.entity.EntityType.FISHING_BOBBER,level);
        var area=hook.getClass().getDeclaredMethod("calculateOpenWater",net.minecraft.core.BlockPos.class);
        area.setAccessible(true);
        JsonArray vanilla;
        var path=net.minecraft.resources.ResourceLocation.withDefaultNamespace("loot_table/gameplay/fishing.json");
        try(var reader=level.getServer().getResourceManager().getResourceOrThrow(path).openAsReader()){
            vanilla=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        }
        var treasureGate=vanilla.asList().stream().map(JsonElement::getAsJsonObject).filter(e->e.get("value").getAsString().endsWith("/treasure")).findFirst().orElseThrow().get("conditions");
        for(String table:List.of("void_starflow","reversion_mire")){
            var entries=read("/data/omnira/loot_table/fishing/"+table+".json").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
            h.assertTrue(treasureGate.equals(entries.get(2).getAsJsonObject().get("conditions")),"Treasure differs from the vanilla open-water rule");
            var liquid=table.equals("void_starflow")?com.mcmagic.omnira.space.sea.VoidSeaContent.LIQUID.get():com.mcmagic.omnira.mire.MireContent.LIQUID.get();
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=2;y++)
                level.setBlockAndUpdate(center.offset(x,y,z),y<=0?liquid.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            h.assertTrue((boolean)area.invoke(hook,center),"Unobstructed source pool rejected");
            level.setBlockAndUpdate(center.east(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            h.assertTrue(!(boolean)area.invoke(hook,center),"Obstructed pool qualified for treasure");
        }
        h.succeed();
    }
}
