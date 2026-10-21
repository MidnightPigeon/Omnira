package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.pulse.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class PulseRelayCreateChecks {
    static void outputs(GameTestHelper h){
        var p=new BlockPos(6,3,6);h.setBlock(p,PulseContent.ORB.get());PulseRelayTests.engine(h,p.north(),Direction.SOUTH);
        h.setBlock(p.east(),AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        h.setBlock(p.south(),AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        var orb=PulseOrbEntity.from(h.getBlockEntity(p));var block=(IRotate)orb.host.getBlockState().getBlock();
        h.assertTrue(block.hasShaftTowards(h.getLevel(),orb.getBlockPos(),orb.host.getBlockState(),Direction.EAST),"Perpendicular shaft missing");
        h.assertTrue(!block.hasShaftTowards(h.getLevel(),orb.getBlockPos(),orb.host.getBlockState(),Direction.SOUTH),"Parallel shaft accepted");
        h.runAfterDelay(25,()->{
            var shaft=(KineticBlockEntity)h.getBlockEntity(p.east());
            h.assertTrue(Math.abs(shaft.getSpeed())==16,"Orb output is not 16 RPM: "+shaft.getSpeed());
            PulseRelayTests.close(h,PulseNetworks.occupied(orb),0);
            h.setBlock(p.east(2),AllBlocks.MECHANICAL_DRILL.getDefaultState().setValue(BlockStateProperties.FACING,Direction.EAST));
        });
        h.runAfterDelay(50,()->{
            var drill=(KineticBlockEntity)h.getBlockEntity(p.east(2));
            double stress=drill.getOrCreateNetwork().getActualStressOf(drill);
            h.assertTrue(stress>0&&!drill.isOverStressed()&&Math.abs(drill.getSpeed())==16,"Load was not driven");
            PulseRelayTests.close(h,PulseNetworks.occupied(orb),stress);
            h.setBlock(p.east(2),net.minecraft.world.level.block.Blocks.AIR);
        });
        h.runAfterDelay(70,()->{PulseRelayTests.close(h,PulseNetworks.occupied(orb),0);h.succeed();});
    }
    static void inputs(GameTestHelper h){
        var p=new BlockPos(6,4,6);h.setBlock(p,PulseContent.ORB.get().defaultBlockState().setValue(PulseOrbBlock.MOUNT,Direction.EAST));
        h.setBlock(p.west(),AllBlocks.GEARBOX.getDefaultState().setValue(BlockStateProperties.AXIS,Direction.Axis.Z));
        h.setBlock(p.west(2),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING,Direction.EAST));
        h.setBlock(p.west().above(),PulseContent.ORB.get());
        h.setBlock(p.east(),AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        var orb=PulseOrbEntity.from(h.getBlockEntity(p));
        h.runAfterDelay(25,()->{
            var motor=(KineticBlockEntity)h.getBlockEntity(p.west(2));var output=(KineticBlockEntity)orb.host;
            h.assertTrue(!java.util.Objects.equals(motor.network,output.network),"Input RPM leaked directly into output");
            var group=com.mcmagic.omnira.compat.create.PulseOrbKinetics.inputGroup(orb);
            h.assertTrue(Math.abs(output.getSpeed())==16,"Converted output not fixed at 16: speed="+output.getSpeed()+", generated="+output.getGeneratedSpeed()+", available="+PulseNetworks.available(orb)+", group="+(group==null?"none":group.sources().size())+", motor="+motor.getSpeed()+"/"+motor.hasNetwork());
            int amount=orb.conversion.reserve(PulseConversions.KINETIC,256,false);
            h.assertTrue(amount==256,"External SU did not become pulse");
            PulseRelayTests.close(h,PulseNetworks.occupied(orb),256);
            var second=PulseOrbEntity.from(h.getBlockEntity(p.west().above()));
            h.assertTrue(group!=null&&group.receivers().size()==2,"Shared external source was not recognized");
            h.assertTrue(second.conversion.reserve(PulseConversions.KINETIC,128,false)==128,"Second relay input unavailable");
            PulseNetworks.occupied(second);
            PulseRelayTests.close(h,motor.getOrCreateNetwork().calculateStress(),384);
            h.setBlock(p,net.minecraft.world.level.block.Blocks.AIR);
            h.setBlock(p.west().above(),net.minecraft.world.level.block.Blocks.AIR);
        });
        h.runAfterDelay(30,()->{var motor=(KineticBlockEntity)h.getBlockEntity(p.west(2));PulseRelayTests.close(h,motor.getOrCreateNetwork().calculateStress(),0);h.succeed();});
    }
    static void nativeTransit(GameTestHelper h){
        var p=new BlockPos(7,4,6);h.setBlock(p,PulseContent.ORB.get().defaultBlockState().setValue(PulseOrbBlock.MOUNT,Direction.EAST));
        h.setBlock(p.west(),AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        PulseRelayTests.engine(h,p.west(2),Direction.EAST);
        var orb=PulseOrbEntity.from(h.getBlockEntity(p));
        h.runAfterDelay(30,()->{
            var shaft=(KineticBlockEntity)h.getBlockEntity(p.west());
            h.assertTrue(Math.abs(shaft.getSpeed())==16,"Native source lost its normal shaft connection");
            PulseRelayTests.close(h,PulseNetworks.available(orb),512);
            h.setBlock(p.south(),com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(3).get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        });
        h.runAfterDelay(55,()->{
            PulseRelayTests.close(h,PulseNetworks.occupied(orb),256);
            PulseRelayTests.close(h,PulseNetworks.available(orb),256);
            PulseRelayTests.close(h,PulseNetworks.sourceOccupied(h.getLevel(),h.absolutePos(p.west(2)),Direction.EAST),256);
            var engine=(KineticBlockEntity)h.getBlockEntity(p.west(2));
            PulseRelayTests.close(h,engine.getOrCreateNetwork().calculateStress(),256);
            var saved=engine.saveWithFullMetadata(h.getLevel().registryAccess());
            PulseRelayTests.close(h,saved.getCompound("Network").getFloat("Stress"),0);
            ((com.mcmagic.omnira.block.entity.ManaEngineAccess)engine).engineState().control(0);
        });
        h.runAfterDelay(60,()->{
            PulseRelayTests.close(h,PulseNetworks.available(orb),0);PulseRelayTests.close(h,PulseNetworks.occupied(orb),0);
            ((com.mcmagic.omnira.block.entity.ManaEngineAccess)h.getBlockEntity(p.west(2))).engineState().control(0);
        });
        h.runAfterDelay(75,()->{
            PulseRelayTests.close(h,PulseNetworks.available(orb),256);PulseRelayTests.close(h,PulseNetworks.occupied(orb),256);
            h.setBlock(p.west(),net.minecraft.world.level.block.Blocks.AIR);
        });
        h.runAfterDelay(80,()->{
            PulseRelayTests.close(h,PulseNetworks.available(orb),0);PulseRelayTests.close(h,PulseNetworks.occupied(orb),0);
            PulseRelayTests.close(h,PulseNetworks.sourceOccupied(h.getLevel(),h.absolutePos(p.west(2)),Direction.EAST),0);h.succeed();
        });
    }
    static void relayTransit(GameTestHelper h){
        var a=new BlockPos(5,4,6);var b=a.east(2);
        h.setBlock(a,PulseContent.ORB.get());h.setBlock(a.east(),AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        h.setBlock(b,PulseContent.ORB.get().defaultBlockState().setValue(PulseOrbBlock.MOUNT,Direction.EAST));
        PulseRelayTests.engine(h,a.north(),Direction.SOUTH);
        var first=PulseOrbEntity.from(h.getBlockEntity(a));var second=PulseOrbEntity.from(h.getBlockEntity(b));
        h.runAfterDelay(30,()->{
            // Idle quotas are shares of one 512 MPU source, never two full copies.
            PulseRelayTests.close(h,PulseNetworks.available(first)+PulseNetworks.available(second),512);
            h.setBlock(b.south(),com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(3).get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        });
        h.runAfterDelay(55,()->{
            PulseRelayTests.close(h,PulseNetworks.occupied(first),256);PulseRelayTests.close(h,PulseNetworks.occupied(second),256);
            PulseRelayTests.close(h,PulseNetworks.sourceOccupied(h.getLevel(),h.absolutePos(a.north()),Direction.SOUTH),256);
            var shaft=(KineticBlockEntity)h.getBlockEntity(a.east());h.assertTrue(!shaft.isOverStressed(),"Transit was debited twice");
            h.succeed();
        });
    }
    private PulseRelayCreateChecks(){}
}
