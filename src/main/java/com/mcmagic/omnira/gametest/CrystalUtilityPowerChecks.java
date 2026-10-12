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
            for(float rpm:new float[]{1,7,16,17,32,128,129,256,4096,-4096}){
                machine.setSpeed(rpm);
                h.assertTrue(Math.abs(machine.calculateStressApplied()*Math.abs(rpm)-256)<.001,"Fixed machine occupancy changed with RPM");
            }
        }
        var assembly=new com.mcmagic.omnira.block.entity.KineticAssemblyBlockEntity(BlockPos.ZERO,com.mcmagic.omnira.registry.ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState());
        for(float rpm:new float[]{1,7,16,17,32,128,129,256,4096,-4096}){
            assembly.setSpeed(rpm);
            h.assertTrue(Math.abs(assembly.calculateStressApplied()*Math.abs(rpm)-256)<.001,"Assembly occupancy changed with RPM");
        }
    }
}
