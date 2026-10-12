package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.ManaEngineBlockEntity;
import com.mcmagic.omnira.block.entity.ManaEngineState;
import net.minecraft.gametest.framework.GameTestHelper;

/** Isolate optional Create types from the scanned test holder. */
final class EngineCoreCreateChecks {
    static void ratedCapacity(GameTestHelper h,ManaEngineState state,int total){
        var kinetic=(ManaEngineBlockEntity)state.host;
        h.assertTrue(kinetic.calculateAddedStressCapacity()*16==total,"Actual Create stress differs from rating");
    }
    private EngineCoreCreateChecks(){}
}
