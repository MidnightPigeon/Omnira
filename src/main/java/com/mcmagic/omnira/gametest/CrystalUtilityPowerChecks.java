package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.compat.create.KineticCrystalWorkstationEntity;
import com.mcmagic.omnira.workstation.CrystalWorkstations;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Kept out of the scanned holder so Create remains optional. */
final class CrystalUtilityPowerChecks {
    static void fixedOccupancy(GameTestHelper h){
        for(int index:new int[]{3,5}){
            var machine=new KineticCrystalWorkstationEntity(BlockPos.ZERO,CrystalWorkstations.BLOCKS.get(index).get().defaultBlockState());
            for(float rpm:new float[]{16,32,128,256,4096,-4096}){
                machine.setSpeed(rpm);
                h.assertTrue(Math.abs(machine.calculateStressApplied()*Math.abs(rpm)-256)<.001,"Fixed machine occupancy changed with RPM");
            }
        }
    }
}
