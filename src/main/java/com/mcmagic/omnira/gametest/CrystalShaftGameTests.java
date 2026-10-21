package com.mcmagic.omnira.gametest;

import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_crystal_shafts")
@PrefixGameTestTemplate(false)
public final class CrystalShaftGameTests {
    @GameTest(template="spell_arena",timeoutTicks=180) public static void codexRearShaftTurnsBelowThresholdAndRequiresFullDemand(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))CodexKineticChecks.check(h);else h.succeed();
    }
    @GameTest(template="spell_arena") public static void orbKeepsNativePulseAndElectricPresentation(GameTestHelper h){
        var pos=new net.minecraft.core.BlockPos(5,4,5);
        h.setBlock(pos,com.mcmagic.omnira.pulse.PulseContent.ORB.get());
        PulseRelayTests.engine(h,pos.north(),net.minecraft.core.Direction.SOUTH);
        var orb=com.mcmagic.omnira.pulse.PulseOrbEntity.from(h.getBlockEntity(pos));
        h.assertTrue(orb.conversion.face(net.minecraft.core.Direction.DOWN).receiveEnergy(128,false)==128,"FE fixture rejected");
        orb.shaftState=com.mcmagic.omnira.pulse.PulseOrbShaftState.capture(orb);
        h.assertTrue(com.mcmagic.omnira.pulse.PulseOrbShaftState.mode(orb.shaftState,net.minecraft.core.Direction.NORTH)==0,"Native pulse socket became a shaft");
        h.assertTrue(com.mcmagic.omnira.pulse.PulseOrbShaftState.incoming(orb.shaftState,net.minecraft.core.Direction.NORTH)
                &&!com.mcmagic.omnira.pulse.PulseOrbShaftState.outgoing(orb.shaftState,net.minecraft.core.Direction.NORTH),"Pulse rings travelled back into the source");
        h.assertTrue(com.mcmagic.omnira.pulse.PulseOrbShaftState.mode(orb.shaftState,net.minecraft.core.Direction.DOWN)==2,"FE input lost its mode");
        var tag=new net.minecraft.nbt.CompoundTag();orb.saveState(tag,h.getLevel().registryAccess());
        h.assertTrue(tag.getInt("ShaftState")==orb.shaftState,"Socket mode missing from sync state");h.succeed();
    }
    @GameTest(template="spell_arena") public static void nativeRequirementHasExactSignedBoundary(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))CrystalShaftCreateChecks.boundary(h);else h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=140) public static void lowSpeedMachinesReleaseStressAndRestart(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))CrystalShaftCreateChecks.machine(h);else h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=140) public static void relayRejectsSlowImportWithoutChangingSource(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))CrystalShaftCreateChecks.orbs(h);else h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void poolAllocatesOnlyEligibleInputs(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))CrystalShaftCreateChecks.pool(h);else h.succeed();
    }
}
