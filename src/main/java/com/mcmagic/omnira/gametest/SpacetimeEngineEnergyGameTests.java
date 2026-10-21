package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.engine.*;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.space.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spacetime_energy")
@PrefixGameTestTemplate(false)
public final class SpacetimeEngineEnergyGameTests {
    @net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
    public static final class Fixture {
        @net.neoforged.bus.api.SubscribeEvent
        public static void register(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event){
            // Explicit headless-test opt-in: never adds a provider to a normal game.
            if(Boolean.getBoolean("omnira.testEnergyProvider"))event.registerBlock(Capabilities.EnergyStorage.BLOCK,
                    (level,pos,state,entity,side)->new net.neoforged.neoforge.energy.EnergyStorage(40000),Blocks.GOLD_BLOCK);
        }
    }
    private static SpacetimeEngineController engine(GameTestHelper h){
        h.assertTrue(com.mcmagic.omnira.energy.EnergyIntegration.available(),"Run with -PtestEnergyProvider");
        var root=h.absolutePos(new BlockPos(4,3,4));
        h.assertTrue(SpacetimeEngineStructure.place(h.getLevel(),root),"Engine placement failed");
        return SpacetimeEngineBlock.controller(h.getLevel(),root);
    }
    @GameTest(template="spell_arena") public static void facesBudgetAndShutdown(GameTestHelper h){
        var e=engine(h);var l=h.getLevel();var root=e.getBlockPos().below();
        for(int i=0;i<4;i++){
            var face=SpacetimeEngineBlock.OUTPUTS[i];var pos=root.relative(face);
            var port=l.getCapability(Capabilities.EnergyStorage.BLOCK,pos,face);
            h.assertTrue(port!=null&&port.canExtract()&&!port.canReceive()&&port.receiveEnergy(100,false)==0,"Not output-only");
            for(var wrong:Direction.values())if(wrong!=face)h.assertTrue(l.getCapability(Capabilities.EnergyStorage.BLOCK,pos,wrong)==null,"Extra power face");
            int offered=port.extractEnergy(10000,true);
            h.assertTrue(offered==25&&port.extractEnergy(10000,true)==offered,"Simulation spent budget");
            h.assertTrue(port.extractEnergy(10000,false)==offered&&port.extractEnergy(10000,false)==0,"Per-face duplicate output");
        }
        e.configure(0,2048,128,false);
        h.assertTrue(e.energy.rate(0)==2048,"Wrong electrical ratio");
        e.configure(0,2048,16,true);
        h.assertTrue(e.energy.rate(0)==2048,"RPM or reverse multiplied FE");
        var cached=e.energy.port(1);l.setBlockAndUpdate(root.offset(-1,0,-1),Blocks.AIR.defaultBlockState());
        h.assertTrue(!cached.canExtract()&&cached.extractEnergy(100,true)==0,"Broken engine retained power");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=60) public static void exactPerSecondAndNormalTicker(GameTestHelper h){
        var e=engine(h);int[] totals=new int[4];
        for(int t=0;t<20;t++){
            final int step=t;h.runAfterDelay(t+1,()->{
                for(int side=0;side<4;side++){
                    var port=e.energy.port(side);totals[side]+=port.extractEnergy(Integer.MAX_VALUE,false);
                    h.assertTrue(port.extractEnergy(Integer.MAX_VALUE,false)==0,"Repeated pull exceeded budget");
                }
                if(step==19){for(int total:totals)h.assertTrue(total==512,"Fractional FE rate drifted: "+total);h.succeed();}
            });
        }
    }
    @GameTest(template="spell_arena") public static void gridsAffectFeWithoutChangingBaseDrive(GameTestHelper h){
        var e=engine(h);
        for(int i=0;i<3;i++)e.grids.setStackInSlot(i,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.BASIC_CRYSTAL_GRID.get()));
        h.assertTrue(e.limit()==(int)Math.round(2048*1.2*1.2*1.2)&&e.energy.rate(0)==512,"Grids must raise the configurable ceiling, not override configured output");
        for(int i=0;i<3;i++)e.grids.setStackInSlot(i,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.OMNI_CRYSTAL_GRID.get()));
        h.assertTrue(e.configure(0,131072,128,false)&&e.energy.rate(0)==131072&&e.generatedSpeed(0)==16,"Unified FE ceiling or fixed conversion speed wrong");
        for(int i=0;i<3;i++)e.grids.setStackInSlot(i,net.minecraft.world.item.ItemStack.EMPTY);
        h.assertTrue(e.energy.rate(0)==2048,"Removal retained out-of-range configured output");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void enginePushesThroughPassivePorts(GameTestHelper h){
        var e=engine(h);var l=h.getLevel();var center=h.absolutePos(new BlockPos(9,3,9));
        h.assertTrue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.place(l,center,Direction.NORTH),"Pool placement failed");
        var pool=com.mcmagic.omnira.alchemy.ReactionPoolBlock.controller(l,center);
        var a=patch(h,e.getBlockPos().below().east(),Direction.EAST);
        var b=patch(h,center.east(),Direction.EAST);
        h.assertTrue(SpacetimePortState.pair(b,a),"Reverse-color link failed");
        a.tick();b.tick();h.assertTrue(pool.energyInput.getEnergyStored()==0,"Port autonomously moved energy");
        var cached=b.energyPort(Direction.WEST);h.assertTrue(!cached.canExtract()&&cached.extractEnergy(100,false)==0,"Native connection exposed duplicate FE");
        e.energy.tick();h.assertTrue(pool.energyInput.getEnergyStored()==0,"Insufficient native network bypassed pulse priority using FE");
        b.unlink();h.assertTrue(!cached.canExtract()&&cached.extractEnergy(100,false)==0,"Unlinked proxy retained source");
        var external=h.absolutePos(new BlockPos(11,3,4));l.setBlockAndUpdate(external,Blocks.GOLD_BLOCK.defaultBlockState());
        var c=patch(h,external,Direction.EAST);h.assertTrue(SpacetimePortState.pair(a,c),"External link failed");
        var source=e.getBlockPos().below().east();
        h.assertTrue(com.mcmagic.omnira.pulse.PulseOutput.mode(l,source,Direction.EAST)==com.mcmagic.omnira.pulse.PulseOutput.PULSE
            &&com.mcmagic.omnira.pulse.PulseOutput.consumptionMode(l,source,Direction.EAST)==com.mcmagic.omnira.pulse.PulseOutput.ELECTRIC,"Remote external conversion lost reservation or changed source animation");
        var output=c.energyPort(Direction.WEST);h.assertTrue(output.canExtract(),"External source missing");
        h.assertTrue(output.extractEnergy(100,false)==25,"Passive electrical source conversion disappeared");
        h.assertTrue(output.extractEnergy(100,false)==0,"Repeated pull duplicated FE");
        c.unlink();h.assertTrue(!output.canExtract()&&output.extractEnergy(100,false)==0,"External unlinked proxy retained source");h.succeed();
    }
    private static SpacetimePortState patch(GameTestHelper h,BlockPos attached,Direction face){
        var pos=attached.relative(face);h.getLevel().setBlockAndUpdate(pos,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(SpacetimePortBlock.FACING,face));
        return SpacetimePortState.at(h.getLevel(),pos);
    }
}
