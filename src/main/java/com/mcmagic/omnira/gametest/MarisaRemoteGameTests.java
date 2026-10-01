package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.ResonanceCoreBlock;
import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import com.mcmagic.omnira.item.ResonanceTerminalItem;
import com.mcmagic.omnira.menu.ResonanceMenu;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.shop.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_marisa_remote") @PrefixGameTestTemplate(false)
public final class MarisaRemoteGameTests {
    private record Setup(ServerPlayer player,MarisaOrbBlockEntity orb,ResonanceCoreBlockEntity core,ItemStack terminal){}
    private static Setup setup(GameTestHelper h,boolean crossDimension){
        var world=crossDimension?h.getLevel().getServer().getLevel(Level.NETHER):h.getLevel();
        var profile=new GameProfile(UUID.randomUUID(),"remote-shop");
        var p=new ServerPlayer(world.getServer(),world,profile,ClientInformation.createDefault());
        p.connection=new FakePlayer(world,profile).connection;p.setGameMode(GameType.SURVIVAL);
        p.setPos(h.absolutePos(new BlockPos(5,3,5)).getCenter().add(100,0,0));
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(0,1000));
        var pos=h.absolutePos(new BlockPos(5,3,5));
        h.getLevel().setBlockAndUpdate(pos,KirisameContent.ORB.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var core=(ResonanceCoreBlockEntity)h.getLevel().getBlockEntity(pos.above());core.owner=p.getUUID();core.refresh();
        h.assertTrue(core.kind()==4&&core.getBlockState().getValue(ResonanceCoreBlock.MODE)==1,"Unbound shop is not red");
        var terminal=new ItemStack(ModItems.RESONANCE_TERMINAL.get());p.getInventory().setItem(0,terminal);
        ResonanceTerminalItem.authorize(terminal,p);
        var tag=ResonanceTerminalItem.data(terminal);core.terminal=tag.getUUID("Terminal");
        var link=new net.minecraft.nbt.CompoundTag();link.putUUID("Identity",core.identity);
        link.putString("Dimension",h.getLevel().dimension().location().toString());link.putLong("Pos",core.getBlockPos().asLong());link.putInt("Kind",4);
        tag.put("Link0",link);ResonanceTerminalItem.save(terminal,tag);core.refresh();
        h.assertTrue(core.getBlockState().getValue(ResonanceCoreBlock.MODE)==4,"Bound shop is not yellow");
        return new Setup(p,(MarisaOrbBlockEntity)h.getLevel().getBlockEntity(pos),core,terminal);
    }
    private static MerchantMenu open(GameTestHelper h,Setup s){
        var menu=new ResonanceMenu(1,s.player.getInventory(),0);s.player.containerMenu=menu;
        h.assertTrue(menu.clickMenuButton(s.player,0)&&menu.kind()==4&&menu.status(0)==4,"Shop node cannot be selected");
        h.assertTrue(menu.clickMenuButton(s.player,ResonanceMenu.OPEN_SHOP)&&s.player.containerMenu instanceof MerchantMenu,"Shop menu did not open");
        var trade=(MerchantMenu)s.player.containerMenu;
        h.assertTrue(trade.stillValid(s.player),"Remote merchant access rejected");return trade;
    }
    @GameTest(template="spell_arena") public static void remotePurchaseSharesStockWithoutMana(GameTestHelper h){
        var s=setup(h,true);var direct=s.orb.connect(s.player);var menu=open(h,s);
        h.assertTrue(menu.getOffers()==direct.getOffers(),"Remote access rerolled player stock");
        var offer=menu.getOffers().getFirst();menu.setSelectionHint(0);
        menu.getSlot(0).set(offer.getCostA().copy());
        h.assertTrue(!menu.quickMoveStack(s.player,2).isEmpty(),"Remote paid trade failed");
        h.assertTrue(offer.getUses()==1&&s.player.getData(ModAttachments.MANA).current()==0,"Remote trade charged mana or lost shared usage");
        s.player.closeContainer();direct.close();h.succeed();
    }
    @GameTest(template="spell_arena") public static void invalidationAndUnlink(GameTestHelper h){
        var s=setup(h,false);var menu=open(h,s);
        s.player.getInventory().setItem(0,ItemStack.EMPTY);
        h.assertTrue(!menu.stillValid(s.player),"Removed terminal still grants access");s.player.closeContainer();
        s.player.getInventory().setItem(0,s.terminal);menu=open(h,s);
        s.core.terminal=null;s.core.refresh();
        h.assertTrue(!menu.stillValid(s.player)&&s.core.getBlockState().getValue(ResonanceCoreBlock.MODE)==1,"Unbound node still grants access");s.player.closeContainer();
        s.core.terminal=ResonanceTerminalItem.data(s.terminal).getUUID("Terminal");menu=open(h,s);
        h.getLevel().removeBlock(s.orb.getBlockPos(),false);
        h.assertTrue(!menu.stillValid(s.player),"Removed shop still trades");s.player.closeContainer();h.succeed();
    }
    @GameTest(template="spell_arena") public static void realBindingAndForeignAccess(GameTestHelper h){
        var s=setup(h,false);s.core.terminal=null;
        var tag=ResonanceTerminalItem.data(s.terminal);tag.remove("Link0");ResonanceTerminalItem.save(s.terminal,tag);
        s.player.setShiftKeyDown(true);
        var hit=new BlockHitResult(s.core.getBlockPos().getCenter(),Direction.UP,s.core.getBlockPos(),false);
        s.terminal.getItem().useOn(new UseOnContext(s.player,InteractionHand.MAIN_HAND,hit));
        h.assertTrue(s.core.terminal!=null&&s.core.getBlockState().getValue(ResonanceCoreBlock.MODE)==4,"Shop binding through item failed");
        var stranger=h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(!s.core.permits(stranger),"Foreign player can access shop link");
        s.player.setShiftKeyDown(false);var menu=open(h,s);
        s.core.identity=UUID.randomUUID();h.assertTrue(!menu.stillValid(s.player),"Replaced node identity retained access");
        s.player.closeContainer();h.succeed();
    }
}
