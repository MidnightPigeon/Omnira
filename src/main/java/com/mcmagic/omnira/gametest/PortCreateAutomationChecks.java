package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pump.*;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.logistics.funnel.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

final class PortCreateAutomationChecks {
    static void funnel(GameTestHelper h){
        var l=h.getLevel();var a=h.absolutePos(new BlockPos(2,4,2));var b=h.absolutePos(new BlockPos(8,4,8));
        l.setBlockAndUpdate(a,Blocks.CHEST.defaultBlockState());l.setBlockAndUpdate(b,Blocks.CHEST.defaultBlockState());
        var funnel=a.east();l.setBlockAndUpdate(funnel,AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(FunnelBlock.FACING,Direction.EAST).setValue(FunnelBlock.EXTRACTING,true));
        h.assertTrue(SpacetimePortState.pair(PortAutomationGameTests.port(h,funnel,Direction.EAST),PortAutomationGameTests.port(h,b,Direction.UP)),"Funnel pair failed");
        ((ChestBlockEntity)l.getBlockEntity(a)).setItem(0,new ItemStack(Items.DIAMOND,32));
        h.runAfterDelay(100,()->{
            var f=(FunnelBlockEntity)l.getBlockEntity(funnel);
            int received=((ChestBlockEntity)l.getBlockEntity(b)).getItem(0).getCount();
            h.assertTrue(received>0,"Funnel did not output through port");
            h.assertTrue(received+((ChestBlockEntity)l.getBlockEntity(a)).getItem(0).getCount()==32,"Funnel lost or duplicated source");h.succeed();
        });
    }
    static void pipes(GameTestHelper h){
        pipes(h,true);
    }
    static void pipes(GameTestHelper h,boolean pipeBridge){
        pipes(h,pipeBridge,-1);
    }
    static void pipes(GameTestHelper h,boolean pipeBridge,int casingFamily){
        var l=h.getLevel();var source=h.absolutePos(new BlockPos(2,4,2));var pump=source.east();var pipe=pump.east();
        var remote=h.absolutePos(new BlockPos(9,4,9));var target=remote.above();
        l.setBlockAndUpdate(source,AllBlocks.FLUID_TANK.getDefaultState());l.setBlockAndUpdate(target,AllBlocks.FLUID_TANK.getDefaultState());
        l.setBlockAndUpdate(pump,AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING,Direction.EAST));
        var horizontal=AllBlocks.FLUID_PIPE.getDefaultState().setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST),true).setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST),true);
        var vertical=AllBlocks.FLUID_PIPE.getDefaultState().setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.UP),true).setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.DOWN),true);
        l.setBlockAndUpdate(pipe,horizontal);if(pipeBridge)l.setBlockAndUpdate(remote,vertical);
        if(casingFamily>=0){
            var block=casingFamily==0?com.mcmagic.omnira.compat.CrystalEncasing.PIPE.get():com.mcmagic.omnira.compat.CrystalEncasing.INFUSED_PIPE.get();
            var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var shell=new ItemStack(block.getCasing());
            for(var at:new BlockPos[]{pipe,remote}){
                // Freeze the intended two-ended route before encasing; native open-air
                // placement can otherwise leave six outlets and spray the test fluid.
                var route=AllBlocks.FLUID_PIPE.getDefaultState();
                for(var direction:Direction.values())route=route.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                        direction.getAxis()==(at.equals(pipe)?Direction.Axis.X:Direction.Axis.Y));
                l.setBlock(at,route,2);
                ((com.simibubi.create.content.decoration.encasing.EncasableBlock)l.getBlockState(at).getBlock()).tryEncase(
                        l.getBlockState(at),l,at,shell,player,net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at),Direction.UP,at,false));
            }
        }
        h.assertTrue(SpacetimePortState.pair(PortAutomationGameTests.port(h,pipe,Direction.EAST),PortAutomationGameTests.port(h,pipeBridge?remote:target,Direction.DOWN)),"Pipe pair failed");
        l.setBlockAndUpdate(pump.north(),AllBlocks.COGWHEEL.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,Direction.Axis.X));
        l.setBlockAndUpdate(pump.north().east(),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.WEST));
        var motor=(com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)l.getBlockEntity(pump.north().east());
        motor.generatedSpeed.setValue(-64);
        var drive=(PumpBlockEntity)l.getBlockEntity(pump);
        h.runAfterDelay(15,()->{
            var input=l.getCapability(Capabilities.FluidHandler.BLOCK,source,Direction.EAST);
            h.assertTrue(input!=null&&input.fill(new FluidStack(Fluids.LAVA,4000),FluidAction.EXECUTE)==4000,"Tank fixture failed");
            drive.updatePressureChange();
        });
        h.runAfterDelay(150,()->{
            var out=l.getCapability(Capabilities.FluidHandler.BLOCK,target,Direction.DOWN);
            int amount=out==null?0:out.getFluidInTank(0).getAmount();
            var localFlow=com.simibubi.create.content.fluids.FluidPropagator.getPipe(l,pipe);
            var remoteFlow=com.simibubi.create.content.fluids.FluidPropagator.getPipe(l,remote);
            h.assertTrue(amount>500,"Pump amount="+amount+" speed="+drive.getSpeed()+" local="+l.getBlockState(pipe)+" remote="+l.getBlockState(remote)+" pressure="+localFlow.hasAnyPressure()+"/"+(remoteFlow!=null&&remoteFlow.hasAnyPressure())+" connection="+new com.mcmagic.omnira.compat.PortFluidFace(l,pipe,Direction.EAST).getConnectedPos());
            SpacetimePortState.at(l,pipe.east()).unlink();
            h.runAfterDelay(15,()->{
                int stopped=out.getFluidInTank(0).getAmount();
                h.runAfterDelay(20,()->{h.assertTrue(out.getFluidInTank(0).getAmount()==stopped,"Unlinked network kept transferring");h.succeed();});
            });
        });
    }
}
