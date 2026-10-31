package com.mcmagic.omnira.gametest;

import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_sable_machines")
@PrefixGameTestTemplate(false)
public final class SableMultiblockGameTests {
    @GameTest(template="spell_arena") public static void tuningNodesMoveTogether(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))SableStorageChecks.nodes(h,true);else h.succeed();
    }
    @GameTest(template="spell_arena") public static void tuningNodesDisconnectAcrossStructures(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))SableStorageChecks.nodes(h,false);else h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedPulseUpgradeSurvivesAssembly(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))SableStorageChecks.pulse(h);else h.succeed();
    }
    @GameTest(template="spell_arena") public static void resonantItemsSurviveAssembly(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))SableStorageChecks.run(h,false);else h.succeed();
    }
    @GameTest(template="spell_arena") public static void resonantFluidsSurviveAssembly(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))SableStorageChecks.run(h,true);else h.succeed();
    }
    private static void check(GameTestHelper h,String machine){
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))SableMachineChecks.run(h,machine);else h.succeed();
    }
    @GameTest(template="spell_arena") public static void aggregation(GameTestHelper h){check(h,"aggregation");}
    @GameTest(template="spell_arena") public static void reactionPool(GameTestHelper h){check(h,"pool");}
    @GameTest(template="spell_arena") public static void catalysis(GameTestHelper h){check(h,"catalysis");}
    @GameTest(template="spell_arena") public static void spacetimeEngine(GameTestHelper h){check(h,"engine");}
    @GameTest(template="spell_arena") public static void miniatureWorld(GameTestHelper h){check(h,"framework");}
    @GameTest(template="spell_arena") public static void hyperDrill(GameTestHelper h){check(h,"drill");}
    @GameTest(template="spell_arena") public static void portsOnOneShip(GameTestHelper h){check(h,"ports");}
    @GameTest(template="spell_arena") public static void fluidPortsOnOneShip(GameTestHelper h){check(h,"fluid_ports");}
    @GameTest(template="spell_arena") public static void kineticPortsOnOneShip(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))check(h,"kinetic_ports");else h.succeed();
    }
    @GameTest(template="spell_arena") public static void onePortMovedDisconnects(GameTestHelper h){check(h,"one_port");}
    @GameTest(template="spell_arena") public static void nativeStormPreservesOutsideBlocks(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))SpacetimeCompatibilityChecks.sable(h);else h.succeed();
    }
}
