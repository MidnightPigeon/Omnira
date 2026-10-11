package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.phase.*;
import com.mcmagic.omnira.space.anchor.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_performance") @PrefixGameTestTemplate(false)
public final class PerformanceGameTests {
    @GameTest(template="spell_arena") public static void phaseCacheInvalidatesOnTopology(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));
        level.setBlock(p,PhaseContent.BLOCK.get().defaultBlockState(),2);
        level.setBlock(p.east(),PhaseContent.BLOCK.get().defaultBlockState(),2);
        var group=PhaseNetworks.component(level,p);
        h.assertTrue(group.blocks().size()==2&&group==PhaseNetworks.component(level,p.east()),"Component not cached across members");
        level.setBlock(p.east(),Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(PhaseNetworks.component(level,p).blocks().size()==1,"Removed member remained cached");
        level.setBlock(p.west(),PhaseContent.BLOCK.get().defaultBlockState(),2);
        h.assertTrue(PhaseNetworks.component(level,p).blocks().size()==2,"New member not merged");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedScanBudgetIsBounded(GameTestHelper h){
        int total=0;for(int i=0;i<100;i++){
            int count=ActivationScanBudget.take(h.getLevel(),h.absolutePos(new BlockPos(i,4,5)));
            h.assertTrue(count>=0&&count<=512,"Individual budget escaped bound");total+=count;
        }
        h.assertTrue(total<=4096,"Multiple anchors multiply per-tick budget");h.succeed();
    }
    @GameTest(template="spell_arena") public static void indexedFieldUpdatesImmediately(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));
        level.setBlockAndUpdate(p,AnchorContent.ACTIVE.get().defaultBlockState());
        var center=net.minecraft.world.phys.Vec3.atCenterOf(p);
        h.assertTrue(StabilityField.strength(level,center.add(64,0,0))==2,"Indexed sphere boundary missing");
        level.setBlockAndUpdate(p,AnchorContent.DORMANT.get().defaultBlockState());
        h.assertTrue(StabilityField.strength(level,center)==0,"Index retained dormant source");
        level.setBlockAndUpdate(p,AnchorContent.ACTIVE.get().defaultBlockState());
        h.assertTrue(StabilityField.strength(level,center)==2,"Index did not restore source");
        level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());h.succeed();
    }
}
