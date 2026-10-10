package com.mcmagic.omnira.gametest;

import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_port_pool")
@PrefixGameTestTemplate(false)
public final class PortPoolGameTests {
    @GameTest(template="spell_arena",timeoutTicks=180)
    public static void twoRemoteEnginesAndNetworkBudget(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        ReactionPoolCompatibilityChecks.verifyRemote(h);
    }
}
