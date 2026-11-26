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
    @GameTest(template="spell_arena") public static void freshlyBoundPlacedBackpacks(GameTestHelper h) throws Exception {
        if(!net.neoforged.fml.ModList.get().isLoaded("sophisticatedbackpacks")){h.succeed();return;}
        var s=setup(h,false);var player=s.player;var level=h.getLevel();
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false));
        player.closeContainer();
        for(String tier:new String[]{"backpack","netherite_backpack"}){
            var pos=h.absolutePos(new BlockPos(tier.equals("backpack")?3:8,3,7));
            boolean crossDimension=tier.equals("netherite_backpack");
            if(crossDimension)pos=pos.atY(100);
            placeBackpack(h,player,pos,tier);
            var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
            var nodePos=pos.above();
            level.setBlockAndUpdate(nodePos,ModBlocks.RESONANCE_CORE.get().defaultBlockState());
            var node=(ResonanceCoreBlockEntity)level.getBlockEntity(nodePos);node.owner=player.getUUID();
            var terminal=new ItemStack(tier.equals("backpack")?ModItems.RESONANCE_TERMINAL.get():ModItems.ENHANCED_RESONANCE_TERMINAL.get());
            player.setItemInHand(hand,terminal);
            var hit=new net.minecraft.world.phys.BlockHitResult(nodePos.getCenter(),net.minecraft.core.Direction.UP,nodePos,false);
            terminal.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,hit));
            h.assertTrue(node.terminal!=null && ResonanceTerminalItem.data(terminal).contains("Link0"),tier+" refused first binding");
            player.setShiftKeyDown(false);
            node.items().insertItem(0,new ItemStack(Items.DIAMOND,3),false);
            net.neoforged.neoforge.items.IItemHandler localInventory=null;
            if(crossDimension){
                var placed = level.getBlockEntity(pos);
                var wrapper = placed.getClass().getMethod("getBackpackWrapper").invoke(placed);
                var upgrades = (net.neoforged.neoforge.items.IItemHandler)wrapper.getClass().getMethod("getUpgradeHandler").invoke(wrapper);
                var tank = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.parse("sophisticatedbackpacks:tank_upgrade"));
                h.assertTrue(upgrades.insertItem(0,new ItemStack(tank),false).isEmpty(),"Tank installation failed");
                wrapper.getClass().getMethod("setColumnsTaken",int.class,boolean.class).invoke(wrapper,2,true);
                var saved = placed.saveWithFullMetadata(level.registryAccess());
                var state = placed.getBlockState();
                level.removeBlockEntity(pos);
                var restored = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,state,saved,level.registryAccess());
                level.setBlockEntity(restored);
                // Other block neighbors can query this capability before the first block-entity tick.
                level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos,net.minecraft.core.Direction.UP);
                var away=level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
                player.setServerLevel(away);away.getChunkAt(pos);
                placeBackpack(h,player,pos,tier);
                localInventory=away.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,pos,net.minecraft.core.Direction.UP);
                h.assertTrue(localInventory!=null && localInventory.insertItem(0,new ItemStack(Items.EMERALD,11),false).isEmpty(),"Local decoy setup failed");
                player.setItemInHand(hand,terminal);player.setShiftKeyDown(false);
            }
            var selector=new ResonanceMenu(1,player.getInventory(),0);player.containerMenu=selector;
            h.assertTrue(selector.clickMenuButton(player,0)&&selector.clickMenuButton(player,ResonanceMenu.OPEN_CONTAINER),tier+" failed to open");
            if(crossDimension)h.assertTrue(selector.status(0)==ResonanceCoreBlockEntity.MIXED,
                    "Cold tank backpack link classified as "+selector.status(0));
            var menu=player.containerMenu;
            h.assertTrue(menu.getClass().getName().endsWith(".gui.BackpackContainer"),"Expected native backpack menu");
            verifyBackpackOpeningPackets(h,channel,player,menu,pos,tier);
            player.tick();player.doTick();
            h.assertTrue(menu==player.containerMenu && menu.getSlot(0).getItem().is(Items.DIAMOND),"Remote native slots missing or menu closed");
            menu.clicked(0,0,ClickType.PICKUP,player);
            h.assertTrue(menu.getCarried().getCount()==3 && node.items().getStackInSlot(0).isEmpty(),"Remote backpack pickup failed");
            menu.clicked(0,0,ClickType.PICKUP,player);
            h.assertTrue(node.items().getStackInSlot(0).getCount()==3,"Remote backpack deposit failed");
            if(crossDimension){
                h.assertTrue(menu.stillValid(player),"Bound backpack failed native validity in another dimension");
                var heldWrapper = menu.getClass().getMethod("getStorageWrapper").invoke(menu);
                var target = level.getBlockEntity(pos);
                target.onLoad();
                h.assertTrue(target.getClass().getMethod("getBackpackWrapper").invoke(target)==heldWrapper,
                        "Late chunk initialization replaced the open storage");
                menu.clicked(0,0,ClickType.QUICK_MOVE,player);
                h.assertTrue(node.items().getStackInSlot(0).isEmpty() && player.getInventory().countItem(Items.DIAMOND)==3,
                        "Shift pickup did not transfer exactly the three stored diamonds");
                int diamondSlot = menu.slots.stream().filter(slot -> slot.container == player.getInventory()
                        && slot.getItem().is(Items.DIAMOND)).findFirst().orElseThrow().index;
                menu.clicked(diamondSlot,0,ClickType.QUICK_MOVE,player);
                h.assertTrue(player.getInventory().countItem(Items.DIAMOND)==0 && node.items().getStackInSlot(0).getCount()==3,
                        "Shift deposit did not return exactly three diamonds to the remote backpack");
                level.removeBlock(nodePos,false);
                h.assertTrue(!menu.stillValid(player),"Destroyed binding retained access");
            }
            player.closeContainer();
            if(crossDimension){
                h.assertTrue(localInventory.getStackInSlot(0).is(Items.EMERALD)&&localInventory.getStackInSlot(0).getCount()==11,
                        "Same-position backpack in player dimension was modified");
                player.serverLevel().removeBlock(pos,false);
                player.setServerLevel(level);level.removeBlock(pos,false);
            }
        }
        channel.finishAndReleaseAll();h.succeed();
    }
    private static void verifyBackpackOpeningPackets(GameTestHelper h, io.netty.channel.embedded.EmbeddedChannel channel,
            net.minecraft.server.level.ServerPlayer player, net.minecraft.world.inventory.AbstractContainerMenu menu,
            BlockPos pos, String tier) throws Exception {
        net.minecraft.world.level.block.entity.BlockEntity copy = null;
        Object packet;
        while ((packet = channel.readOutbound()) != null) {
            if (!(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom)) continue;
            if (custom.payload() instanceof com.mcmagic.omnira.network.RemoteContainerSnapshot snapshot
                    && snapshot.menuId() == menu.containerId) {
                copy = snapshot.loadEntity(player.level(), pos, h.getLevel().getBlockState(pos));
                var wrapper = copy.getClass().getMethod("getBackpackWrapper").invoke(copy);
                var item = (ItemStack)wrapper.getClass().getMethod("getBackpack").invoke(wrapper);
                h.assertTrue(item.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sophisticatedbackpacks", tier))),
                        "Snapshot did not initialize the " + tier + " before opening");
            }
            if (custom.payload() instanceof net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload open
                    && open.windowId() == menu.containerId) {
                h.assertTrue(copy != null, "Opening packet preceded the backpack snapshot");
                var data = new net.minecraft.network.RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.wrappedBuffer(open.additionalData()), player.registryAccess());
                try {
                    var contextType = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext");
                    var context = contextType.getMethod("fromBuffer",net.minecraft.network.FriendlyByteBuf.class,
                            net.minecraft.world.level.Level.class).invoke(null,data,player.level());
                    h.assertTrue(!data.isReadable(), "Native backpack context did not consume the opening data");
                    h.assertTrue(pos.equals(contextType.getMethod("getBackpackPosition",
                            net.minecraft.world.entity.player.Player.class).invoke(context,player)), "Wrong opening position");
                    ((com.mcmagic.omnira.compat.SophisticatedBackpackMenus.BoundContext)context).omnira$bind(copy);
                    var opened = (net.minecraft.world.inventory.AbstractContainerMenu)menu.getClass().getConstructor(
                            int.class,net.minecraft.world.entity.player.Player.class,contextType).newInstance(menu.containerId,player,context);
                    h.assertTrue(opened.slots.size() == menu.slots.size(), "Wrong native menu size for " + tier);
                    for (var slot : menu.slots) {
                        if (slot.container != player.getInventory()) continue;
                        var remoteSlot = opened.getSlot(slot.index);
                        h.assertTrue(remoteSlot.container == player.getInventory() && remoteSlot.getContainerSlot() == slot.getContainerSlot(),
                                "Opening data mapped player slot " + slot.getContainerSlot() + " into backpack storage");
                    }
                } finally {
                    data.release();
                }
                return;
            }
        }
        h.fail("Missing native backpack opening packet");
    }

    private static void placeBackpack(GameTestHelper h,net.minecraft.server.level.ServerPlayer player,BlockPos pos,String tier){
        var bag=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sophisticatedbackpacks",tier)));
        var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
        player.setItemInHand(hand,bag);player.setShiftKeyDown(true);
        var level=player.serverLevel();
        level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var place=new net.minecraft.world.phys.BlockHitResult(pos.below().getCenter().add(0,.5,0),net.minecraft.core.Direction.UP,pos.below(),false);
        h.assertTrue(bag.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,place)).consumesAction(),"Backpack placement failed");
    }
    @GameTest(template="spell_arena") public static void remoteMachineButtonsRecheckTheirRealHost(GameTestHelper h) throws Exception {
        var s=setup(h,false);var level=h.getLevel();var pos=s.ball.getBlockPos();
        var channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        s.player.connection.getConnection().channelActive(channel.pipeline().firstContext());
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(s.player.connection.getConnection());
        level.setBlockAndUpdate(pos,com.mcmagic.omnira.pulse.PulseContent.RESONANT.get().defaultBlockState());
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Pulse orb did not open");
        var menu=(com.mcmagic.omnira.pulse.PulseOrbMenu)s.player.containerMenu;
        menu.setCarried(new ItemStack(Items.EMERALD));
        h.assertTrue(menu.clickMenuButton(s.player,0)&&menu.orb.frequency[0].is(Items.EMERALD),"Remote frequency change failed");
        s.player.closeContainer();
        level.setBlockAndUpdate(pos,com.mcmagic.omnira.codex.MyriadCodexContent.BLOCK.get().defaultBlockState());
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Codex did not open");
        var codex=(com.mcmagic.omnira.codex.MyriadCodexMenu)s.player.containerMenu;
        h.assertTrue(codex.clickMenuButton(s.player,30)&&codex.codex.hollow,"Remote codex option failed");
        s.core.terminal=null;
        h.assertTrue(!codex.clickMenuButton(s.player,30)&&codex.codex.hollow,"Revoked session modified the codex");
        s.player.closeContainer();channel.finishAndReleaseAll();h.succeed();
    }
    @GameTest(template="spell_arena") public static void bindAndOpenAggregationRingFromTop(GameTestHelper h) throws Exception {
        var s=setup(h,false);var level=h.getLevel();
        var channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        s.player.connection.getConnection().channelActive(channel.pipeline().firstContext());
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(s.player.connection.getConnection());
        h.assertTrue(s.menu.clickMenuButton(s.player,ResonanceMenu.UNLINK),"Initial unlink failed");
        var center=s.core.getBlockPos().below(2);
        h.assertTrue(com.mcmagic.omnira.aggregation.AggregationStructure.assemble(level,center,net.minecraft.core.Direction.NORTH),"Ring not assembled");
        s.player.setShiftKeyDown(true);
        var pos=s.core.getBlockPos();
        s.player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(s.player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false)));
        s.player.setShiftKeyDown(false);
        h.assertTrue(s.core.terminal!=null&&s.core.menuPosition().equals(center),"Unable to bind from top");
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Cannot open ring from top");
        h.assertTrue(s.player.containerMenu instanceof com.mcmagic.omnira.aggregation.AggregationRingMenu&&s.player.containerMenu.stillValid(s.player),"Wrong or invalid ring menu");
        var nativeMenu=s.player.containerMenu;
        var otherCenter=center.east(4);
        h.assertTrue(com.mcmagic.omnira.aggregation.AggregationStructure.assemble(level,otherCenter,net.minecraft.core.Direction.NORTH),"Second ring not assembled");
        var other=(com.mcmagic.omnira.aggregation.AggregationRingBlockEntity)level.getBlockEntity(otherCenter);
        var otherMenu=new com.mcmagic.omnira.aggregation.AggregationRingMenu(99,s.player.getInventory(),other);
        h.assertTrue(!otherMenu.stillValid(s.player),"Remote session leaked to a second machine of the same type");
        s.player.tick();s.player.doTick();
        h.assertTrue(s.player.containerMenu==nativeMenu,"Remote ring closed during player tick");
        var machine=(com.mcmagic.omnira.aggregation.AggregationRingBlockEntity)level.getBlockEntity(center);
        machine.setItem(com.mcmagic.omnira.aggregation.AggregationLayout.OUTPUT,new ItemStack(Items.DIAMOND));
        nativeMenu.clicked(com.mcmagic.omnira.aggregation.AggregationLayout.OUTPUT,0,ClickType.PICKUP,s.player);
        h.assertTrue(nativeMenu.getCarried().is(Items.DIAMOND)&&s.player.getData(ModAttachments.MANA).current()==500,"Remote ring inventory or cost incorrect");
        level.removeBlock(center,false);
        h.assertTrue(!nativeMenu.stillValid(s.player),"Dismantled ring retained access");
        s.player.closeContainer();channel.finishAndReleaseAll();h.succeed();
    }
    @GameTest(template="spell_arena") public static void electricalAssemblyUsesNativeInventory(GameTestHelper h) throws Exception {
        var s=setup(h,false);var pos=s.ball.getBlockPos();var level=h.getLevel();
        var channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        s.player.connection.getConnection().channelActive(channel.pipeline().firstContext());
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(s.player.connection.getConnection());
        level.setBlockAndUpdate(pos,ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState());
        s.core.refresh();
        h.assertTrue(s.core.kind()==ResonanceCoreBlockEntity.MIXED,"Item inventory and rear FE input must be mixed");
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Powered assembly UI unavailable");
        h.assertTrue(s.player.containerMenu instanceof com.mcmagic.omnira.menu.ArcaneAssemblyTableMenu,"Wrong assembly UI");
        var menu=s.player.containerMenu;
        s.player.tick();s.player.doTick();
        h.assertTrue(s.player.containerMenu==menu&&menu.stillValid(s.player),"Remote assembly menu closed");
        menu.setCarried(new ItemStack(Items.DIAMOND));menu.clicked(6,0,ClickType.PICKUP,s.player);
        var work=((com.mcmagic.omnira.block.entity.AssemblyAccess)level.getBlockEntity(pos)).assembly();
        h.assertTrue(work.getItem(6).is(Items.DIAMOND)&&menu.getCarried().isEmpty(),"Failed remote deposit");
        menu.clicked(6,0,ClickType.PICKUP,s.player);
        h.assertTrue(work.getItem(6).isEmpty()&&menu.getCarried().is(Items.DIAMOND),"Failed remote withdrawal");
        s.player.closeContainer();channel.finishAndReleaseAll();h.succeed();
    }
    @GameTest(template="spell_arena") public static void openingRequiresHalfMaximumMana(GameTestHelper h){
        var s=setup(h,false);
        s.player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(500,1001));
        h.assertTrue(!s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER)&&s.player.containerMenu==s.menu,"Insufficient mana opened menu");
        h.assertTrue(s.player.getData(ModAttachments.MANA).current()==500,"Failed opening spent mana");
        h.assertTrue(!BranchAdvancementGameTests.recorded(s.player,"remote_access"),"Failed opening recorded branch");
        s.player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(500.5,1001));
        h.assertTrue(s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER)&&s.player.getData(ModAttachments.MANA).current()==0,"Exact half maximum should open and spend once");
        h.assertTrue(BranchAdvancementGameTests.recorded(s.player,"remote_access"),"Remote opening did not record branch");
        s.player.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void beaconRemoteConfirmation(GameTestHelper h) throws Exception {
        var s=setup(h,false);
        var pos=s.ball.getBlockPos();
        var conn=s.player.connection.getConnection();
        var channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        conn.channelActive(channel.pipeline().firstContext());
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(conn);
        s.player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),conn,s.player,
              net.minecraft.server.network.CommonListenerCookie.createInitial(s.player.getGameProfile(),false));
        h.getLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.BEACON.defaultBlockState());
        s.core.refresh();
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Cannot open bound beacon");
        var menu=(net.minecraft.world.inventory.BeaconMenu)s.player.containerMenu;
        var slot=menu.getSlot(0);slot.set(new ItemStack(Items.EMERALD));
        var packet=new net.minecraft.network.protocol.game.ServerboundSetBeaconPacket(java.util.Optional.of(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED),java.util.Optional.empty());
        s.player.connection.handleSetBeaconPacket(packet);
        h.assertTrue(slot.getItem().isEmpty() && net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED.equals(menu.getPrimaryEffect()),
                "Remote beacon did not consume payment and apply the selected effect");
        s.core.terminal=null;
        slot.set(new ItemStack(Items.EMERALD));
        s.player.connection.handleSetBeaconPacket(packet);
        h.assertTrue(slot.getItem().is(Items.EMERALD),"Revoked link still authorized beacon payment");
        s.player.closeContainer();channel.finishAndReleaseAll();h.succeed();
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
        h.assertTrue(s.menu.slots.isEmpty(),"Terminal selector exposed inventory slots");
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
    private static void checkRemoteStorage(GameTestHelper h,Setup s,int slot){
        var menu=(net.minecraft.world.inventory.ChestMenu)s.player.containerMenu;
        var storage=menu.getContainer();
        storage.setItem(slot,new ItemStack(Items.IRON_INGOT,3));
        h.assertTrue(s.player.distanceToSqr(s.core.getBlockPos().getCenter())>80*80,"Expected distant player");
        for(int tick=0;tick<6;tick++){
            s.player.tick();
            s.player.doTick();
            h.assertTrue(s.player.containerMenu==menu,"Player tick closed the remote storage");
        }
        menu.clicked(slot,0,ClickType.PICKUP,s.player);
        h.assertTrue(storage.getItem(slot).isEmpty()&&menu.getCarried().getCount()==3,"Remote pickup failed");
        menu.clicked(slot,0,ClickType.PICKUP,s.player);
        h.assertTrue(storage.getItem(slot).getCount()==3&&menu.getCarried().isEmpty(),"Remote deposit failed");
        h.assertTrue(s.player.getData(ModAttachments.MANA).current()==500,"Remote interaction charged extra mana");
    }
    @GameTest(template="spell_arena") public static void remoteBarrelSurvivesPlayerTicks(GameTestHelper h){
        var s=setup(h,false);
        h.getLevel().setBlockAndUpdate(s.ball.getBlockPos(),net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState());
        s.core.refresh();
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Barrel cannot open");
        checkRemoteStorage(h,s,26);
        s.player.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void doubleChestRemovalRevokesNativeAccess(GameTestHelper h){
        var s=setup(h,false);var pos=s.ball.getBlockPos();var level=h.getLevel();
        level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT));
        level.setBlockAndUpdate(pos.east(),net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
        s.core.refresh();
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Double chest cannot open");
        h.assertTrue(s.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu chest&&chest.getRowCount()==6,"Double chest did not retain six rows");
        checkRemoteStorage(h,s,0);
        checkRemoteStorage(h,s,53);
        level.removeBlock(pos.east(),false);
        h.assertTrue(Boolean.FALSE.equals(com.mcmagic.omnira.storage.RemoteContainerAccess.validity(s.player,s.player.containerMenu)),"Removed chest half retained access");
        s.player.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void reactionPoolNodeUsesControllerMenu(GameTestHelper h){
        var s=setup(h,false);var center=s.ball.getBlockPos().below();var level=h.getLevel();
        h.assertTrue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.place(level,center,net.minecraft.core.Direction.NORTH),"Pool placement failed");
        s.core.refresh();
        s.menu.broadcastChanges();
        h.assertTrue(s.core.kind()==6&&s.core.getBlockState().getValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE)==6&&s.menu.status(0)==6,"Pool mixed type not reflected in core and terminal");
        h.assertTrue(s.menu.clickMenuButton(s.player,0)&&s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER),"Pool cannot open");
        h.assertTrue(s.player.containerMenu instanceof com.mcmagic.omnira.alchemy.ReactionPoolMenu,"Pool did not open native UI");
        var menu=(com.mcmagic.omnira.alchemy.ReactionPoolMenu)s.player.containerMenu;
        for(int tick=0;tick<6;tick++){s.player.tick();s.player.doTick();}
        h.assertTrue(s.player.containerMenu==menu,"Pool closed");
        menu.setCarried(new ItemStack(Items.IRON_INGOT,3));
        menu.clicked(0,0,ClickType.PICKUP,s.player);
        h.assertTrue(menu.pool.items.getStackInSlot(0).getCount()==3,"Pool deposit failed");
        menu.clicked(0,0,ClickType.PICKUP,s.player);
        h.assertTrue(menu.pool.items.getStackInSlot(0).isEmpty()&&menu.getCarried().getCount()==3,"Pool pickup failed");
        menu.pool.fluids.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(menu.clickMenuButton(s.player,0),"Pool ignored remote fluid button");
        s.player.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void createToolbox(GameTestHelper h) throws Exception {
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        var s=setup(h,false);var level=h.getLevel();var pos=s.ball.getBlockPos();
        var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:brown_toolbox"));
        h.assertTrue(block!=net.minecraft.world.level.block.Blocks.AIR,"Missing toolbox");
        level.setBlockAndUpdate(pos,block.defaultBlockState());s.core.refresh();
        var channel=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());
        s.player.connection.getConnection().channelActive(channel.pipeline().firstContext());
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(s.player.connection.getConnection());
        boolean select=s.menu.clickMenuButton(s.player,0);
        boolean opened=s.menu.clickMenuButton(s.player,ResonanceMenu.OPEN_CONTAINER);
        h.assertTrue(opened,"Toolbox open="+opened+" selected="+select+" core kind="+s.core.kind()+" menu kind="+s.menu.kind()+" mana="+s.player.getData(ModAttachments.MANA).current());
        var menu=s.player.containerMenu;
        h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.MENU.getKey(menu.getType()).toString().equals("create:toolbox"),"Not the toolbox menu");
        for(int tick=0;tick<6;tick++){s.player.tick();s.player.doTick();}
        h.assertTrue(s.player.containerMenu==menu,"Toolbox closed");
        menu.setCarried(new ItemStack(Items.IRON_INGOT,3));
        menu.clicked(0,0,ClickType.PICKUP,s.player);
        h.assertTrue(menu.getSlot(0).getItem().getCount()==3&&menu.getCarried().isEmpty(),"Toolbox deposit failed");
        menu.clicked(0,0,ClickType.PICKUP,s.player);
        h.assertTrue(menu.getCarried().getCount()==3&&menu.getSlot(0).getItem().isEmpty(),"Toolbox pickup failed");
        s.player.closeContainer();channel.finishAndReleaseAll();h.succeed();
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
        for(var offset:com.mcmagic.omnira.world.dimension.DreamRitual.ANCHORS)h.assertTrue(((CrystalPedestalBlockEntity)h.getLevel().getBlockEntity(center.offset(offset[0],0,offset[1]))).getItem(0).isEmpty(),"Offering not consumed");
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
