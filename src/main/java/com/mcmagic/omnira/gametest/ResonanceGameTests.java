package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.ResonanceTerminalItem;
import com.mcmagic.omnira.menu.ResonanceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_resonance") @PrefixGameTestTemplate(false)
public final class ResonanceGameTests {
    @GameTest(template="spell_arena") public static void openingRequiresHalfMaximumMana(GameTestHelper h){
        var s=setup(h,false);
        s.player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(500,1001));
        h.assertTrue(!s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER)&&s.player.containerMenu==s.menu,"Insufficient mana opened menu");
        h.assertTrue(s.player.getData(ModAttachments.MANA).current()==500,"Failed opening spent mana");
        s.player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(500.5,1001));
        h.assertTrue(s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER)&&s.player.getData(ModAttachments.MANA).current()==0,"Exact half maximum should open and spend once");
        s.player.closeContainer();h.succeed();
    }
    private record Setup(net.minecraft.server.level.ServerPlayer player,ResonanceMenu menu,CrystalBallBlockEntity ball,ResonanceCoreBlockEntity core) {}
    private static Setup setup(GameTestHelper h,boolean liquid) {
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"remote-container");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(h.absolutePos(new BlockPos(5,2,5)).getCenter().add(100,0,0));
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000));
        var terminal=new ItemStack(ModItems.RESONANCE_TERMINAL.get());p.getInventory().setItem(0,terminal);ResonanceTerminalItem.authorize(terminal,p);
        var base=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(base,(liquid?ModBlocks.LIQUID_CRYSTAL_BALL:ModBlocks.CRYSTAL_BALL).get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(base.above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var core=(ResonanceCoreBlockEntity)h.getLevel().getBlockEntity(base.above());core.owner=p.getUUID();
        var tag=ResonanceTerminalItem.data(terminal);core.terminal=tag.getUUID("Terminal");
        var link=new net.minecraft.nbt.CompoundTag();link.putUUID("Identity",core.identity);link.putString("Dimension",h.getLevel().dimension().location().toString());link.putLong("Pos",core.getBlockPos().asLong());link.putInt("Kind",liquid?3:2);tag.put("Link0",link);ResonanceTerminalItem.save(terminal,tag);core.refresh();
        var menu=new ResonanceMenu(1,p.getInventory(),0);p.containerMenu=menu;h.assertTrue(menu.clickMenuButton(p,0),"Remote container could not select");
        return new Setup(p,menu,(CrystalBallBlockEntity)h.getLevel().getBlockEntity(base),core);
    }
    @GameTest(template="spell_arena") public static void nativeItemsWithoutManaAndSourceLock(GameTestHelper h) {
        var s=setup(h,false);s.ball.setItem(0,new ItemStack(Items.STONE,20));
        s.player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(500,1000));
        s.menu.clicked(27,0,ClickType.PICKUP,s.player);
        h.assertTrue(s.player.getInventory().getItem(0).is(ModItems.RESONANCE_TERMINAL.get()) && s.menu.getCarried().isEmpty(),"Held terminal moved");
        h.assertTrue(s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Native menu did not open");
        h.assertTrue(s.player.containerMenu instanceof com.mcmagic.omnira.menu.CrystalBallMenu,"Wrong menu type");
        var nativeMenu=s.player.containerMenu;
        h.assertTrue(nativeMenu.stillValid(s.player),"Native menu rejected remote distance");
        nativeMenu.clicked(0,0,ClickType.PICKUP,s.player);
        h.assertTrue(nativeMenu.getCarried().getCount()==20&&s.ball.getItem(0).isEmpty()&&s.player.getData(ModAttachments.MANA).current()==0,"Native transfer failed or charged mana");
        s.player.getInventory().setItem(0,ItemStack.EMPTY);
        h.assertTrue(Boolean.FALSE.equals(com.mcmagic.omnira.storage.RemoteContainerAccess.validity(s.player,nativeMenu)),"Removed terminal retained access");
        s.player.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void replacedCoreRejectsAccess(GameTestHelper h) {
        var s=setup(h,false);s.ball.setItem(0,new ItemStack(Items.DIAMOND));s.core.identity=java.util.UUID.randomUUID();
        s.menu.broadcastChanges();
        h.assertTrue(s.menu.getCarried().isEmpty() && s.ball.getItem(0).is(Items.DIAMOND) && s.menu.kind()==0,"Replacement core reused link");
        var other=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(!ResonanceTerminalItem.authorize(s.player.getInventory().getItem(0),other),"Foreign terminal access allowed");s.menu.removed(s.player);h.succeed();
    }
    @GameTest(template="spell_arena") public static void fluidUsesNativeMenuWithoutMana(GameTestHelper h) {
        var s=setup(h,true);var ball=(LiquidCrystalBallBlockEntity)s.ball;
        h.assertTrue(s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Liquid native menu did not open");
        var nativeMenu=(com.mcmagic.omnira.menu.CrystalBallMenu)s.player.containerMenu;
        h.assertTrue(nativeMenu.liquid()&&nativeMenu.ball==ball,"Liquid native menu lost target");
        h.assertTrue(s.player.getData(ModAttachments.MANA).current()==500,"Opening native liquid menu must cost half maximum mana");
        s.core.terminal=null;
        h.assertTrue(Boolean.FALSE.equals(com.mcmagic.omnira.storage.RemoteContainerAccess.validity(s.player,nativeMenu)),"Unlinked native liquid menu retained access");
        s.player.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void ritualConsumesEightCoresAndRetainsCenter(GameTestHelper h) {
        ritual(h,false);
    }
    @GameTest(template="spell_arena") public static void furnaceKeepsItsNativeMenu(GameTestHelper h) throws Exception {
        var s=setup(h,false);var pos=s.ball.getBlockPos();
        var channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        s.player.connection.getConnection().channelActive(channel.pipeline().firstContext());
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(s.player.connection.getConnection());
        h.getLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState());s.core.refresh();
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Furnace cannot open");
        h.assertTrue(s.player.containerMenu instanceof net.minecraft.world.inventory.FurnaceMenu,"Furnace replaced with generic storage");
        h.getLevel().removeBlock(pos,false);
        h.assertTrue(Boolean.FALSE.equals(com.mcmagic.omnira.storage.RemoteContainerAccess.validity(s.player,s.player.containerMenu)),"Removed furnace retained access");
        s.player.closeContainer();channel.finishAndReleaseAll();h.succeed();
    }
    @GameTest(template="spell_arena") public static void createStorageWithoutUiCannotOpen(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        var s=setup(h,false);var pos=s.ball.getBlockPos();
        for(var block:new net.minecraft.world.level.block.Block[]{com.simibubi.create.AllBlocks.ITEM_VAULT.get(),com.simibubi.create.AllBlocks.FLUID_TANK.get()}){
            h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());s.core.refresh();
            s.menu.clickMenuButton(s.player,0);
            h.assertTrue(s.menu.kind()==0&&!s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"UI invented for "+block);
            h.assertTrue(s.player.containerMenu==s.menu,"No-UI container opened a menu");
            h.assertTrue(s.player.getData(ModAttachments.MANA).current()==1000,"Failed opening charged mana");
        }
        s.player.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void doubleChestRemovalRevokesNativeAccess(GameTestHelper h){
        var s=setup(h,false);var pos=s.ball.getBlockPos();var level=h.getLevel();
        level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT));
        level.setBlockAndUpdate(pos.east(),net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
        s.core.refresh();
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Double chest cannot open");
        h.assertTrue(s.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu chest&&chest.getRowCount()==6,"Double chest did not retain six rows");
        level.removeBlock(pos.east(),false);
        h.assertTrue(Boolean.FALSE.equals(com.mcmagic.omnira.storage.RemoteContainerAccess.validity(s.player,s.player.containerMenu)),"Removed chest half retained access");
        s.player.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void reactionPoolNodeUsesControllerMenu(GameTestHelper h){
        var s=setup(h,false);var center=s.ball.getBlockPos().below();var level=h.getLevel();
        h.assertTrue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.place(level,center,net.minecraft.core.Direction.NORTH),"Pool placement failed");
        s.core.refresh();
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Pool cannot open");
        h.assertTrue(s.player.containerMenu instanceof com.mcmagic.omnira.alchemy.ReactionPoolMenu,"Pool did not open native UI");
        s.player.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void advancedCoreSupportsResonance(GameTestHelper h) {
        ritual(h,true);
    }
    private static void ritual(GameTestHelper h,boolean advanced) {
        var core=(advanced?ModBlocks.ADVANCED_RITUAL_ENERGY_CORE:ModBlocks.RITUAL_ENERGY_CORE).get();
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var center=h.absolutePos(new BlockPos(6,2,6));
        h.getLevel().setBlockAndUpdate(center,core.defaultBlockState());
        for(var offset:com.mcmagic.omnira.world.dimension.DreamRitual.ANCHORS) {
            var pos=center.offset(offset[0],0,offset[1]);h.getLevel().setBlockAndUpdate(pos,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
            ((CrystalPedestalBlockEntity)h.getLevel().getBlockEntity(pos)).setItem(0,new ItemStack(ModItems.RESONANCE_CORE.get()));
        }
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.SPACETIME_KNOT.get()));
        h.assertTrue(com.mcmagic.omnira.world.dimension.ResonanceRitual.activate(h.getLevel(),center,p,net.minecraft.world.InteractionHand.MAIN_HAND),"Ritual failed");
        h.assertTrue(p.getMainHandItem().isEmpty() && h.getLevel().getBlockState(center).is(core),"Ritual consumed center or kept knot");
        for(var offset:com.mcmagic.omnira.world.dimension.DreamRitual.ANCHORS)h.assertTrue(!((CrystalPedestalBlockEntity)h.getLevel().getBlockEntity(center.offset(offset[0],0,offset[1]))).getItem(0).isEmpty(),"Symbolic offering consumed");
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(2)).stream().anyMatch(e->e.getItem().is(ModItems.RESONANCE_TERMINAL.get())),"Terminal not ejected");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bindingLimitAndInvalidLinkRemoval(GameTestHelper h) {
        var s=setup(h,false);s.player.setShiftKeyDown(true);var terminal=s.player.getMainHandItem();
        for(int i=1;i<=4;i++) {
            var base=h.absolutePos(new BlockPos(2*i,2,9));h.getLevel().setBlockAndUpdate(base,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
            var pos=base.above();h.getLevel().setBlockAndUpdate(pos,ModBlocks.RESONANCE_CORE.get().defaultBlockState());
            var core=(ResonanceCoreBlockEntity)h.getLevel().getBlockEntity(pos);core.owner=s.player.getUUID();
            terminal.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(s.player,net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false)));
            h.assertTrue((core.terminal!=null)==(i<4),"Binding limit must be four cores");
        }
        s.core.identity=java.util.UUID.randomUUID();s.menu.broadcastChanges();
        h.assertTrue(s.menu.kind()==0 && s.menu.selected()==-1 && !ResonanceTerminalItem.data(terminal).contains("Link0"),"Invalid link was not automatically released");
        s.menu.removed(s.player);h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void crossDimensionGenericChest(GameTestHelper h) {
        var s=setup(h,false);s.menu.removed(s.player);
        var level=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);var base=new BlockPos(s.core.getBlockPos().getX(),80,s.core.getBlockPos().getZ());
        level.getChunk(base.getX()>>4,base.getZ()>>4);
        level.setBlockAndUpdate(base,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(base.above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var core=(ResonanceCoreBlockEntity)level.getBlockEntity(base.above());core.owner=s.player.getUUID();
        var terminal=s.player.getMainHandItem();var tag=ResonanceTerminalItem.data(terminal);core.terminal=tag.getUUID("Terminal");
        var link=tag.getCompound("Link0");link.putString("Dimension",level.dimension().location().toString());link.putLong("Pos",base.above().asLong());link.putUUID("Identity",core.identity);ResonanceTerminalItem.save(terminal,tag);
        ((net.minecraft.world.Container)level.getBlockEntity(base)).setItem(0,new ItemStack(Items.DIAMOND,3));
        var menu=new ResonanceMenu(2,s.player.getInventory(),0);
        s.player.containerMenu=menu;
        h.assertTrue(menu.clickMenuButton(s.player,0)&&menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Cross-dimensional chest unavailable");
        var nativeMenu=s.player.containerMenu;h.assertTrue(nativeMenu instanceof net.minecraft.world.inventory.ChestMenu,"Chest native menu missing");
        nativeMenu.clicked(0,0,ClickType.PICKUP,s.player);
        h.assertTrue(nativeMenu.getCarried().getCount()==3 && s.player.getData(ModAttachments.MANA).current()==500,"Cross-dimensional opening cost incorrect");
        s.player.closeContainer();level.removeBlock(base.above(),false);level.removeBlock(base,false);h.succeed();
    }
}
