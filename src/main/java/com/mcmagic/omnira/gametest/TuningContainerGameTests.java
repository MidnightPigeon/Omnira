package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.storage.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_tuning_container")
@PrefixGameTestTemplate(false)
public final class TuningContainerGameTests {
    @GameTest(template="spell_arena") public static void tuningItemsIgnoreNearbyUpgradeFilters(GameTestHelper h){
        var a=new BlockPos(3,3,4);var b=a.east(3);
        h.setBlock(a.below(),CrystalStorageContent.ITEM_ORB.get());h.setBlock(b.below(),CrystalStorageContent.ITEM_ORB.get());
        var first=node(h,a);var second=node(h,b);
        var source=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)h.getBlockEntity(a.below());
        var target=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)h.getBlockEntity(b.below());
        source.upgrades.setStackInSlot(0,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.OUTPUT_UPGRADE.get()));
        target.upgrades.setStackInSlot(0,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.INTAKE_UPGRADE.get()));
        var settings=new net.minecraft.nbt.CompoundTag();settings.putBoolean("Whitelist",true);settings.putInt("Directions",0);
        source.upgrades.update(0,settings.copy());target.upgrades.update(0,settings.copy());
        source.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,2));
        NodeTransport.pair(first,second);NodeTransport.tick(first);
        h.assertTrue(source.getItem(0).getCount()==1&&target.getItem(0).getCount()==1,"Nearby upgrade settings filtered tuning items");
        NodeTransport.unpair(first);h.succeed();
    }
    @GameTest(template="spell_arena") public static void transportSkipsBlockedItemsAndIgnoresUpgradeFilters(GameTestHelper h){
        var a=new BlockPos(3,3,4);var b=a.east(3);
        h.setBlock(a.below(),Blocks.CHEST);h.setBlock(b.below(),Blocks.CHEST);
        var first=node(h,a);var second=node(h,b);
        var source=(net.minecraft.world.Container)h.getBlockEntity(a.below());var target=(net.minecraft.world.Container)h.getBlockEntity(b.below());
        source.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
        source.setItem(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
        for(int slot=0;slot<target.getContainerSize();slot++)target.setItem(slot,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,64));
        target.getItem(0).shrink(1);NodeTransport.pair(first,second);NodeTransport.tick(first);
        h.assertTrue(source.getItem(0).getCount()==1&&source.getItem(1).isEmpty()&&target.getItem(0).getCount()==64,"Blocked first item prevented a later eligible transfer");
        NodeTransport.unpair(first);
        h.setBlock(a.below(),CrystalStorageContent.FLUID_ORB.get());h.setBlock(b.below(),CrystalStorageContent.FLUID_ORB.get());
        var liquid=(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)h.getBlockEntity(a.below());
        var receiving=(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)h.getBlockEntity(b.below());
        liquid.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        liquid.upgrades.setStackInSlot(0,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.OUTPUT_UPGRADE.get()));
        var settings=new net.minecraft.nbt.CompoundTag();settings.putBoolean("Whitelist",true);liquid.upgrades.update(0,settings);
        h.runAfterDelay(21,()->{
            NodeTransport.pair(first,second);NodeTransport.tick(first);
            h.assertTrue(liquid.tank.getFluidAmount()==500&&receiving.tank.getFluidAmount()==500,"Independent tuning transfer was filtered or exceeded 500 mB");
            NodeTransport.unpair(first);h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void failedFluidMergeKeepsTransport(GameTestHelper h){
        var a=new BlockPos(3,3,4);var b=a.east(3);
        h.setBlock(a.below(),CrystalStorageContent.FLUID_ORB.get());h.setBlock(b.below(),CrystalStorageContent.FLUID_ORB.get());
        var first=node(h,a);var second=node(h,b);
        var left=(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)h.getBlockEntity(a.below());
        var right=(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)h.getBlockEntity(b.below());
        left.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,500),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        right.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,500),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(NodeTransport.pair(first,second),"Independent liquid stores could not link");
        var marker=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
        marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
        left.setFrequency(0,marker);right.setFrequency(0,marker);
        NodeTransport.tick(first);
        h.assertTrue(left.networkStatus==0&&right.networkStatus==3&&first.peer!=null&&second.peer!=null,"Failed entrant affected the first orb or cancelled transport");
        NodeTransport.unpair(first);h.succeed();
    }
    @GameTest(template="spell_arena") public static void cancelledLinksPersistAndMatchBothIdentities(GameTestHelper h){
        var tag=new net.minecraft.nbt.CompoundTag();var entries=new net.minecraft.nbt.ListTag();
        var node=java.util.UUID.randomUUID();var peer=java.util.UUID.randomUUID();
        var entry=new net.minecraft.nbt.CompoundTag();entry.putUUID("Node",node);entry.putUUID("Peer",peer);entries.add(entry);tag.put("RemovedLinks",entries);
        var ledger=TuningOutputs.load(tag,h.getLevel().registryAccess());
        ledger=TuningOutputs.load(ledger.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(!ledger.consumeRemovedLink(node,java.util.UUID.randomUUID()),"Cancellation affected a different link");
        h.assertTrue(ledger.consumeRemovedLink(node,peer)&&!ledger.consumeRemovedLink(node,peer),"Cancellation did not survive saving or was applied twice");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedNetworksCancelTransportWithoutRestoringIt(GameTestHelper h){
        for(boolean liquid:new boolean[]{false,true}){
            var a=new BlockPos(liquid?9:3,3,4);var b=a.east(3);
            h.setBlock(a.below(),(liquid?CrystalStorageContent.FLUID_ORB:CrystalStorageContent.ITEM_ORB).get());
            h.setBlock(b.below(),(liquid?CrystalStorageContent.FLUID_ORB:CrystalStorageContent.ITEM_ORB).get());
            var first=node(h,a);var second=node(h,b);
            first.terminal=java.util.UUID.randomUUID();second.terminal=java.util.UUID.randomUUID();
            var firstTerminal=first.terminal;var secondTerminal=second.terminal;
            h.assertTrue(NodeTransport.pair(first,second),"Separate stores could not link");
            var left=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)h.getBlockEntity(a.below());
            var right=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)h.getBlockEntity(b.below());
            var marker=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
            marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
            left.setFrequency(0,marker);right.setFrequency(0,marker);
            h.assertTrue(first.peer==null&&second.peer==null,"Shared storage did not immediately cancel transport");
            h.assertTrue(firstTerminal.equals(first.terminal)&&secondTerminal.equals(second.terminal),"Transport cancellation removed terminal bindings");
            h.assertTrue(!NodeTransport.pair(first,second),"Shared store accepted a self-transfer link");
            right.setFrequency(0,net.minecraft.world.item.ItemStack.EMPTY);NodeTransport.tick(first);
            h.assertTrue(first.peer==null&&second.peer==null,"Splitting restored the old link");
            h.assertTrue(NodeTransport.pair(first,second),"Manual reconnection after splitting failed");
            NodeTransport.unpair(first);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void joinedCrystalNetworksEvictLaterTerminalLinkPermanently(GameTestHelper h){
        for(boolean liquid:new boolean[]{false,true}){
            var a=new BlockPos(liquid?9:3,3,4);var b=a.east(3);
            h.setBlock(a.below(),(liquid?CrystalStorageContent.FLUID_ORB:CrystalStorageContent.ITEM_ORB).get());
            h.setBlock(b.below(),(liquid?CrystalStorageContent.FLUID_ORB:CrystalStorageContent.ITEM_ORB).get());
            var first=node(h,a);var second=node(h,b);var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.setUUID(java.util.UUID.randomUUID());first.owner=player.getUUID();second.owner=player.getUUID();
            var terminal=new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.RESONANCE_TERMINAL.get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,terminal);bind(player,first);bind(player,second);
            h.assertTrue(com.mcmagic.omnira.item.ResonanceTerminalItem.data(terminal).contains("Link1"),"Independent crystal stores could not both bind");
            var left=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)h.getBlockEntity(a.below());
            var right=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)h.getBlockEntity(b.below());
            var marker=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
            marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
            left.setFrequency(0,marker);right.setFrequency(0,marker);
            com.mcmagic.omnira.item.ResonanceLinks.prune(terminal,h.getLevel(),false);
            var data=com.mcmagic.omnira.item.ResonanceTerminalItem.data(terminal);
            h.assertTrue(data.contains("Link0")&&!data.contains("Link1")&&first.terminal!=null&&second.terminal==null,"Shared network did not evict later link");
            right.setFrequency(0,net.minecraft.world.item.ItemStack.EMPTY);
            com.mcmagic.omnira.item.ResonanceLinks.prune(terminal,h.getLevel(),false);
            h.assertTrue(!com.mcmagic.omnira.item.ResonanceTerminalItem.data(terminal).contains("Link1")&&second.terminal==null,"Disconnected network restored discarded binding");
        }
        h.succeed();
    }
    private static ResonanceCoreBlockEntity node(GameTestHelper h,BlockPos pos){
        h.setBlock(pos,ModBlocks.RESONANCE_CORE.get());return (ResonanceCoreBlockEntity)h.getBlockEntity(pos);
    }
    private static net.minecraft.world.InteractionResult bind(net.minecraft.world.entity.player.Player player,ResonanceCoreBlockEntity node){
        player.setShiftKeyDown(true);
        return player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(node.getBlockPos().getCenter(),Direction.UP,node.getBlockPos(),false)));
    }
    @GameTest(template="spell_arena") public static void terminalsBindOneNodePerContainerIndependently(GameTestHelper h){
        var a=new BlockPos(3,3,4);var b=a.east();
        h.setBlock(a.below(),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.LEFT));
        h.setBlock(b.below(),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.RIGHT));
        var first=node(h,a);var second=node(h,b);var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        first.owner=player.getUUID();second.owner=player.getUUID();
        var terminal=new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.RESONANCE_TERMINAL.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,terminal);bind(player,first);
        var original=com.mcmagic.omnira.item.ResonanceTerminalItem.data(terminal).getCompound("Link0").copy();
        h.assertTrue(bind(player,second)==net.minecraft.world.InteractionResult.FAIL,"Duplicate binding did not return failure");
        var data=com.mcmagic.omnira.item.ResonanceTerminalItem.data(terminal);
        h.assertTrue(original.equals(data.getCompound("Link0"))&&first.terminal!=null,"Rejected binding altered the original link");
        h.assertTrue(data.contains("Link0")&&!data.contains("Link1")&&second.terminal==null,"Terminal duplicated the same container");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.RESONANCE_TERMINAL.get()));bind(player,second);
        h.assertTrue(second.terminal!=null&&!second.terminal.equals(first.terminal),"Independent terminal cannot bind another node");
        var oldDuplicate=com.mcmagic.omnira.item.ResonanceTerminalItem.data(player.getMainHandItem()).getCompound("Link0").copy();
        data.put("Link1",oldDuplicate);second.terminal=first.terminal;com.mcmagic.omnira.item.ResonanceTerminalItem.save(terminal,data);
        com.mcmagic.omnira.item.ResonanceLinks.prune(terminal,h.getLevel(),false);
        h.assertTrue(!com.mcmagic.omnira.item.ResonanceTerminalItem.data(terminal).contains("Link1")&&second.terminal==null,"Legacy duplicate terminal link was retained");h.succeed();
    }
    @GameTest(template="spell_arena") public static void doubleChestHasOneOutputAndStillAcceptsInput(GameTestHelper h){
        var a=new BlockPos(3,3,4);var b=a.east();
        h.setBlock(a.below(),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.LEFT));
        h.setBlock(b.below(),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.RIGHT));
        var first=node(h,a);var second=node(h,b);
        h.assertTrue(StorageContainerIdentity.same(first,second),"Double chest halves have different identities");
        h.assertTrue(!NodeTransport.pair(first,second)&&first.peer==null&&second.peer==null,"A container was linked to itself");
        h.setBlock(new BlockPos(7,2,4),Blocks.CHEST);var target=node(h,new BlockPos(7,3,4));
        h.setBlock(new BlockPos(9,2,4),Blocks.CHEST);var incoming=node(h,new BlockPos(9,3,4));
        h.assertTrue(NodeTransport.pair(first,target),"First output failed");
        var ledger=TuningOutputs.get(h.getLevel());
        h.assertTrue(!ledger.available(second),"Second output is allowed");
        var saved=ledger.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
        h.assertTrue(!TuningOutputs.load(saved,h.getLevel().registryAccess()).available(second),"Output reservation did not persist");
        h.assertTrue(NodeTransport.pair(incoming,second),"Occupied container rejected an input");
        NodeTransport.unpair(first);h.assertTrue(ledger.available(second),"Unpair did not release output reservation");h.succeed();
    }
    @GameTest(template="spell_arena") public static void createTankMembersShareControllerIdentity(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        var a=new BlockPos(3,2,4);var b=a.east();
        h.setBlock(a,com.simibubi.create.AllBlocks.FLUID_TANK.get());h.setBlock(b,com.simibubi.create.AllBlocks.FLUID_TANK.get());
        var controller=(com.simibubi.create.content.fluids.tank.FluidTankBlockEntity)h.getBlockEntity(a);
        var member=(com.simibubi.create.content.fluids.tank.FluidTankBlockEntity)h.getBlockEntity(b);
        controller.setWidth(2);controller.setHeight(1);member.setController(h.absolutePos(a));
        var first=node(h,a.above());var second=node(h,b.above());
        h.assertTrue(StorageContainerIdentity.key(first).equals(StorageContainerIdentity.key(second)),"Create tank members have different identities");h.succeed();
    }
}
