package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Optional Create checks must not be resolved when NeoForge scans test holders. */
final class ReactionPoolCompatibilityChecks {
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
            engine.redstoneControl=false;engine.inventory.setItem(0,new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
            var from=connector(h,enginePos,output);var to=connector(h,target,face);
            h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(from,to),"Pair failed");
            peers.add(from);peers.add(to);ports.add((com.mcmagic.omnira.compat.CreateReactionPool.Port)l.getBlockEntity(target));
        }
        h.runAfterDelay(35,()->{
            for(var port:ports)h.assertTrue(port.hasNetwork()&&!port.isOverStressed()&&Math.abs(port.getSpeed())==16,"Remote engine failed");
            h.assertTrue(Math.abs(com.mcmagic.omnira.compat.CreateReactionPool.power(pool)-2)<.01,"Two 16 RPM shafts should supply two base rates");
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
            // Emulate a speed change without a capacity change: old per-rpm demand must be refreshed.
            left.setSpeed(32);left.tick();
            h.assertTrue(net.calculateStress()<=net.calculateCapacity()&&!left.isOverStressed(),"Speed change retained old stress coefficient");
            h.assertTrue(Math.abs(net.getActualStressOf(left)-1024)<.01,"Doubling RPM did not double occupancy");
            left.setSpeed(64);left.tick();
            h.assertTrue(left.isOverStressed(),"Insufficient stress capacity silently reduced the requested occupancy");
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
        engine.redstoneControl=false;engine.inventory.setItem(0,new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
        h.runAfterDelay(40,()->{
            var left=(com.mcmagic.omnira.compat.CreateReactionPool.Port)h.getLevel().getBlockEntity(portPos);
            h.assertTrue(left.hasNetwork()&&Math.abs(left.getSpeed())>0,"Shaft failed to connect to engine");
            ReactionPoolBlockEntity.tick(h.getLevel(),pool.getBlockPos(),pool.getBlockState(),pool);
            h.assertTrue(Math.abs(com.mcmagic.omnira.compat.CreateReactionPool.power(pool)-1)<.01,"16 RPM did not give base mechanical efficiency: rpm="+left.getSpeed()+", factor="+pool.efficiency());
            h.assertTrue(Math.abs(left.getOrCreateNetwork().getActualStressOf(left)-512)<.01,"Pool occupancy not based on RPM");
        });
        for(int i=41;i<=60;i++)h.runAfterDelay(i,()->pool.energyInput.receiveEnergy(Integer.MAX_VALUE,false));
        h.runAfterDelay(61,()->{
            var left=(com.mcmagic.omnira.compat.CreateReactionPool.Port)h.getLevel().getBlockEntity(portPos);
            int before=pool.energyInput.getEnergyStored();
            double expected=pool.electricalEfficiency();
            ReactionPoolBlockEntity.tick(h.getLevel(),pool.getBlockPos(),pool.getBlockState(),pool);
            h.assertTrue(expected>=4&&Math.abs(pool.efficiency()-expected)<.01&&pool.energyInput.getEnergyStored()<before&&pool.electricalPowered(),"FE not consumed, power sources summed or electrical mode missing");
            var right=(com.mcmagic.omnira.compat.CreateReactionPool.Port)h.getLevel().getBlockEntity(pool.getBlockPos().offset(ReactionPoolBlock.offset(facing,5)));
            right.setSpeed(16);right.setNetwork(left.network);left.getOrCreateNetwork().add(right);
            left.tick();
            h.assertTrue(Math.abs(com.mcmagic.omnira.compat.CreateReactionPool.power(pool)-2)<.01,"Two 16 RPM inputs failed to reserve 1024 SU: left="+left.getSpeed()+", right="+right.getSpeed()+", stress="+left.getOrCreateNetwork().calculateStress()+", capacity="+left.getOrCreateNetwork().calculateCapacity());
            h.succeed();
        });
    }
}
