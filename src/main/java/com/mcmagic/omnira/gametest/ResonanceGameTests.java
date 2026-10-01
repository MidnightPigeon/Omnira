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
    private record Setup(net.minecraft.world.entity.player.Player player,ResonanceMenu menu,CrystalBallBlockEntity ball,ResonanceCoreBlockEntity core) {}
    private static Setup setup(GameTestHelper h,boolean liquid) {
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000));
        var terminal=new ItemStack(ModItems.RESONANCE_TERMINAL.get());p.getInventory().setItem(0,terminal);ResonanceTerminalItem.authorize(terminal,p);
        var base=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(base,(liquid?ModBlocks.LIQUID_CRYSTAL_BALL:ModBlocks.CRYSTAL_BALL).get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(base.above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var core=(ResonanceCoreBlockEntity)h.getLevel().getBlockEntity(base.above());core.owner=p.getUUID();
        var tag=ResonanceTerminalItem.data(terminal);core.terminal=tag.getUUID("Terminal");
        var link=new net.minecraft.nbt.CompoundTag();link.putUUID("Identity",core.identity);link.putString("Dimension",h.getLevel().dimension().location().toString());link.putLong("Pos",core.getBlockPos().asLong());link.putInt("Kind",liquid?3:2);tag.put("Link0",link);ResonanceTerminalItem.save(terminal,tag);core.refresh();
        var menu=new ResonanceMenu(1,p.getInventory(),0);h.assertTrue(menu.clickMenuButton(p,0),"Remote container could not open");
        return new Setup(p,menu,(CrystalBallBlockEntity)h.getLevel().getBlockEntity(base),core);
    }
    @GameTest(template="spell_arena") public static void paidItemsAndSourceLock(GameTestHelper h) {
        var s=setup(h,false);s.ball.setItem(0,new ItemStack(Items.STONE,20));
        s.player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(250,1000));
        s.menu.clicked(0,0,ClickType.PICKUP,s.player);
        h.assertTrue(s.menu.getCarried().getCount()==2 && s.ball.getItem(0).getCount()==18 && s.player.getData(ModAttachments.MANA).current()==50,"Per-item affordability/debit incorrect");
        s.menu.clicked(0,0,ClickType.PICKUP,s.player);
        h.assertTrue(s.menu.getCarried().getCount()==2 && s.ball.getItem(0).getCount()==18,"Unaffordable insertion executed");
        s.menu.setCarried(ItemStack.EMPTY);s.menu.clicked(55,0,ClickType.PICKUP,s.player);
        h.assertTrue(s.player.getInventory().getItem(0).is(ModItems.RESONANCE_TERMINAL.get()) && s.menu.getCarried().isEmpty(),"Held terminal moved");
        s.player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(500,1000));s.menu.setCarried(new ItemStack(Items.DIRT,3));
        s.menu.clicked(1,0,ClickType.PICKUP,s.player);
        h.assertTrue(s.ball.getItem(1).getCount()==3 && s.player.getData(ModAttachments.MANA).current()==200,"Insertion not charged");s.menu.removed(s.player);h.succeed();
    }
    @GameTest(template="spell_arena") public static void replacedCoreRejectsAccess(GameTestHelper h) {
        var s=setup(h,false);s.ball.setItem(0,new ItemStack(Items.DIAMOND));s.core.identity=java.util.UUID.randomUUID();
        s.menu.clicked(0,0,ClickType.PICKUP,s.player);
        h.assertTrue(s.menu.getCarried().isEmpty() && s.ball.getItem(0).is(Items.DIAMOND) && s.menu.kind()==0,"Replacement core reused link");
        var other=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(!ResonanceTerminalItem.authorize(s.player.getInventory().getItem(0),other),"Foreign terminal access allowed");s.menu.removed(s.player);h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=90) public static void fluidPrepaymentCompletionAndCancellation(GameTestHelper h) {
        var s=setup(h,true);var ball=(LiquidCrystalBallBlockEntity)s.ball;
        s.menu.slots.get(27).set(new ItemStack(Items.LAVA_BUCKET));s.menu.broadcastChanges();
        h.assertTrue(ball.tank.isEmpty(),"Bucket transferred before two seconds");
        for(int tick=1;tick<=44;tick++)h.runAtTickTime(tick,s.menu::broadcastChanges);
        h.runAtTickTime(2,()->h.assertTrue(s.player.getData(ModAttachments.MANA).current()==800 && ball.tank.isEmpty(),"Bucket must prepay 200 mana before transfer"));
        h.runAtTickTime(45,()->{
            h.assertTrue(ball.tank.getFluidAmount()==1000 && s.menu.slots.get(27).getItem().is(Items.BUCKET),"Bucket did not finish after transfer time");
            s.menu.slots.get(27).set(new ItemStack(Items.BUCKET));s.menu.broadcastChanges();
            h.assertTrue(s.player.getData(ModAttachments.MANA).current()==600,"Extraction not prepaid");
            s.menu.clicked(27,0,ClickType.PICKUP,s.player);s.menu.broadcastChanges();
        });
        h.runAtTickTime(70,()->{
            s.menu.broadcastChanges();h.assertTrue(ball.tank.getFluidAmount()==1000 && s.player.getData(ModAttachments.MANA).current()==600,"Cancelled transfer moved fluid or refunded mana");
            s.menu.removed(s.player);h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void ritualConsumesEightCoresAndRetainsCenter(GameTestHelper h) {
        ritual(h,false);
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
        h.assertTrue(menu.clickMenuButton(s.player,0),"Cross-dimensional chest unavailable");menu.clicked(0,0,ClickType.PICKUP,s.player);
        h.assertTrue(menu.getCarried().getCount()==3 && s.player.getData(ModAttachments.MANA).current()==700,"Cross-dimensional transfer lost items or bypassed fees");
        menu.removed(s.player);level.removeBlock(base.above(),false);level.removeBlock(base,false);h.succeed();
    }
}
