package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.*;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_port_automation") @PrefixGameTestTemplate(false)
public final class PortAutomationGameTests {
    static SpacetimePortState port(GameTestHelper h,BlockPos support,Direction face){
        h.getLevel().setBlockAndUpdate(support.relative(face),ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(SpacetimePortBlock.FACING,face));
        return SpacetimePortState.at(h.getLevel(),support.relative(face));
    }
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void hopperUsesRotatedRemoteFace(GameTestHelper h){
        var level=h.getLevel();var a=h.absolutePos(new BlockPos(2,4,2));var b=h.absolutePos(new BlockPos(8,4,8));
        level.setBlockAndUpdate(a,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.DOWN));
        level.setBlockAndUpdate(b,Blocks.CHEST.defaultBlockState());
        var hopper=(HopperBlockEntity)level.getBlockEntity(a);var chest=(ChestBlockEntity)level.getBlockEntity(b);
        h.assertTrue(SpacetimePortState.pair(port(h,a,Direction.DOWN),port(h,b,Direction.EAST)),"Pair failed");
        hopper.setItem(0,new ItemStack(Items.DIAMOND,4));
        h.runAfterDelay(50,()->{h.assertTrue(hopper.isEmpty()&&chest.getItem(0).getCount()==4,"Hopper did not use native output cadence");h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=160)
    public static void createFunnelOutputsToRemoteContainer(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        PortCreateAutomationChecks.funnel(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void createPumpCrossesRotatedPipeFaces(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        PortCreateAutomationChecks.pipes(h);
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void createPumpFeedsRemoteTank(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        PortCreateAutomationChecks.pipes(h,false);
    }
}
