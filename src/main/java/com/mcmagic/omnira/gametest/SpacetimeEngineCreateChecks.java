package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.engine.*;
import com.mcmagic.omnira.compat.create.SpacetimeEngineKinetics;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class SpacetimeEngineCreateChecks {
    static void fixedMachines(GameTestHelper h,SpacetimeEngineController e){
        var machines=new java.util.ArrayList<KineticBlockEntity>();
        for(int side=0;side<3;side++){
            var face=SpacetimeEngineBlock.OUTPUTS[side];var pos=e.getBlockPos().below().relative(face,2);
            var block=side==2?com.mcmagic.omnira.registry.ModBlocks.ARCANE_ASSEMBLY_TABLE.get():com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(side==0?3:5).get();
            h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,face));
            machines.add((KineticBlockEntity)h.getLevel().getBlockEntity(pos));
        }
        h.runAfterDelay(25,()->{
            checkFixedMachines(h,machines,16);
            ((com.mcmagic.omnira.compat.create.KineticCrystalWorkstationEntity)machines.get(0)).workstation().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON));
            var anvil=((com.mcmagic.omnira.compat.create.KineticCrystalWorkstationEntity)machines.get(1)).workstation();
            anvil.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));anvil.rename="Idle load test";
            for(int side=0;side<3;side++)e.configure(side,256,37,false);
        });
        h.runAfterDelay(55,()->{
            checkFixedMachines(h,machines,37);
            for(int side=0;side<2;side++)h.assertTrue(((com.mcmagic.omnira.compat.create.KineticCrystalWorkstationEntity)machines.get(side)).workstation().progress>0,"Working machine failed to process");
            e.configure(0,256,0,false);
        });
        h.runAfterDelay(80,()->{
            h.assertTrue(machines.get(0).getSpeed()==0,"Stopped source still rotates consumer");
            h.assertTrue(Math.abs(machines.get(1).getSpeed())==37&&Math.abs(machines.get(2).getSpeed())==37,"Stopping one output stopped other sources");
            h.succeed();
        });
    }
    private static void checkFixedMachines(GameTestHelper h,java.util.List<KineticBlockEntity> machines,float rpm){
        for(var machine:machines){
            h.assertTrue(!machine.isOverStressed()&&Math.abs(machine.getSpeed())==rpm,"Fixed machine stopped or overloaded");
            h.assertTrue(Math.abs(machine.getOrCreateNetwork().getActualStressOf(machine)-256)<.001,"Fixed machine idle/working occupancy is not 256 SU");
        }
    }
    static void idleAnvil(GameTestHelper h,SpacetimeEngineController e){
        var l=h.getLevel();var target=h.absolutePos(new net.minecraft.core.BlockPos(10,3,10));
        l.setBlockAndUpdate(target,com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(5).get().defaultBlockState());
        var machine=(com.mcmagic.omnira.compat.create.KineticCrystalWorkstationEntity)l.getBlockEntity(target);
        var from=port(h,e.getBlockPos().below().east(),net.minecraft.core.Direction.EAST);
        var to=port(h,target,net.minecraft.core.Direction.SOUTH);
        h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(from,to),"Anvil remote pair failed");
        h.runAfterDelay(30,()->{
            checkIdleAnvil(h,machine,16);
            checkOtherOutputs(h,e,machine);
            var net=machine.getOrCreateNetwork();
            // A missing, connected endpoint represents a real unloaded reservation.
            var endpoint=(KineticBlockEntity)to.host;var coefficient=net.members.remove(endpoint);
            net.initFromTE(0,128,1);
            com.mcmagic.omnira.compat.create.OmniraKineticAccounting.repairClosedNetwork(net);
            h.assertTrue(Math.abs(net.calculateStress()-384)<.001,"Discarded unverified unloaded stress");
            net.members.put(endpoint,coefficient);
            // Reproduce TEST's four-node line saved as 36 nodes and 4352 SU.
            net.initFromTE(0,4096,32);
            h.assertTrue(machine.isOverStressed(),"Fixture did not reproduce phantom overload");
            e.configure(1,512,128,false);
        });
        h.runAfterDelay(55,()->{checkIdleAnvil(h,machine,128);e.configure(1,256,32,false);});
        h.runAfterDelay(60,()->e.configure(1,128,32,false));
        h.runAfterDelay(70,()->{
            h.assertTrue(machine.isOverStressed()&&machine.getSpeed()==0,"Insufficient capacity must still overload");
            checkOtherOutputs(h,e,machine);e.configure(1,256,32,false);
        });
        h.runAfterDelay(80,()->{checkIdleAnvil(h,machine,32);from.unlink();});
        h.runAfterDelay(100,()->{h.assertTrue(machine.getSpeed()==0,"Disconnected anvil rotates");h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(from,to),"Anvil reconnect failed");});
        h.runAfterDelay(130,()->{
            checkIdleAnvil(h,machine,32);
            checkOtherOutputs(h,e,machine);
            var net=machine.getOrCreateNetwork();
            h.assertTrue(net.getSize()==4,"Phantom members remain");
            net.updateStressFor(machine,100);
            machine.tick();checkIdleAnvil(h,machine,32);
            net.initFromTE(0,0,32);
            com.mcmagic.omnira.compat.create.OmniraKineticAccounting.repairClosedNetwork(net);
            for(var member:net.members.keySet())h.assertTrue(member.saveWithFullMetadata(l.registryAccess()).getCompound("Network").getInt("Size")==4,"Saved phantom size would recur after reload");
            var saved=machine.saveWithFullMetadata(l.registryAccess());
            var restored=new com.mcmagic.omnira.compat.create.KineticCrystalWorkstationEntity(target,machine.getBlockState());
            restored.setLevel(l);restored.loadWithComponents(saved,l.registryAccess());
            h.assertTrue(Math.abs(restored.calculateStressApplied()*Math.abs(restored.getTheoreticalSpeed())-256)<.001,"Reload lost fixed occupancy");
            var output=(SpacetimeEngineKinetics.Output)from.host.getLevel().getBlockEntity(from.attached());
            output.setSpeed(128);
            h.assertTrue(Math.abs(output.calculateAddedStressCapacity()*Math.abs(output.getGeneratedSpeed())-256)<.001,"Driven RPM reduced source capacity");
            h.succeed();
        });
    }
    private static void checkOtherOutputs(GameTestHelper h,SpacetimeEngineController e,KineticBlockEntity machine){
        var ids=new java.util.HashSet<Long>();
        for(int side=0;side<4;side++){
            var output=(SpacetimeEngineKinetics.Output)h.getLevel().getBlockEntity(e.getBlockPos().below().relative(SpacetimeEngineBlock.OUTPUTS[side]));
            var net=output.getOrCreateNetwork();
            h.assertTrue(ids.add(net.id)&&net.sources.size()==1&&net.sources.containsKey(output),"Outputs are not four independent sources");
            h.assertTrue((side==1)==java.util.Objects.equals(output.network,machine.network),"Anvil connected to wrong source");
            if(side!=1)h.assertTrue(net.calculateStress()==0&&!output.isOverStressed()&&Math.abs(output.getSpeed())==16,"Anvil load leaked to another output");
        }
    }
    private static void checkIdleAnvil(GameTestHelper h,com.mcmagic.omnira.compat.create.KineticCrystalWorkstationEntity machine,float rpm){
        var net=machine.getOrCreateNetwork();
        h.assertTrue(!machine.isOverStressed()&&Math.abs(machine.getSpeed())==rpm,"Idle anvil overloaded: speed="+machine.getTheoreticalSpeed()+", stress="+net.calculateStress()+", capacity="+net.calculateCapacity());
        h.assertTrue(Math.abs(net.getActualStressOf(machine)-256)<.001,"Idle anvil lost fixed occupancy");
        h.assertTrue(Math.abs(machine.workstation().rpm)==rpm,"Idle shaft animation stopped");
    }
    static void remote(GameTestHelper h,SpacetimeEngineController e){
        var l=h.getLevel();var from=e.getBlockPos().below().east();var target=h.absolutePos(new net.minecraft.core.BlockPos(10,5,10));
        e.configure(1,2048,64,false);
        l.setBlockAndUpdate(target,com.simibubi.create.AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,net.minecraft.core.Direction.Axis.Y));
        var a=port(h,from,net.minecraft.core.Direction.EAST);var b=port(h,target,net.minecraft.core.Direction.DOWN);
        h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(a,b),"Remote pair failed");
        h.runAfterDelay(25,()->{
            var shaft=(KineticBlockEntity)l.getBlockEntity(target);
            h.assertTrue(Math.abs(shaft.getSpeed())==64,"Perpendicular remote output failed");
            h.assertTrue(Math.abs(shaft.getOrCreateNetwork().calculateCapacity()-2048)<.01,"Remote capacity changed");
            b.unlink();h.runAfterDelay(20,()->{h.assertTrue(shaft.getSpeed()==0,"Unlinked shaft still rotating");h.succeed();});
        });
    }
    private static com.mcmagic.omnira.space.SpacetimePortState port(GameTestHelper h,net.minecraft.core.BlockPos source,net.minecraft.core.Direction face){
        var at=source.relative(face);h.getLevel().setBlockAndUpdate(at,com.mcmagic.omnira.registry.ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(com.mcmagic.omnira.space.SpacetimePortBlock.FACING,face));
        return com.mcmagic.omnira.space.SpacetimePortState.at(h.getLevel(),at);
    }
    static void run(GameTestHelper h,SpacetimeEngineController e){
        var root=e.getBlockPos().below();
        for(int i=0;i<4;i++){
            var d=SpacetimeEngineBlock.OUTPUTS[i];e.configure(i,2048,16<<i,i%2==1);
            h.getLevel().setBlockAndUpdate(root.relative(d,2),com.simibubi.create.AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,d.getAxis()));
            var s=h.getLevel().getBlockState(root.relative(d));var block=(IRotate)s.getBlock();
            for(var face:net.minecraft.core.Direction.values())h.assertTrue(block.hasShaftTowards(h.getLevel(),root.relative(d),s,face)==(face==d),"Extra shaft exposed");
        }
        h.runAfterDelay(25,()->{
            check(h,e);
            for(int i=0;i<4;i++)e.configure(i,2048,128>>i,i%2==0);
            h.runAfterDelay(25,()->{check(h,e);e.grids.setStackInSlot(0,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.OMNI_CRYSTAL_GRID.get()));
                e.configure(0,8192,128,false);
                h.runAfterDelay(25,()->{check(h,e);e.grids.setStackInSlot(0,net.minecraft.world.item.ItemStack.EMPTY);
                    h.runAfterDelay(25,()->{check(h,e);h.succeed();});});});
        });
    }
    private static void check(GameTestHelper h,SpacetimeEngineController e){
        var root=e.getBlockPos().below();var networks=new java.util.HashSet<Long>();
        for(int i=0;i<4;i++){
            var d=SpacetimeEngineBlock.OUTPUTS[i];var output=(SpacetimeEngineKinetics.Output)h.getLevel().getBlockEntity(root.relative(d));
            var shaft=(KineticBlockEntity)h.getLevel().getBlockEntity(root.relative(d,2));
            h.assertTrue(shaft.getTheoreticalSpeed()==e.generatedSpeed(i),"Wrong speed for "+d+": "+shaft.getTheoreticalSpeed());
            float total=output.calculateAddedStressCapacity()*Math.abs(output.getTheoreticalSpeed());
            h.assertTrue(total<=e.capacity(i)&&e.capacity(i)-total<.01,"RPM multiplied capacity: "+total);
            h.assertTrue(Math.abs(output.getOrCreateNetwork().calculateCapacity()-e.capacity(i))<.01,"Network capacity stale");
            h.assertTrue(networks.add(output.getOrCreateNetwork().id),"Outputs linked internally");
        }
    }
    private SpacetimeEngineCreateChecks(){}
}
