package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_machine_power")
@PrefixGameTestTemplate(false)
public final class AssemblyEnergyGameTests {
    @GameTest(template="spell_arena") public static void kineticRateAcceleratesAssemblyWithoutPowerAfterStop(GameTestHelper h){
        var placed=table(h);var table=new ArcaneAssemblyTableBlockEntity(placed.getBlockPos(),placed.getBlockState());table.setLevel(h.getLevel());
        table.setItem(3,new ItemStack(ModItems.WOODEN_STAFF_SHAFT.get()));table.setItem(6,new ItemStack(ModItems.IRON_REINFORCEMENT.get()));table.setItem(0,new ItemStack(ModItems.SPIRITUAL_CRYSTAL_TIP.get()));
        for(int i=0;i<4;i++)table.tickPower(true,32);
        h.assertTrue(table.progress()==0,"32 RPM assembly struck before five ticks");
        table.tickPower(true,32);h.assertTrue(table.progress()>0,"32 RPM assembly did not strike after five ticks");int before=table.progress();
        for(int i=0;i<10;i++)table.tickPower(false,0);
        h.assertTrue(table.progress()==before&&!table.powered(),"Stopped mechanical input continued assembly");h.succeed();
    }
    @GameTest(template="spell_arena") public static void exactSharedInputBudget(GameTestHelper h){
        for(int rate:new int[]{256,8192})for(int start=0;start<20;start++){
            var limit=new com.mcmagic.omnira.energy.EnergyInputLimit(rate);int total=0;
            for(long tick=start;tick<start+20;tick++){
                int offer=limit.receive(tick,Integer.MAX_VALUE,Integer.MAX_VALUE,true);
                h.assertTrue(offer==limit.receive(tick,Integer.MAX_VALUE,Integer.MAX_VALUE,true),"Simulation spent rate budget");
                int first=limit.receive(tick,1,Integer.MAX_VALUE,false);
                int rest=limit.receive(tick,Integer.MAX_VALUE,Integer.MAX_VALUE,false);
                h.assertTrue(first+rest==offer&&limit.receive(tick,1,Integer.MAX_VALUE,false)==0,"Multi-call budget bypass");
                total+=first+rest;
            }
            h.assertTrue(total==rate,"Fractional FE rate drifted");
            h.assertTrue(limit.receive(10000,Integer.MAX_VALUE,Integer.MAX_VALUE,false)<=Math.ceil(rate/20.0),"Idle time banked a burst");
        }
        h.succeed();
    }
    private static ArcaneAssemblyTableBlockEntity table(GameTestHelper h) {
        h.setBlock(4,2,4,ModBlocks.ARCANE_ASSEMBLY_TABLE.get());
        return ((AssemblyAccess)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4,2,4)))).assembly();
    }
    @GameTest(template="spell_arena") public static void electricityWorksOnBothHostsAndOnlyInputFace(GameTestHelper h) {
        var table=table(h);var host=h.getLevel().getBlockEntity(table.getBlockPos());
        var back=host.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        for(var face:Direction.values())h.assertTrue((h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,host.getBlockPos(),face)!=null)==(face==back),"FE exposed on wrong face");
        int accepted=table.energyInput.receiveEnergy(Integer.MAX_VALUE,true);
        h.assertTrue(accepted>=102&&accepted<=103&&table.energyInput.getEnergyStored()==0,"Simulation or input rate failed");
        var stored=table.getUpdateTag(h.getLevel().registryAccess());stored.putInt("AssemblyEnergy",256);
        table.loadWithComponents(stored,h.getLevel().registryAccess());
        table.setItem(3,new ItemStack(ModItems.WOODEN_STAFF_SHAFT.get()));
        table.setItem(6,new ItemStack(ModItems.IRON_REINFORCEMENT.get()));
        table.setItem(0,new ItemStack(ModItems.SPIRITUAL_CRYSTAL_TIP.get()));
        table.tickPower(false);h.assertTrue(!table.powered(),"Legacy cached FE still powered assembly");
        int[] received={0};for(int i=1;i<=40;i++)h.runAfterDelay(i,()->{
            received[0]+=table.energyInput.receiveEnergy(Integer.MAX_VALUE,false);
            h.assertTrue(table.energyInput.receiveEnergy(256,false)==0,"Repeated calls bypassed tick budget");
        });
        h.runAfterDelay(41,()->{
            table.tickPower(false);h.assertTrue(table.electricalPowered()&&!table.mechanicalPowered()&&table.progress()>0&&received[0]==4096&&table.energyInput.getEnergyStored()==0,"Continuous FE work, idle draw or exact rate failed");
        });
        h.runAfterDelay(42,()->{table.tickPower(false);h.assertTrue(!table.powered(),"Stopped input left assembly running");h.succeed();});
    }
    @GameTest(template="spell_arena") public static void mechanicalPriorityAndElectricalPacket(GameTestHelper h) {
        var table=table(h);var stored=table.getUpdateTag(h.getLevel().registryAccess());stored.putInt("AssemblyEnergy",512);
        table.loadWithComponents(stored,h.getLevel().registryAccess());table.tickPower(true);
        h.assertTrue(table.mechanicalPowered()&&!table.electricalPowered()&&table.energyInput.receiveEnergy(100,false)==0,"Mechanical drive also accepted FE");
        table.tickPower(false);h.assertTrue(!table.powered(),"Cached FE bypassed stopped input");
        for(int i=1;i<=10;i++)h.runAfterDelay(i,()->table.energyInput.receiveEnergy(Integer.MAX_VALUE,false));
        h.runAfterDelay(11,()->{
            table.tickPower(false);var tag=table.getUpdateTag(h.getLevel().registryAccess());
            var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var menu=new com.mcmagic.omnira.menu.ArcaneAssemblyTableMenu(1,player.getInventory(),table);
            h.assertTrue(menu.powered()&&menu.manaCost()==0&&!menu.canStrike(),"Electric menu still enabled manual mana forging");
            var copy=new ArcaneAssemblyTableBlockEntity(table.getBlockPos(),table.getBlockState());copy.loadWithComponents(tag,h.getLevel().registryAccess());
            h.assertTrue(copy.electricalPowered()&&!copy.mechanicalPowered()&&copy.energyInput.getEnergyStored()==0,"Client packet lost electrical mode");
            h.assertTrue(table.energyInput.receiveEnergy(100,true)==table.energyInput.receiveEnergy(100,true)&&table.energyInput.extractEnergy(50,false)==0,"Simulation or extraction mutated energy");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void ownEngineRemainsFunctionalWithoutCreate(GameTestHelper h) {
        if(net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        var table=table(h);
        var facing=table.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        var enginePos=table.getBlockPos().relative(facing.getOpposite());
        h.getLevel().setBlockAndUpdate(enginePos,ModBlocks.MANA_ENGINE.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,facing));
        var engine=((ManaEngineAccess)h.getLevel().getBlockEntity(enginePos)).engineState();
        engine.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        table.tickDirectDrive();
        h.assertTrue(table.pulsePowered()&&!table.mechanicalPowered()&&!table.electricalPowered(),"Own engine no longer provides native pulses");
        engine.control(0);h.runAfterDelay(1,()->{table.tickDirectDrive();h.assertTrue(!table.powered(),"Stopped own engine still drove machine");h.succeed();});
    }
}
