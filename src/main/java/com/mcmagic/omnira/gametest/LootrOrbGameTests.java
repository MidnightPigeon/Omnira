package com.mcmagic.omnira.gametest;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("omnira_lootr") @PrefixGameTestTemplate(false)
public final class LootrOrbGameTests {
    @GameTest(template="spell_arena") public static void playerBreakDropsOnlyCleanShell(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")){h.succeed();return;}LootrOrbTestCases.playerBreakDropsOnlyCleanShell(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=300) public static void personalItemsPersistAndStayIsolated(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")){h.succeed();return;}
        LootrOrbTestCases.personalItemsPersistAndStayIsolated(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=300) public static void personalFluidsPersistAndStayIsolated(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")){h.succeed();return;}
        LootrOrbTestCases.personalFluidsPersistAndStayIsolated(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=300) public static void nativeConversionAndNoOrbDrops(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")){h.succeed();return;}
        LootrOrbTestCases.nativeConversionAndNoOrbDrops(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=300) public static void actualStructoryTemplateConvertsAndGeneratesLoot(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")){h.succeed();return;}
        LootrOrbTestCases.actualStructoryTemplateConvertsAndGeneratesLoot(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=300) public static void actualStructoryChestConvertsAndGeneratesLoot(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")){h.succeed();return;}
        LootrOrbTestCases.actualStructoryChestConvertsAndGeneratesLoot(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=300) public static void ordinaryStorageAndSandStayNative(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")){h.succeed();return;}
        LootrOrbTestCases.ordinaryStorageAndSandStayNative(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=300) public static void sameLootrOwnerUsesOneLiveView(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")){h.succeed();return;}
        LootrOrbTestCases.sameLootrOwnerUsesOneLiveView(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=400) public static void cataclysmTreasureConverts(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")||!net.neoforged.fml.ModList.get().isLoaded("cataclysm")){h.succeed();return;}LootrOrbTestCases.cataclysmTreasureConverts(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void blockedAutomationShatteringAndPersonalBucket(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")){h.succeed();return;}LootrOrbTestCases.blockedAutomationShatteringAndPersonalBucket(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=400) public static void cataclysmChestConverts(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr")||!net.neoforged.fml.ModList.get().isLoaded("cataclysm")){h.succeed();return;}LootrOrbTestCases.cataclysmChestConverts(h);
    }
}
