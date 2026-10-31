package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.storage.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;

final class SableStorageChecks {
    static void nodes(GameTestHelper h,boolean together){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(5,3,5));
        var selected=new java.util.HashSet<BlockPos>();
        for(int i=0;i<=6;i++){
            level.setBlockAndUpdate(base.east(i),Blocks.STONE.defaultBlockState());
            if(together||i==0)selected.add(base.east(i));
        }
        for(int i:new int[]{0,6}){
            level.setBlockAndUpdate(base.east(i),Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(base.east(i).above(),com.mcmagic.omnira.registry.ModBlocks.RESONANCE_CORE.get().defaultBlockState());
            if(together||i==0)selected.add(base.east(i).above());
        }
        var a=(ResonanceCoreBlockEntity)level.getBlockEntity(base.above());
        var b=(ResonanceCoreBlockEntity)level.getBlockEntity(base.east(6).above());
        h.assertTrue(NodeTransport.pair(a,b),"Node fixture failed to pair");
        a.terminal=java.util.UUID.randomUUID();a.setChanged();
        var gathered=SubLevelAssemblyHelper.gatherConnectedBlocks(base,level,32,(q,w,e,r,t)->selected.contains(e));
        var ship=SubLevelAssemblyHelper.assembleBlocks(level,base,gathered.blocks(),gathered.boundingBox());
        h.assertTrue(ship!=null,"Node fixture did not assemble");
        var delta=ship.getPlot().getCenterBlock().subtract(base);var retained=new SableTestBody(ship);
        h.runAfterDelay(25,()->{
            try(retained){
                var actual=(ResonanceCoreBlockEntity)level.getBlockEntity(base.above().offset(delta));actual.refresh();
                h.assertTrue(actual.terminal==null,"Moved node retained external terminal binding");
                if(together){
                    var target=(ResonanceCoreBlockEntity)level.getBlockEntity(base.east(6).above().offset(delta));target.refresh();
                    h.assertTrue(target.getBlockPos().equals(actual.peer)&&actual.getBlockPos().equals(target.peer),"Whole-structure node link lost");
                    ((net.minecraft.world.Container)level.getBlockEntity(actual.getBlockPos().below())).setItem(0,new ItemStack(Items.DIAMOND,4));
                    NodeTransport.tick(actual);
                    h.assertTrue(((net.minecraft.world.Container)level.getBlockEntity(target.getBlockPos().below())).getItem(0).is(Items.DIAMOND),"Moved nodes did not transfer items");
                }else{
                    b.refresh();h.assertTrue(actual.peer==null&&b.peer==null,"Partial movement retained a cross-structure link");
                }
            }h.succeed();
        });
    }
    static void pulse(GameTestHelper h){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(5,3,5));
        level.setBlockAndUpdate(base,Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(base.above(),com.mcmagic.omnira.pulse.PulseContent.RESONANT.get().defaultBlockState());
        var orb=com.mcmagic.omnira.pulse.PulseOrbEntity.from(level.getBlockEntity(base.above()));
        var marker=new ItemStack(Items.PAPER);
        marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
        orb.frequency(0,marker);orb.frequency(1,new ItemStack(Items.QUARTZ));
        orb.upgrade.setStackInSlot(0,new ItemStack(com.mcmagic.omnira.registry.ModItems.BASIC_STACKING_UPGRADE.get()));
        var selected=Set.of(base,base.above());
        var gathered=SubLevelAssemblyHelper.gatherConnectedBlocks(base,level,32,(a,b,c,d,e)->selected.contains(c));
        var ship=SubLevelAssemblyHelper.assembleBlocks(level,base,gathered.blocks(),gathered.boundingBox());
        h.assertTrue(ship!=null,"Pulse storage fixture did not assemble");
        var moved=base.above().offset(ship.getPlot().getCenterBlock().subtract(base));
        var retained=new SableTestBody(ship);
        h.runAfterDelay(25,()->{
            try(retained){
                var actual=com.mcmagic.omnira.pulse.PulseOrbEntity.from(level.getBlockEntity(moved));
                h.assertTrue(actual!=null&&actual.capacity()==16384,"Assembly lost shared pulse upgrade");
                actual.frequency(1,ItemStack.EMPTY);
                h.assertTrue(actual.upgrade.getStackInSlot(0).is(com.mcmagic.omnira.registry.ModItems.BASIC_STACKING_UPGRADE.get()),"Detached pulse upgrade remained in stale endpoint");
            }h.succeed();
        });
    }
    static void run(GameTestHelper h,boolean liquid){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(5,3,5));
        var block=(liquid?CrystalStorageContent.FLUID_ORB:CrystalStorageContent.ITEM_ORB).get();
        level.setBlockAndUpdate(base,Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(base.above(),block.defaultBlockState());
        var ball=(CrystalBallBlockEntity)level.getBlockEntity(base.above());
        var marker=new ItemStack(Items.PAPER);
        marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
        ball.setFrequency(0,marker);ball.setFrequency(1,new ItemStack(Items.QUARTZ));
        if(liquid)((LiquidCrystalBallBlockEntity)ball).tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        else ball.setItem(0,new ItemStack(Items.DIAMOND,23));
        var selected=Set.of(base,base.above());
        var gathered=SubLevelAssemblyHelper.gatherConnectedBlocks(base,level,32,(a,b,c,d,e)->selected.contains(c));
        var ship=SubLevelAssemblyHelper.assembleBlocks(level,base,gathered.blocks(),gathered.boundingBox());
        h.assertTrue(ship!=null,"Storage fixture did not assemble");
        var moved=base.above().offset(ship.getPlot().getCenterBlock().subtract(base));
        var retained=new SableTestBody(ship);
        h.runAfterDelay(25,()->{
            try(retained){
                var actual=(CrystalBallBlockEntity)level.getBlockEntity(moved);
                h.assertTrue(actual!=null,"Moved orb missing");
                CrystalStorageNetworks.get(level).register(actual);
                h.assertTrue(liquid?((LiquidCrystalBallBlockEntity)actual).tank.getFluidAmount()==1000:actual.getItem(0).getCount()==23,"Physical assembly lost shared contents");
                actual.setFrequency(1,ItemStack.EMPTY);
                h.assertTrue(liquid?((LiquidCrystalBallBlockEntity)actual).tank.getFluidAmount()==1000:actual.getItem(0).getCount()==23,"Disconnect after physical assembly lost contents to stale endpoint");
            }h.succeed();
        });
    }
}
