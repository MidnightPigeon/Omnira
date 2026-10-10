package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.mire.MireContent;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
import com.mcmagic.omnira.time.DoomsdayRitePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_timeflow_storage")
@PrefixGameTestTemplate(false)
public final class TimeflowStorageGameTests {
    @GameTest(template="spell_arena",timeoutTicks=80)
    public static void infiniteTimeflowStorage(GameTestHelper h){
        h.setBlock(new BlockPos(3,3,3),ModBlocks.LIQUID_CRYSTAL_BALL.get());
        var ball=(LiquidCrystalBallBlockEntity)h.getBlockEntity(new BlockPos(3,3,3));
        ball.tank.fill(new FluidStack(MireContent.SOURCE.get(),1000),FluidAction.EXECUTE);
        h.assertTrue(ball.infinite(),"Timeflow must be renewable in liquid storage");
        h.runAfterDelay(22,()->{
            h.assertTrue(ball.fluidHandler.drain(1000,FluidAction.SIMULATE).getAmount()==500,"External renewable simulation ignored transfer budget");
            h.assertTrue(ball.fluidHandler.drain(1000,FluidAction.EXECUTE).getAmount()==500&&ball.tank.getFluidAmount()==1000,"External timeflow extraction depleted reserve");
            h.assertTrue(ball.tank.drain(1000,FluidAction.EXECUTE).getAmount()==1000&&ball.tank.isEmpty(),"Internal timeflow extraction did not debit real volume");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void twoSourcesRegenerate(GameTestHelper h){
        for(int x=2;x<=6;x++)for(int z=2;z<=4;z++){
            h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
            h.setBlock(new BlockPos(x,3,z),x==2||x==6||z==2||z==4?Blocks.STONE:Blocks.AIR);
        }
        h.setBlock(new BlockPos(3,3,3),MireContent.LIQUID.get());
        h.setBlock(new BlockPos(5,3,3),MireContent.LIQUID.get());
        h.succeedWhen(()->{
            var state=h.getLevel().getFluidState(h.absolutePos(new BlockPos(4,3,3)));
            h.assertTrue(MireContent.fluid(state)&&state.isSource(),"Two sources failed to replenish middle");
        });
    }
    @GameTest(template="spell_arena")
    public static void riteTreasureMetadata(GameTestHelper h){
        int items=0,fluids=0;
        for(var cell:DoomsdayRitePlan.cells()){
            var tag=DoomsdayRitePlan.treasure(cell,42);
            if(tag.contains("LootTable")){items++;h.assertTrue(tag.getString("LootTable").equals("omnira:chests/doomsday_rite"),"Wrong item treasure");}
            if(tag.getBoolean("FluidTreasure"))fluids++;
        }
        h.assertTrue(items==1&&fluids==1,"Rite must have one item and one fluid treasure");h.succeed();
    }
}
