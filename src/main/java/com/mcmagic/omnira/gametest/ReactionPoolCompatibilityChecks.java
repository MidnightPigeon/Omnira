package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Optional Create checks must not be resolved when NeoForge scans test holders. */
final class ReactionPoolCompatibilityChecks {
    static void independentRpm(GameTestHelper h){
        var center=h.absolutePos(new net.minecraft.core.BlockPos(6,3,5));
        h.assertTrue(ReactionPoolBlock.place(h.getLevel(),center,net.minecraft.core.Direction.NORTH),"Pool placement failed");
        var pool=ReactionPoolBlock.controller(h.getLevel(),center);
        var ports=new java.util.ArrayList<com.mcmagic.omnira.compat.CreateReactionPool.Port>();
        var motors=new java.util.ArrayList<com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity>();
        for(int part:new int[]{3,5}){
            var port=center.offset(ReactionPoolBlock.offset(net.minecraft.core.Direction.NORTH,part));
            var face=ReactionPoolBlock.outward(net.minecraft.core.Direction.NORTH,part);var at=port.relative(face);
            h.getLevel().setBlockAndUpdate(at,com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,face.getOpposite()));
            var motor=(com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)h.getLevel().getBlockEntity(at);
            motor.generatedSpeed.setValue(16);motors.add(motor);ports.add((com.mcmagic.omnira.compat.CreateReactionPool.Port)h.getLevel().getBlockEntity(port));
        }
        int[][] rpm={{16,16},{32,16},{8,32},{16,-32},{128,128},{256,256},{0,16}};
        double[] factors={1,1.5,1,1.5,8,8,0};
        for(int i=0;i<rpm.length;i++){
            final int stage=i;
            h.runAfterDelay(30*(i+1),()->{
                double sum=0;
                for(int side=0;side<2;side++){
                    var port=ports.get(side);float expected=Math.abs(rpm[stage][side])>=16?Math.abs(rpm[stage][side])*32:0;
                    h.assertTrue((expected==0||port.hasNetwork())&&!port.isOverStressed(),"Independent motor lost its network");
                    float actual=port.hasNetwork()?port.getOrCreateNetwork().getActualStressOf(port):0;
                    h.assertTrue(Math.abs(actual-expected)<.01,"Per-side SU mismatch at stage "+stage+"/"+side);
                    h.assertTrue(port.isSpeedRequirementFulfilled()==(expected>0),"Per-side RPM threshold mismatch");sum+=expected;
                }
                h.assertTrue(Math.abs(com.mcmagic.omnira.compat.CreateReactionPool.power(pool)-sum/1024)<.01,"Independent SU did not sum");
                ReactionPoolBlockEntity.tick(h.getLevel(),pool.getBlockPos(),pool.getBlockState(),pool);
                h.assertTrue(Math.abs(pool.efficiency()-factors[stage])<.01&&pool.powered()==(factors[stage]>=1),"Mechanical efficiency or cap mismatch: "+stage+"/"+pool.efficiency());
                if(stage+1<rpm.length)for(int side=0;side<2;side++)motors.get(side).generatedSpeed.setValue(rpm[stage+1][side]);
                else h.succeed();
            });
        }
    }
    static void typedCreateFilters(GameTestHelper h){
        var registry=h.getLevel().registryAccess();
        for(int mode=0;mode<3;mode++){
            var nested=com.simibubi.create.AllItems.FILTER.asStack();
            var samples=mode==0?java.util.List.of(new ItemStack(net.minecraft.world.item.Items.STONE))
                    :mode==1?java.util.List.of(new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET))
                    :java.util.List.of(new ItemStack(net.minecraft.world.item.Items.STONE),new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
            nested.set(com.simibubi.create.AllDataComponents.FILTER_ITEMS,net.minecraft.world.item.component.ItemContainerContents.fromItems(samples));
            nested.set(com.simibubi.create.AllDataComponents.FILTER_ITEMS_BLACKLIST,false);
            var outer=com.simibubi.create.AllItems.FILTER.asStack();
            outer.set(com.simibubi.create.AllDataComponents.FILTER_ITEMS,net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(nested)));
            outer.set(com.simibubi.create.AllDataComponents.FILTER_ITEMS_BLACKLIST,false);
            var plate=new ItemStack(ModItems.OUTPUT_UPGRADE.get());
            com.mcmagic.omnira.block.entity.OrbUpgrades.setFilter(plate,0,outer,registry);
            var stored=ItemStack.parseOptional(registry,com.mcmagic.omnira.block.entity.OrbUpgrades.settings(plate).getCompound("Filter0"));
            int expected=mode==0?1:mode==1?2:3;
            h.assertTrue(com.mcmagic.omnira.block.entity.OrbFilterMatcher.domains(h.getLevel(),stored)==expected,"Nested Create whitelist domain mismatch: "+mode);
            h.assertTrue(com.mcmagic.omnira.block.entity.OrbFilterMatcher.allowsTyped(h.getLevel(),new ItemStack[]{stored},true,new ItemStack(net.minecraft.world.item.Items.DIAMOND),null)==(mode==1),"Nested Create whitelist restricted wrong item domain");
            h.assertTrue(com.mcmagic.omnira.block.entity.OrbFilterMatcher.allowsTyped(h.getLevel(),new ItemStack[]{stored},true,ItemStack.EMPTY,new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,100))==(mode==0),"Nested Create whitelist restricted wrong fluid domain");
        }
        h.succeed();
    }
    static void verifyRemote(GameTestHelper h){
        var l=h.getLevel();var center=h.absolutePos(new net.minecraft.core.BlockPos(6,3,5));
        h.assertTrue(ReactionPoolBlock.place(l,center,net.minecraft.core.Direction.NORTH),"Pool placement failed");
        var pool=ReactionPoolBlock.controller(l,center);
        pool.manualFluid.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,10000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        pool.items.setStackInSlot(0,new ItemStack(net.minecraft.world.item.Items.GLASS,32));
        for(int part:new int[]{6,8})l.setBlockAndUpdate(center.offset(ReactionPoolBlock.offset(net.minecraft.core.Direction.NORTH,part)).north(),
                com.mcmagic.omnira.storage.CrystalStorageContent.VOID_ORB.get().defaultBlockState());
        var peers=new java.util.ArrayList<com.mcmagic.omnira.space.SpacetimePortState>();
        var ports=new java.util.ArrayList<com.mcmagic.omnira.compat.CreateReactionPool.Port>();
        for(int i=0;i<2;i++){
            int part=i==0?3:5;var face=ReactionPoolBlock.outward(net.minecraft.core.Direction.NORTH,part);
            var target=center.offset(ReactionPoolBlock.offset(net.minecraft.core.Direction.NORTH,part));
            var enginePos=h.absolutePos(new net.minecraft.core.BlockPos(i==0?2:10,3,11));
            var output=i==0?net.minecraft.core.Direction.EAST:net.minecraft.core.Direction.NORTH;
            l.setBlockAndUpdate(enginePos,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,output));
            var engine=((com.mcmagic.omnira.block.entity.ManaEngineAccess)l.getBlockEntity(enginePos)).engineState();
            engine.inventory.setItem(0,new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
            var from=connector(h,enginePos,output);var to=connector(h,target,face);
            h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(from,to),"Pair failed");
            peers.add(from);peers.add(to);ports.add((com.mcmagic.omnira.compat.CreateReactionPool.Port)l.getBlockEntity(target));
        }
        h.runAfterDelay(35,()->{
            for(var port:ports)h.assertTrue(port.hasNetwork()&&!port.isOverStressed()&&Math.abs(port.getSpeed())==16,"Remote engine failed");
            h.assertTrue(Math.abs(com.mcmagic.omnira.compat.CreateReactionPool.power(pool)-1)<.01&&pool.efficiency()==2,"Per-side external load and native acceleration diverged");
            h.assertTrue(pool.fluids.total()<10000&&pool.items.getStackInSlot(0).getCount()<32,"Powered pool did not push both outputs into void orbs");
            var saved=ports.get(0).saveWithFullMetadata(l.registryAccess());
            var restored=new com.mcmagic.omnira.compat.CreateReactionPool.Port(ports.get(0).getBlockPos(),ports.get(0).getBlockState());
            restored.setLevel(l);restored.loadWithComponents(saved,l.registryAccess());
            h.assertTrue(Math.abs(restored.calculateStressApplied()*Math.abs(restored.getTheoreticalSpeed())-512)<.01,"Reload lost RPM-based pool stress");
            peers.get(0).unlink();
        });
        h.runAfterDelay(50,()->h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(peers.get(0),peers.get(1)),"Reconnection failed"));
        h.runAfterDelay(75,()->{
            var left=ports.get(0);var right=ports.get(1);
            h.assertTrue(!left.isOverStressed()&&!right.isOverStressed(),"Reconnect left pool overloaded");
            var net=left.getOrCreateNetwork();
            // Each side consumes its own RPM-scaled share, even on separate remote networks.
            left.setSpeed(32);left.tick();
            h.assertTrue(net.calculateStress()<=net.calculateCapacity()&&!left.isOverStressed(),"Speed change retained old stress coefficient");
            h.assertTrue(Math.abs(net.getActualStressOf(left)-1024)<.01&&Math.abs(right.getOrCreateNetwork().getActualStressOf(right)-512)<.01,"RPM-scaled occupancy changed the other side");
            left.setSpeed(64);left.tick();
            h.assertTrue(left.isOverStressed(),"Insufficient network capacity did not overstress the faster input");
            left.setSpeed(16);left.tick();
            // Create retains unloaded members' stress until they return. Reserve their share.
            var unloaded=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)peers.get(0).host;
            var unloadedCoefficient=net.members.remove(unloaded);
            net.initFromTE(0,128,1);left.tick();
            h.assertTrue(net.calculateStress()<=net.calculateCapacity()&&!left.isOverStressed(),"Ignored unloaded network stress");
            h.assertTrue(Math.abs(net.getActualStressOf(left)-512)<.01,"Unloaded members changed RPM-based occupancy");
            net.members.put(unloaded,unloadedCoefficient);
            net.initFromTE(0,0,0);left.tick();
            h.assertTrue(Math.abs(net.getActualStressOf(left)-512)<.01,"Occupancy changed without RPM change");
            net.initFromTE(0,1024,0);left.tick();
            h.assertTrue(Math.abs(net.getActualStressOf(left)-512)<.01&&!left.isOverStressed(),"Stale unloaded reservation blocked fully loaded pool");
            net.initFromTE(0,1024,11);left.tick();
            h.assertTrue(Math.abs(net.getActualStressOf(left)-512)<.01&&net.getSize()==4,"Legacy closed remote drive kept phantom stress or members");
            h.succeed();
        });
    }
    private static com.mcmagic.omnira.space.SpacetimePortState connector(GameTestHelper h,net.minecraft.core.BlockPos block,net.minecraft.core.Direction face){
        var p=block.relative(face);
        h.getLevel().setBlockAndUpdate(p,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(com.mcmagic.omnira.space.SpacetimePortBlock.FACING,face));
        return com.mcmagic.omnira.space.SpacetimePortState.at(h.getLevel(),p);
    }
    static void verify(GameTestHelper h,ReactionPoolBlockEntity pool){
        var facing=pool.getBlockState().getValue(ReactionPoolBlock.FACING);
        var portPos=pool.getBlockPos().offset(ReactionPoolBlock.offset(facing,3));var side=ReactionPoolBlock.outward(facing,3);
        var enginePos=portPos.relative(side);
        h.getLevel().setBlockAndUpdate(enginePos,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,side.getOpposite()));
        var engine=((com.mcmagic.omnira.block.entity.ManaEngineAccess)h.getLevel().getBlockEntity(enginePos)).engineState();
        engine.inventory.setItem(0,new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
        h.runAfterDelay(40,()->{
            var left=(com.mcmagic.omnira.compat.CreateReactionPool.Port)h.getLevel().getBlockEntity(portPos);
            h.assertTrue(left.hasNetwork()&&Math.abs(left.getSpeed())>0,"Shaft failed to connect to engine");
            ReactionPoolBlockEntity.tick(h.getLevel(),pool.getBlockPos(),pool.getBlockState(),pool);
            h.assertTrue(Math.abs(com.mcmagic.omnira.compat.CreateReactionPool.power(pool)-.5)<.01,"Single 16 RPM input did not provide half the base requirement");
            h.assertTrue(Math.abs(left.getOrCreateNetwork().getActualStressOf(left)-512)<.01,"Single 16 RPM input did not reserve 512 SU");
        });
        for(int i=41;i<=60;i++)h.runAfterDelay(i,()->pool.energyInput.receiveEnergy(Integer.MAX_VALUE,false));
        h.runAfterDelay(61,()->{
            var left=(com.mcmagic.omnira.compat.CreateReactionPool.Port)h.getLevel().getBlockEntity(portPos);
            int before=pool.energyInput.getEnergyStored();
            double expected=pool.electricalEfficiency();
            ReactionPoolBlockEntity.tick(h.getLevel(),pool.getBlockPos(),pool.getBlockState(),pool);
            h.assertTrue(pool.efficiency()==1&&pool.energyInput.getEnergyStored()==before&&pool.pulsePowered()&&!pool.electricalPowered(),"Native pulse priority consumed or accelerated with external FE");
            var right=(com.mcmagic.omnira.compat.CreateReactionPool.Port)h.getLevel().getBlockEntity(pool.getBlockPos().offset(ReactionPoolBlock.offset(facing,5)));
            right.setSpeed(16);right.setNetwork(left.network);left.getOrCreateNetwork().add(right);
            left.getOrCreateNetwork().updateStressFor(right,right.calculateStressApplied());
            left.tick();
            h.assertTrue(Math.abs(com.mcmagic.omnira.compat.CreateReactionPool.power(pool)-1)<.01,"Two 16 RPM inputs did not sum to 1024 SU: left="+left.getSpeed()+", right="+right.getSpeed()+", stress="+left.getOrCreateNetwork().calculateStress()+", capacity="+left.getOrCreateNetwork().calculateCapacity());
            h.succeed();
        });
    }
}
