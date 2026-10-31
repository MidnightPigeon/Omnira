package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.fate.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.spell.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;

@GameTestHolder("omnira_arcanist") @PrefixGameTestTemplate(false)
public final class ArcanistGameTests {
    private static FakePlayer player(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"arcanist"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5,3,5)));p.setXRot(-90);
        return p;
    }
    private static FakePlayer awakened(GameTestHelper h){
        var p=player(h);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.RAIN_SOAKED_SUITCASE.get()));
        ModItems.RAIN_SOAKED_SUITCASE.get().use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);return p;
    }
    @GameTest(template="spell_arena") public static void suitcaseImmediateRetainedAndCannotOverwrite(GameTestHelper h){
        var p=awakened(h);
        h.assertTrue(ManuscriptReward.equipped(p,"talent")==FateCurioItem.Kind.ARCANIST&&ManuscriptReward.equipped(p,"curse")==FateCurioItem.Kind.STORM,"Pair missing");
        var stack=p.getMainHandItem();h.assertTrue(stack.getCount()==1&&stack.get(ModDataComponents.SUITCASE_OPEN_UNTIL)==h.getLevel().getGameTime()+RainSoakedSuitcaseItem.ACTIVATION_TICKS,"Suitcase consumed or animation missing");
        ArcanistFate.data(p).putInt("Clock",100);
        stack.getItem().use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(ArcanistFate.data(p).getInt("Clock")==100,"Repeated use reset curse");
        var mods=((FateCurioItem)ModItems.ARCANIST_TALENT.get()).getAttributeModifiers(new top.theillusivec4.curios.api.SlotContext("talent",p,0,false,true),net.minecraft.resources.ResourceLocation.parse("omnira:test"),new ItemStack(ModItems.ARCANIST_TALENT.get()));
        h.assertTrue(mods.get(ModAttributes.MANA_REGEN).iterator().next().amount()==.3,"Regen attribute incorrect");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rankCyclesByIndividualInstallation(GameTestHelper h){
        var p=awakened(h);var a=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());var b=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());var other=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());
        for(int n=0;n<7;n++){h.assertTrue(ArcanistFate.bonus(p,a)==n%3*.25,"Wrong rank");ArcanistFate.completed(p,a);}
        h.assertTrue(ArcanistFate.bonus(p,b)==0,"Switch retained rank");ArcanistFate.completed(p,b);
        h.assertTrue(ArcanistFate.bonus(p,other)==0,"Same item type treated as same installation");
        h.assertTrue(!ArcanistFate.cast(p,other,power->false)&&ArcanistFate.bonus(p,b)==.25,"Failed cast advanced or destroyed rank");
        ArcanistFate.completed(p,other);h.assertTrue(ArcanistFate.bonus(p,b)==0&&ArcanistFate.bonus(p,a)==0,"Switch back restored old progression");
        var saved=ItemStack.parseOptional(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)other.save(h.getLevel().registryAccess()));
        h.assertTrue(ArcanistFate.bonus(p,saved)==.25,"Carrier identity did not survive save");h.succeed();
    }
    @GameTest(template="spell_arena") public static void actualGridCastsShareAcrossStavesAndGunResets(GameTestHelper h){
        var p=awakened(h);p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000,0));
        var crystal=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());crystal.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(ElementType.AIR,ElementType.AIR));crystal.set(ModDataComponents.SPELL_PAYLOAD,SpellPayload.EMPTY);
        var grid=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());grid.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(crystal)));
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("crystal_grid",0,grid);
        double base=0;
        for(int n=0;n<3;n++){
            var staff=com.mcmagic.omnira.item.staff.StaffAssembly.basic().create();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,staff);
            h.assertTrue(StaffItem.castSequence(p,staff,1)==1,"Staff cast failed");
            var shots=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.spell.entity.SpellEntity.class,p.getBoundingBox().inflate(4),e->e.ownedBy(p));
            h.assertTrue(shots.size()==1,"Unexpected shot count");if(n==0)base=shots.getFirst().power();
            h.assertTrue(Math.abs(shots.getFirst().power()-base-n*.25)<.001,"Staff swap reset grid or power not applied");shots.forEach(net.minecraft.world.entity.Entity::discard);
        }
        StaffItem.castSequence(p,p.getMainHandItem(),1);
        var gun=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());gun.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(crystal)));p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,gun);
        h.assertTrue(ArcaneArquebusItem.castLoaded(p,gun)&&ArcanistFate.bonus(p,grid)==0&&ArcanistFate.bonus(p,gun)==.25,"Gun did not switch installation");h.succeed();
    }
    @GameTest(template="spell_arena") public static void milkCannotClearAndManaStopsRegenerating(GameTestHelper h){
        var p=awakened(h);ArcanistFate.data(p).putInt("Clock",ArcanistFate.PERIOD-1);
        p.getAttribute(ModAttributes.MAX_MANA).setBaseValue(1000);p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(100,1000,0));
        ArcanistFate.advance(p);h.assertTrue(p.getData(ModAttachments.MANA).current()==50&&ArcanistFate.storm(p),"Rain drain wrong");
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET),h.getLevel(),p);p.removeAllEffects();
        p.tickCount=20;com.mcmagic.omnira.mana.ManaEvents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(ArcanistFate.storm(p)&&p.getData(ModAttachments.MANA).current()==50,"Milk cleared curse or regeneration continued");
        ArcanistFate.drain(p);h.assertTrue(p.getHealth()==20&&p.getData(ModAttachments.MANA).current()==0,"Draining last mana also took health");
        ArcanistFate.drain(p);h.assertTrue(p.getHealth()==10,"Health not based on maximum mana");
        ManuscriptReward.cleanse(p);h.assertTrue(!ArcanistFate.storm(p)&&!FateRuntime.data(p).contains(ArcanistFate.KEY),"Cleanse left rain state");h.succeed();
    }
    @GameTest(template="spell_arena") public static void fractionalHealthRoundsUpAndCanKill(GameTestHelper h){
        var p=awakened(h);p.getAttribute(ModAttributes.MAX_MANA).setBaseValue(101);p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(0,101,0));
        ArcanistFate.drain(p);h.assertTrue(p.getHealth()==18,"Health rounding wrong");p.setHealth(1);ArcanistFate.drain(p);h.assertTrue(!p.isAlive(),"Storm cannot kill");h.succeed();
    }
    @GameTest(template="spell_arena") public static void warningDurationAndRespawnPreserveClock(GameTestHelper h){
        var p=awakened(h);var s=ArcanistFate.data(p);s.putInt("Clock",ArcanistFate.PERIOD-ArcanistFate.WARNING-1);
        ArcanistFate.advance(p);h.assertTrue(!ArcanistFate.storm(p)&&s.getInt("Rain")==0,"Warning caused drain");
        p.getAttribute(ModAttributes.MAX_MANA).setBaseValue(100);p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,100,0));
        for(int i=0;i<200;i++)ArcanistFate.advance(p);
        h.assertTrue(s.getInt("Rain")==299,"Warning is not ten seconds");
        for(int i=0;i<299;i++)ArcanistFate.advance(p);
        h.assertTrue(s.getInt("Rain")==0&&p.getData(ModAttachments.MANA).current()==925,"Not fifteen one-second drain pulses");
        s.putInt("Rain",123);var next=player(h);ArcanistFate.clone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(next,p,true));
        h.assertTrue(ArcanistFate.data(next).getInt("Rain")==123&&ArcanistFate.data(next).getInt("Clock")==s.getInt("Clock"),"Respawn resets curse");h.succeed();
    }
    @GameTest(template="spell_arena") public static void stormBlocksRanksUntilCleansed(GameTestHelper h){
        var p=awakened(h);var grid=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());ArcanistFate.completed(p,grid);ArcanistFate.completed(p,grid);
        ArcanistFate.data(p).putInt("Clock",ArcanistFate.PERIOD-1);ArcanistFate.advance(p);
        ArcanistFate.completed(p,grid);h.assertTrue(ArcanistFate.bonus(p,grid)==0&&!ArcanistFate.data(p).contains("Casts"),"Ranks accrued during storm");
        ManuscriptReward.cleanse(p);h.assertTrue(ArcanistFate.bonus(p,grid)==0,"Lost talent retained rank");h.succeed();
    }
    @GameTest(template="spell_arena") public static void lootMigrationAndNestedChance(GameTestHelper h)throws Exception{
        var defaults=com.mcmagic.omnira.config.OmniraLootConfig.defaults().getAsJsonObject("treasureTables");
        com.google.gson.JsonObject previous;
        try(var in=new java.io.InputStreamReader(ArcanistGameTests.class.getResourceAsStream("/loot_migrations/storm_suitcase_v1.json"),java.nio.charset.StandardCharsets.UTF_8)){previous=com.google.gson.JsonParser.parseReader(in).getAsJsonObject();}
        var custom=previous.deepCopy();custom.getAsJsonObject("omnira:chests/void_lighthouse").addProperty("random_sequence","pack:custom");var saved=custom.deepCopy();
        h.assertTrue(com.mcmagic.omnira.config.OmniraLootConfig.migrateDefaultStormSuitcase(previous),"No migration");
        com.mcmagic.omnira.config.OmniraLootConfig.migrateDefaultStormSuitcase(custom);
        h.assertTrue(custom.get("omnira:chests/void_lighthouse").equals(saved.get("omnira:chests/void_lighthouse")),"Overwrote custom loot");
        for(String name:java.util.List.of("fold_observatory_lower","fold_observatory_upper","calibration_materials","calibration_supplies","skyreach_supplies","void_lighthouse","radiant_courtyard")){
            var table=defaults.getAsJsonObject("omnira:chests/"+name);int count=0;
            for(var pool:table.getAsJsonArray("pools"))if(pool.toString().contains("rain_soaked_suitcase")){
                count++;h.assertTrue(pool.getAsJsonObject().getAsJsonArray("conditions").get(0).getAsJsonObject().get("chance").getAsDouble()==.05,"Not independent five percent");
            }
            h.assertTrue(count==1,"Repeated or missing suitcase pool");
        }
        h.assertTrue(!defaults.get("omnira:chests/topological_labyrinth").toString().contains("rain_soaked_suitcase"),"Nested labyrinth double-rolls suitcase");
        h.assertTrue(defaults.get("omnira:chests/radiant_courtyard").toString().contains("omnira:void_starsea"),"Dream light loot got spatial-only suitcase");h.succeed();
    }
}
