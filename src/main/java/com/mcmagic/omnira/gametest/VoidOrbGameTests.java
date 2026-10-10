package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.storage.CrystalStorageContent;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_void_orb")
@PrefixGameTestTemplate(false)
public final class VoidOrbGameTests {
    @GameTest(template="spell_arena") public static void shatteringDropsOnlyAggregateWithoutGrace(GameTestHelper h){
        var ball=place(h);var world=h.getLevel();var pos=ball.getBlockPos();
        ball.upgrades.insertItem(1,new ItemStack(ModItems.INTAKE_UPGRADE.get()),false);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(int i=0;i<9;i++)ball.landedOn(player);
        h.assertTrue(world.getBlockState(pos).isAir(),"Orb survived nine impacts");
        var drops=world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1));
        h.assertTrue(drops.size()==1&&drops.getFirst().getItem().is(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()),"Shattering leaked glass, upgrades or an orb");
        var aggregate=drops.getFirst();
        h.assertTrue(aggregate.getItem().getCount()==1,"Expected one aggregate");
        aggregate.getItem().getItem().onEntityItemUpdate(aggregate.getItem(),aggregate);
        h.assertTrue(aggregate.isRemoved(),"Aggregate gained an unexpected pickup grace period");
        h.succeed();
    }
    private static VoidCrystalBallBlockEntity place(GameTestHelper h){
        h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(2,2,2)),CrystalStorageContent.VOID_ORB.get().defaultBlockState());
        return (VoidCrystalBallBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2,2,2)));
    }
    @GameTest(template="spell_arena") public static void irreversibleDualSinkAndReload(GameTestHelper h){
        var ball=place(h);var world=h.getLevel();var pos=ball.getBlockPos();
        var items=world.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.UP);
        var fluids=world.getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.UP);
        var offered=new ItemStack(Items.DIAMOND,64);
        h.assertTrue(items!=null&&fluids!=null,"Both input capabilities must exist");
        h.assertTrue(items.insertItem(0,offered,true).isEmpty()&&offered.getCount()==64,"Simulation must accept without modifying caller stack");
        for(int i=0;i<10;i++)h.assertTrue(items.insertItem(0,offered,false).isEmpty(),"Item sink must not fill up");
        h.assertTrue(fluids.fill(new FluidStack(Fluids.WATER,40000),FluidAction.SIMULATE)==40000,"Fluid simulation rejected");
        h.assertTrue(fluids.fill(new FluidStack(Fluids.LAVA,40000),FluidAction.EXECUTE)==40000,"Fluid execution rejected");
        h.assertTrue(items.extractItem(0,64,false).isEmpty()&&fluids.drain(40000,FluidAction.EXECUTE).isEmpty(),"Destroyed contents can be extracted");
        var loaded=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,ball.getBlockState(),ball.saveWithFullMetadata(world.registryAccess()),world.registryAccess());
        h.assertTrue(loaded instanceof VoidCrystalBallBlockEntity&&((VoidCrystalBallBlockEntity)loaded).isEmpty(),"Reload changed type or restored destroyed contents");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void menuDestroysAndOnlyIntakeFits(GameTestHelper h){
        var ball=place(h);var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var menu=new com.mcmagic.omnira.menu.CrystalBallMenu(1,player.getInventory(),ball);
        h.assertTrue(menu.storageSlots==1&&menu.playerStart==2&&menu.slots.get(1).getContainerSlot()==1,"Wrong trash or upgrade layout");
        menu.setCarried(new ItemStack(Items.DIAMOND,8));menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        h.assertTrue(menu.getCarried().isEmpty()&&ball.isEmpty(),"Trash click did not destroy contents immediately");
        player.getInventory().setItem(9,new ItemStack(Items.STONE,64));menu.quickMoveStack(player,menu.playerStart);
        h.assertTrue(player.getInventory().getItem(9).isEmpty()&&ball.isEmpty(),"Shift insertion did not destroy items");
        h.assertTrue(ball.upgrades.insertItem(1,new ItemStack(ModItems.INTAKE_UPGRADE.get()),false).isEmpty(),"Intake rejected");
        h.assertTrue(!ball.upgrades.insertItem(0,new ItemStack(ModItems.OUTPUT_UPGRADE.get()),false).isEmpty(),"Hidden upgrade socket accepted upgrade");
        ball.upgrades.extractItem(1,1,false);
        h.assertTrue(!ball.upgrades.insertItem(1,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()),false).isEmpty(),"Capacity upgrade accepted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void filteredIntakeAndResonanceAccess(GameTestHelper h){
        var ball=place(h);var world=h.getLevel();var source=ball.getBlockPos().north();world.setBlockAndUpdate(source,Blocks.BARREL.defaultBlockState());
        var inventory=(net.minecraft.world.Container)world.getBlockEntity(source);inventory.setItem(0,new ItemStack(Items.STONE,4));inventory.setItem(1,new ItemStack(Items.DIAMOND,4));
        ball.upgrades.insertItem(1,new ItemStack(ModItems.INTAKE_UPGRADE.get()),false);
        var settings=OrbUpgrades.settings(ball.upgrades.getStackInSlot(1));settings.putBoolean("Whitelist",true);settings.putInt("Directions",1);ball.upgrades.update(1,settings);
        ball.upgrades.filter(1,0,new ItemStack(Items.DIAMOND));
        for(int i=0;i<20;i++)ball.serverTick();
        h.assertTrue(inventory.countItem(Items.STONE)==4&&inventory.countItem(Items.DIAMOND)==3&&ball.isEmpty(),"Intake did not honor whitelist and 1/s rate");
        world.setBlockAndUpdate(ball.getBlockPos().above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var node=(ResonanceCoreBlockEntity)world.getBlockEntity(ball.getBlockPos().above());
        h.assertTrue(node.kind()==5&&node.items()!=null&&node.fluids()!=null,"Resonance cannot access both sinks");node.refresh();h.succeed();
    }
    @GameTest(template="spell_arena") public static void unorderedAssembly(GameTestHelper h){
        var inventory=new net.minecraft.world.SimpleContainer(7);
        inventory.setItem(6,new ItemStack(ModItems.CRYSTAL_BALL.get()));inventory.setItem(0,new ItemStack(Items.ENDER_EYE));
        inventory.setItem(2,new ItemStack(ModItems.LIQUID_CRYSTAL_BALL.get()));inventory.setItem(4,new ItemStack(ModItems.DARK_MICROCORE.get()));
        var work=com.mcmagic.omnira.recipe.AssemblyWork.find(h.getLevel(),inventory);
        h.assertTrue(work!=null&&work.finish().get().getFirst().is(CrystalStorageContent.VOID_ITEM.get()),"Unordered four-input recipe rejected");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=75) public static void terminalUsesNativeVoidDisposal(GameTestHelper h){
        var ball=place(h);var world=h.getLevel();
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"remote-void");
        var player=new net.minecraft.server.level.ServerPlayer(world.getServer(),world,profile,net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.neoforged.neoforge.common.util.FakePlayer(world,profile).connection;
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000));
        var terminal=new ItemStack(ModItems.RESONANCE_TERMINAL.get());player.getInventory().setItem(0,terminal);
        com.mcmagic.omnira.item.ResonanceTerminalItem.authorize(terminal,player);
        world.setBlockAndUpdate(ball.getBlockPos().above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var core=(ResonanceCoreBlockEntity)world.getBlockEntity(ball.getBlockPos().above());core.owner=player.getUUID();
        var tag=com.mcmagic.omnira.item.ResonanceTerminalItem.data(terminal);core.terminal=tag.getUUID("Terminal");
        var link=new net.minecraft.nbt.CompoundTag();link.putUUID("Identity",core.identity);link.putString("Dimension",world.dimension().location().toString());
        link.putLong("Pos",core.getBlockPos().asLong());link.putInt("Kind",5);tag.put("Link0",link);
        com.mcmagic.omnira.item.ResonanceTerminalItem.save(terminal,tag);core.refresh();
        var menu=new com.mcmagic.omnira.menu.ResonanceMenu(1,player.getInventory(),0);
        player.containerMenu=menu;
        h.assertTrue(menu.clickMenuButton(player,0)&&menu.kind()==6&&menu.clickMenuButton(player,com.mcmagic.omnira.menu.ResonanceMenu.OPEN_CONTAINER),"Dual sink could not open in terminal");
        h.assertTrue(core.getBlockState().getValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE)==5,"Bound void node is not black mode");
        var nativeMenu=player.containerMenu;
        nativeMenu.setCarried(new ItemStack(Items.DIAMOND));nativeMenu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        h.assertTrue(nativeMenu.getCarried().isEmpty()&&ball.isEmpty(),"Terminal item input did not destroy");
        nativeMenu.slots.get(0).set(new ItemStack(Items.WATER_BUCKET));nativeMenu.broadcastChanges();
        h.runAtTickTime(45,()->{
            h.assertTrue(nativeMenu.slots.get(0).getItem().isEmpty()&&ball.fluidHandler.getFluidInTank(0).isEmpty(),"Native void slot retained input");
            h.assertTrue(player.getData(ModAttachments.MANA).current()==500,"Void opening must charge half maximum mana once");
            player.closeContainer();h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void tuningTransportsIntoSink(GameTestHelper h){
        var sink=place(h);var world=h.getLevel();var source=h.absolutePos(new BlockPos(6,2,2));
        world.setBlockAndUpdate(source,Blocks.BARREL.defaultBlockState());
        ((net.minecraft.world.Container)world.getBlockEntity(source)).setItem(0,new ItemStack(Items.DIAMOND,3));
        world.setBlockAndUpdate(source.above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        world.setBlockAndUpdate(sink.getBlockPos().above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var from=(ResonanceCoreBlockEntity)world.getBlockEntity(source.above());var to=(ResonanceCoreBlockEntity)world.getBlockEntity(sink.getBlockPos().above());
        h.assertTrue(com.mcmagic.omnira.storage.NodeTransport.pair(from,to),"Sink tuning link rejected");
        com.mcmagic.omnira.storage.NodeTransport.tick(from);
        h.assertTrue(((net.minecraft.world.Container)world.getBlockEntity(source)).countItem(Items.DIAMOND)==2&&sink.isEmpty(),"Tuning item input failed");
        world.setBlockAndUpdate(source,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var liquid=(LiquidCrystalBallBlockEntity)world.getBlockEntity(source);liquid.tank.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE);
        from.output=false;
        h.runAfterDelay(22,()->{
            from.output=true;com.mcmagic.omnira.storage.NodeTransport.tick(from);
            h.assertTrue(liquid.tank.getFluidAmount()==500&&sink.fluidHandler.getFluidInTank(0).isEmpty(),"Tuning fluid input failed");h.succeed();
        });
    }
}
