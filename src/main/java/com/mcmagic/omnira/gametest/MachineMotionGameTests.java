package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.client.MachineRotation;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_machine_motion")
@PrefixGameTestTemplate(false)
public final class MachineMotionGameTests {
    @GameTest(template="spell_arena") public static void speedTransitionsKeepAngle(GameTestHelper h){
        var phase=new MachineRotation.Phase(45);
        phase.advance(10000,0,5);
        h.assertTrue(phase.advance(10000,1,5)==45,"Starting replaced angle with world time");
        double moving=phase.advance(10001,1,5);
        h.assertTrue(moving>45&&moving<46,"No smooth acceleration");
        h.assertTrue(phase.advance(10001,0,5)==moving,"Stopping reset angle");
        double slowing=phase.advance(10002,0,5);
        h.assertTrue(slowing>moving&&slowing-moving<1,"Stopping did not coast gently");h.succeed();
    }
    @GameTest(template="spell_arena") public static void interpolationIsFrameRateIndependent(GameTestHelper h){
        var a=new MachineRotation.Phase(0);var b=new MachineRotation.Phase(0);
        a.advance(0,0,5);b.advance(0,0,5);
        double one=a.advance(1,1,5),many=0;
        for(int i=1;i<=20;i++)many=b.advance(i/20.0,1,5);
        h.assertTrue(Math.abs(one-many)<1e-9,"Rotation depends on frame rate");h.succeed();
    }
    @GameTest(template="spell_arena") public static void freezeAndRenderGapsDoNotCatchUp(GameTestHelper h){
        var phase=new MachineRotation.Phase(30);phase.advance(0,1,0);
        h.assertTrue(phase.advance(1,1,0)==31,"Normal rotation failed");
        h.assertTrue(phase.advance(2,0,0)==31,"Stilled machine moved");
        h.assertTrue(phase.advance(1000,1,0)==31,"Offscreen gap caused catch-up");
        h.assertTrue(phase.advance(999,1,0)==31,"Clock reversal moved machine");
        h.assertTrue(phase.advance(1000,1,0)==32,"Resumed motion jumped");h.succeed();
    }
    @GameTest(template="spell_arena") public static void machinesKeepIndependentPhases(GameTestHelper h){
        var rotation=new MachineRotation(45,0);var a=new Object();var b=new Object();
        rotation.angle(a,0,1);rotation.angle(b,0,0);
        h.assertTrue(rotation.angle(a,1,1)==46&&rotation.angle(b,1,0)==45,"Machines shared phase");
        h.assertTrue(rotation.angle(null,10000,1)==45,"Preview mutated runtime state");h.succeed();
    }
}
