package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.workstation.CondensationChannel;
import com.mcmagic.omnira.mana.ManaState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;

@GameTestHolder("omnira_condensation_input") @PrefixGameTestTemplate(false)
public final class CondensationInteractionGameTests {
    private static SimpleCondensationTableBlockEntity table(GameTestHelper h,boolean advanced){
        var pos=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(pos,(advanced?ModBlocks.ADVANCED_CONDENSATION_TABLE:ModBlocks.SIMPLE_CONDENSATION_TABLE).get().defaultBlockState());
        return (SimpleCondensationTableBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static FakePlayer player(GameTestHelper h,SimpleCondensationTableBlockEntity table){
        var p=new FakePlayer(h.getLevel(),new GameProfile(java.util.UUID.randomUUID(),"condense"));
        p.setPos(table.getBlockPos().getCenter().add(0,1,0));p.setXRot(90);p.setData(ModAttachments.MANA,new ManaState(500,500));return p;
    }
    @GameTest(template="spell_arena",timeoutTicks=240) public static void exactDurationDebitAndConcurrentLock(GameTestHelper h){
        var t=table(h,true);var p=player(h,t);var q=player(h,t);
        t.setItem(0,new ItemStack(ModItems.SPACETIME_KNOT.get()));
        h.assertTrue(t.work.start(p)&&!t.work.start(q),"Concurrent table work accepted");
        h.assertTrue(t.getUpdateTag(h.getLevel().registryAccess()).getBoolean("Condensing")
                &&!t.saveWithoutMetadata(h.getLevel().registryAccess()).contains("Condensing"),"Working visual not synced or incorrectly persisted");
        h.runAfterDelay(19,()->{t.work.tick();h.assertTrue(p.getData(ModAttachments.MANA).current()==500,"Charged before one second");});
        h.runAfterDelay(20,()->{t.work.tick();t.work.tick();h.assertTrue(p.getData(ModAttachments.MANA).current()==470,"First installment missing or duplicated");});
        h.runAfterDelay(199,()->{t.work.tick();h.assertTrue(t.getItem(0).is(ModItems.SPACETIME_KNOT.get())&&p.getData(ModAttachments.MANA).current()==230,"Incorrect installments or premature output");});
        h.runAfterDelay(200,()->{t.work.tick();h.assertTrue(t.getItem(0).is(ModItems.PARADOX_DUST.get())&&p.getData(ModAttachments.MANA).current()==200&&q.getData(ModAttachments.MANA).current()==500,"Wrong output or debit");h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=150) public static void shortClickAndLongHoldDoNotOverlap(GameTestHelper h){
        var t=table(h,false);var p=player(h,t);
        CondensationChannel.input(p,true,t.getBlockPos());CondensationChannel.input(p,false,t.getBlockPos());
        h.assertTrue(!t.work.active()&&t.isEmpty(),"Short click started condensation");
        for(int tick=1;tick<=110;tick++)h.runAtTickTime(tick,()->CondensationChannel.input(p,true,t.getBlockPos()));
        h.runAtTickTime(112,()->{
            CondensationChannel.input(p,false,t.getBlockPos());
            h.assertTrue(t.getItem(0).is(ModItems.SPIRITUAL_CRYSTAL.get())&&t.getItem(0).getCount()==1,"Long hold lost its output or failed");
            h.assertTrue(p.getData(ModAttachments.MANA).current()==400,"Simple table debit changed");
            CondensationChannel.input(p,true,t.getBlockPos());CondensationChannel.input(p,false,t.getBlockPos());
            h.assertTrue(t.isEmpty()&&p.getInventory().countItem(ModItems.SPIRITUAL_CRYSTAL.get())==1,"Short release did not collect");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=260) public static void manaFailureNeedsNewPress(GameTestHelper h){
        var t=table(h,true);var p=player(h,t);t.setItem(0,new ItemStack(ModItems.SPACETIME_KNOT.get()));
        for(int tick=1;tick<=230;tick++)h.runAtTickTime(tick,()->CondensationChannel.input(p,true,t.getBlockPos()));
        h.runAtTickTime(20,()->p.setData(ModAttachments.MANA,new ManaState(0,500)));
        h.runAtTickTime(25,()->{h.assertTrue(!t.work.active(),"Mana shortage did not cancel");p.setData(ModAttachments.MANA,new ManaState(500,500));});
        h.runAtTickTime(232,()->{
            h.assertTrue(!t.work.active()&&t.getItem(0).is(ModItems.SPACETIME_KNOT.get())&&p.getData(ModAttachments.MANA).current()==500,"Held input restarted after shortage");
            CondensationChannel.input(p,false,t.getBlockPos());CondensationChannel.input(p,true,t.getBlockPos());
        });
        for(int tick=233;tick<=241;tick++)h.runAtTickTime(tick,()->CondensationChannel.input(p,true,t.getBlockPos()));
        h.runAtTickTime(242,()->{h.assertTrue(t.work.active(),"Fresh press did not retry");CondensationChannel.stop(p);h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=80) public static void installmentsCancellationIgnoresDiscount(GameTestHelper h){
        var t=table(h,false);var p=player(h,t);
        p.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(10);
        p.setData(ModAttachments.MANA,new ManaState(20,500));
        h.assertTrue(t.work.start(p),"First installment should suffice to start");
        h.runAfterDelay(20,()->{
            t.work.tick();
            h.assertTrue(p.getData(ModAttachments.MANA).current()==0&&!t.work.active()&&t.isEmpty(),"Condensation accepted a discount or shortage produced output");
            p.setData(ModAttachments.MANA,new ManaState(100,500));h.assertTrue(t.work.start(p),"Restart failed");
        });
        h.runAfterDelay(40,()->{t.work.tick();t.work.cancel();h.assertTrue(p.getData(ModAttachments.MANA).current()==80&&t.isEmpty(),"Cancellation refunded invested mana");h.succeed();});
    }
    @GameTest(template="spell_arena") public static void ingredientCountsCancellationAndDiscount(GameTestHelper h){
        var t=table(h,true);var p=player(h,t);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.TIME_WARP_POINT.get(),10));
        CondensationChannel.shortClick(p,t);
        h.assertTrue(t.getItem(0).getCount()==8&&p.getMainHandItem().getCount()==2&&t.work.start(p),"Eight-item recipe placement failed");
        t.setItem(0,new ItemStack(ModItems.TIME_WARP_POINT.get(),7));t.work.tick();
        h.assertTrue(!t.work.active()&&!t.work.start(p)&&p.getData(ModAttachments.MANA).current()==500,"Changed ingredients charged mana");
        t.setItem(0,new ItemStack(ModItems.SPACETIME_KNOT.get()));p.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(10);
        p.setData(ModAttachments.MANA,new ManaState(29,500));h.assertTrue(!t.work.start(p),"Discount incorrectly lowered installment");
        p.setData(ModAttachments.MANA,new ManaState(30,500));h.assertTrue(t.work.start(p),"First installment rejected");t.work.cancel();
        h.assertTrue(t.createMenu(1,p.getInventory(),p)==null,"Condensation still opens UI");h.succeed();
    }
}
