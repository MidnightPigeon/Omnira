package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.compat.create.*;
import com.mcmagic.omnira.workstation.*;
import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class CrystalShaftCreateChecks {
    private static CreativeMotorBlockEntity motor(GameTestHelper h,BlockPos pos,Direction output,int rpm){
        h.setBlock(pos,AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING,output));
        var motor=(CreativeMotorBlockEntity)h.getBlockEntity(pos);motor.generatedSpeed.setValue(rpm);return motor;
    }
    static void boundary(GameTestHelper h){
        for(float rpm:new float[]{0,1,8,15.999F,-8,-15.999F,Float.NaN,Float.POSITIVE_INFINITY})
            h.assertTrue(!CrystalShaftInput.sufficient(rpm),"Accepted invalid RPM "+rpm);
        for(float rpm:new float[]{16,-16,32,-256})h.assertTrue(CrystalShaftInput.sufficient(rpm),"Rejected valid RPM "+rpm);
        int x=2;
        for(var block:new net.minecraft.world.level.block.Block[]{CrystalWorkstations.BLOCKS.get(3).get(),CrystalWorkstations.BLOCKS.get(5).get(),ModBlocks.ARCANE_ASSEMBLY_TABLE.get()}){
            var pos=new BlockPos(x,3,4);x+=3;h.setBlock(pos,block);
            var machine=(KineticBlockEntity)h.getBlockEntity(pos);
            for(float rpm:new float[]{8,-8,16,-16}){
                machine.setSpeed(rpm);boolean valid=Math.abs(rpm)>=16;
                h.assertTrue(machine.isSpeedRequirementFulfilled()==valid,"Native requirement mismatch "+block+"/"+rpm);
                h.assertTrue((machine.calculateStressApplied()>0)==valid,"Low RPM consumed stress "+block);
            }
        }h.succeed();
    }
    static void machine(GameTestHelper h){
        var pos=new BlockPos(5,4,5);h.setBlock(pos,CrystalWorkstations.BLOCKS.get(3).get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
        var motor=motor(h,pos.south(),Direction.NORTH,8);var host=(KineticBlockEntity)h.getBlockEntity(pos);
        var work=CrystalWorkstations.work(host);
        h.runAfterDelay(25,()->{
            h.assertTrue(!work.mechanical&&!host.isSpeedRequirementFulfilled(),"8 RPM powered a machine");
            PulseRelayTests.close(h,motor.getOrCreateNetwork().calculateStress(),0);motor.generatedSpeed.setValue(-16);
        });
        h.runAfterDelay(55,()->{
            h.assertTrue(work.mechanical&&host.isSpeedRequirementFulfilled(),"-16 RPM did not restart machine");
            PulseRelayTests.close(h,motor.getOrCreateNetwork().calculateStress(),256);motor.generatedSpeed.setValue(-8);
        });
        h.runAfterDelay(85,()->{
            h.assertTrue(!work.mechanical,"Dropping below 16 RPM kept machine running");
            PulseRelayTests.close(h,motor.getOrCreateNetwork().calculateStress(),0);h.succeed();
        });
    }
    static void orbs(GameTestHelper h){
        var pos=new BlockPos(5,4,5);h.setBlock(pos,PulseContent.RESONANT.get().defaultBlockState().setValue(PulseOrbBlock.MOUNT,Direction.EAST));
        var motor=motor(h,pos.west(),Direction.EAST,8);var orb=PulseOrbEntity.from(h.getBlockEntity(pos));
        h.runAfterDelay(25,()->{
            PulseRelayTests.close(h,PulseNetworks.available(orb),0);
            h.assertTrue(!((KineticBlockEntity)orb.host).isSpeedRequirementFulfilled(),"Relay native warning ignored its isolated inlet");
            h.assertTrue(PulseOrbShaftState.mode(orb.shaftState,Direction.WEST)==PulseOutput.ROTATION,"Slow physical shaft lost rotation mode");
            motor.generatedSpeed.setValue(16);
        });
        h.runAfterDelay(55,()->{
            h.assertTrue(PulseNetworks.available(orb)>0,"16 RPM import unavailable");
            h.assertTrue(orb.conversion.reserve(PulseConversions.KINETIC,256,false)==256,"Valid relay import failed");motor.generatedSpeed.setValue(8);
        });
        h.runAfterDelay(85,()->{
            PulseRelayTests.close(h,PulseNetworks.available(orb),0);PulseRelayTests.close(h,PulseNetworks.occupied(orb),0);
            PulseRelayTests.close(h,motor.getOrCreateNetwork().calculateStress(),0);h.succeed();
        });
    }
    static void pool(GameTestHelper h){
        var root=h.absolutePos(new BlockPos(6,4,6));
        h.assertTrue(ReactionPoolBlock.place(h.getLevel(),root,Direction.NORTH),"Pool fixture failed");
        var pool=ReactionPoolBlock.controller(h.getLevel(),root);
        for(int part:new int[]{3,5}){
            var port=root.offset(ReactionPoolBlock.offset(Direction.NORTH,part));var face=ReactionPoolBlock.outward(Direction.NORTH,part);
            var at=port.relative(face);
            h.getLevel().setBlockAndUpdate(at,AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING,face.getOpposite()));
            var motor=(CreativeMotorBlockEntity)h.getLevel().getBlockEntity(at);motor.generatedSpeed.setValue(part==3?8:16);
        }
        h.runAfterDelay(30,()->{
            double power=com.mcmagic.omnira.compat.CreateReactionPool.power(pool);
            String diagnostic="complete="+pool.complete();
            for(int part:new int[]{3,5}){
                var at=root.offset(ReactionPoolBlock.offset(Direction.NORTH,part));var port=(KineticBlockEntity)h.getLevel().getBlockEntity(at);
                diagnostic+=" port"+part+"="+port.getSpeed()+"/"+port.getTheoreticalSpeed()+"/"+port.calculateStressApplied()+"/"+port.hasNetwork();
                var face=ReactionPoolBlock.outward(Direction.NORTH,part);var drive=(KineticBlockEntity)h.getLevel().getBlockEntity(at.relative(face));
                diagnostic+=" motor="+(drive==null?"missing":drive.getSpeed()+"/"+drive.getBlockState());
            }
            h.assertTrue(Math.abs(power-.5)<.01,"Pool power="+power+" "+diagnostic);
            for(int part:new int[]{3,5}){
                var at=root.offset(ReactionPoolBlock.offset(Direction.NORTH,part));var port=(KineticBlockEntity)h.getLevel().getBlockEntity(at);
                h.assertTrue(port.isSpeedRequirementFulfilled()==(part==5),"Pool port threshold mismatch");
                PulseRelayTests.close(h,port.getOrCreateNetwork().calculateStress(),part==5?512:0);
            }h.succeed();
        });
    }
    private CrystalShaftCreateChecks(){}
}
