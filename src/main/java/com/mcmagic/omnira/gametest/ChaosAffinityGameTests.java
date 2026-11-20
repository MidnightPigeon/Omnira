package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import com.mcmagic.omnira.item.staff.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_chaos_affinity") @PrefixGameTestTemplate(false)
public final class ChaosAffinityGameTests {
    private static FakePlayer player(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"chaos-affinity"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(h.absoluteVec(new Vec3(5.5,4,5.5)));p.setYRot(0);p.setXRot(0);
        AffinityCommands.apply(p,Affinity.CHAOS);p.setData(ModAttachments.MANA,new ManaState(500,500));return p;
    }
    private static void roll(FakePlayer p,boolean success){
        for(long seed=0;;seed++)if((net.minecraft.util.RandomSource.create(seed).nextFloat()<.25F)==success){p.getRandom().setSeed(seed);return;}
    }
    private static void advance(FakePlayer p,int ticks){
        for(int i=0;i<ticks;i++){p.tickCount++;ChaosAffinity.tick(new PlayerTickEvent.Post(p));}
    }
    private static long spells(FakePlayer p){return p.level().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(64),e->e.ownedBy(p)).size();}
    private static ItemStack ordinary(){
        var s=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
        s.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(ElementType.AIR,ElementType.AIR));
        s.set(ModDataComponents.SPELL_PAYLOAD,SpellPayload.EMPTY);return s;
    }
    private static ItemStack grid(FakePlayer p,ItemStack staff){
        p.setItemInHand(InteractionHand.MAIN_HAND,staff);
        var grid=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());
        grid.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(ordinary(),ordinary(),ordinary())));
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("crystal_grid",0,grid);return grid;
    }
    @GameTest(template="spell_arena") public static void statsPerfectingAndCommands(GameTestHelper h){
        var p=player(h);AffinityEffects.refresh(p);AffinityEffects.refresh(p);
        h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==75 && Math.abs(p.getAttributeValue(ModAttributes.SPELL_POWER)-1.1)<.001,"Chaos attributes missing or duplicated");
        h.assertTrue(SpellCooldowns.ticks(p,100,1)==125,"Cooldown penalty missing");
        p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.CHAOS.ordinal(),0,true));AffinityEffects.refresh(p);
        h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==200 && SpellCooldowns.ticks(p,100,1)==100,"Perfecting retained negative effects");
        h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.SPELL_POWER)-1.2)<.001,"Perfected power lost");
        AffinityCommands.apply(p,Affinity.NONE);
        h.assertTrue(p.getData(ModAttachments.AFFINITY).equals(AffinityState.NONE),"Command invoked renunciation");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void echoesBypassCooldownWithoutRecursionOrExtraConsumption(GameTestHelper h){
        var p=player(h);p.setPos(p.position().add(0,60,0));p.setXRot(-90);
        var staff=StaffAssembly.basic().create();var grid=grid(p,staff);long before=spells(p);
        roll(p,true);((com.mcmagic.omnira.item.StaffItem)staff.getItem()).castHeld(p,staff);
        double mana=p.getData(ModAttachments.MANA).current();int cursor=grid.getOrDefault(ModDataComponents.GRID_CURSOR,0);
        h.assertTrue(spells(p)==before+1 && p.getCooldowns().isOnCooldown(staff.getItem()),"Initial staff cast failed");
        advance(p,3);h.assertTrue(spells(p)==before+1,"Echo fired too early");
        advance(p,1);h.assertTrue(spells(p)==before+2,"Staff or chain cooldown blocked echo");
        advance(p,20);
        h.assertTrue(spells(p)==before+2 && p.getData(ModAttachments.MANA).current()==mana && grid.getOrDefault(ModDataComponents.GRID_CURSOR,0)==cursor,"Echo recurred, charged mana or advanced grid");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void multiShotRollsEachSpell(GameTestHelper h){
        var p=player(h);p.setPos(p.position().add(0,60,0));p.setXRot(-90);var base=StaffAssembly.basic();
        var staff=new StaffAssembly(base.shaft(),base.reinforcement(),new ItemStack(ModItems.MULTI_ARCANE_TIP.get()),List.of()).create();
        var grid=grid(p,staff);long before=spells(p);
        roll(p,false);((com.mcmagic.omnira.item.StaffItem)staff.getItem()).castHeld(p,staff);
        advance(p,4);h.assertTrue(spells(p)==before+1,"Failed lottery echoed");
        advance(p,2);roll(p,true);StaffEvents.tick(new PlayerTickEvent.Post(p));
        advance(p,4);h.assertTrue(spells(p)==before+3,"Second shot did not independently echo");
        advance(p,2);StaffEvents.tick(new PlayerTickEvent.Post(p));
        h.assertTrue(spells(p)==before+3 && grid.getOrDefault(ModDataComponents.GRID_CURSOR,0)==2,"Echo advanced sequence or added a third shot");h.succeed();
    }
    @GameTest(template="spell_arena") public static void singleUseCrystalEchoConsumesOnlyOnce(GameTestHelper h){
        var p=player(h);p.setPos(p.position().add(0,60,0));p.setXRot(-90);var crystal=ordinary();crystal.setCount(2);
        p.setItemInHand(InteractionHand.MAIN_HAND,crystal);roll(p,true);
        h.assertTrue(crystal.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction(),"Crystal cast failed");
        double mana=p.getData(ModAttachments.MANA).current();
        h.assertTrue(crystal.getCount()==1 && spells(p)==1,"Initial crystal consumption incorrect");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);advance(p,4);
        h.assertTrue(crystal.getCount()==1 && spells(p)==2 && p.getData(ModAttachments.MANA).current()==mana,"Echo consumed another crystal or required it held");h.succeed();
    }
    @GameTest(template="spell_arena") public static void industrialWheelEchoKeepsAutomaticMode(GameTestHelper h){
        if(!com.mcmagic.omnira.item.IndustrialWheelItem.available()){h.succeed();return;}
        var p=player(h);p.setPos(p.position().add(0,60,0));p.setXRot(-90);
        var staff=new ItemStack(ModItems.INDUSTRIAL_WHEEL.get());var grid=grid(p,staff);roll(p,true);
        try{
            com.mcmagic.omnira.item.IndustrialWheelItem.toggle(p,InteractionHand.MAIN_HAND);
            double mana=p.getData(ModAttachments.MANA).current();int cursor=grid.getOrDefault(ModDataComponents.GRID_CURSOR,0);
            h.assertTrue(spells(p)==1,"Wheel initial cast failed");advance(p,4);
            h.assertTrue(spells(p)==2 && p.getData(ModAttachments.MANA).current()==mana && grid.getOrDefault(ModDataComponents.GRID_CURSOR,0)==cursor,"Wheel echo mischarged or advanced sequence");
            com.mcmagic.omnira.item.IndustrialWheelItem.tick(new PlayerTickEvent.Post(p));
            h.assertTrue(spells(p)==2,"Wheel ignored its own ongoing cooldown");
        }finally{com.mcmagic.omnira.item.IndustrialWheelItem.stop(p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void beamEchoAndBacklashImmunity(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        var target=h.spawn(EntityType.IRON_GOLEM,5,4,10);target.setNoAi(true);
        try{
            h.assertTrue(ChaosBeam.start(p,InteractionHand.MAIN_HAND,ItemStack.EMPTY,SpellPayload.EMPTY,1.1,20),"Channel failed");
            for(int i=0;i<ChaosBeam.TRAVEL_TICKS;i++){p.tickCount++;roll(p,true);ChaosBeam.tick(new PlayerTickEvent.Post(p));}
            h.assertTrue(Math.abs(target.getHealth()-94.5)<.001 && p.getHealth()==20 && p.getData(ModAttachments.MANA).current()==432,"Initial effect or damage-only immunity wrong");
            p.getCooldowns().addCooldown(Items.STICK,100);advance(p,3);
            h.assertTrue(Math.abs(target.getHealth()-94.5)<.001,"Pulse echo early");advance(p,1);
            h.assertTrue(Math.abs(target.getHealth()-89)<.001 && p.getHealth()==20 && Math.abs(p.getData(ModAttachments.MANA).current()-388.8)<.001,"Pulse echo blocked, mischarged or damaged caster");
            advance(p,4);h.assertTrue(Math.abs(target.getHealth()-89)<.001,"Pulse echo recurred");
            p.setData(ModAttachments.MANA,new ManaState(10,500));ChaosBeam.backlash(p);
            h.assertTrue(p.isAlive() && p.getHealth()==20 && p.getData(ModAttachments.MANA).current()==0,"Minimum mana backlash or exact-zero survival incorrect");
            p.tickCount=20;ChaosBeam.tick(new PlayerTickEvent.Post(p));
            h.assertTrue(!ChaosBeam.active(p) && p.isAlive(),"Immunity bypassed normal casting cost");
        }finally{ChaosBeam.stop(p);target.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void manaBacklashStillOverdrawsPerfectedAffinity(GameTestHelper h){
        for(boolean perfected:new boolean[]{false,true}){
            var p=player(h);p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.CHAOS.ordinal(),0,perfected));AffinityEffects.refresh(p);
            p.setData(ModAttachments.MANA,new ManaState(9,500));ChaosBeam.backlash(p);
            h.assertTrue(!p.isAlive() && p.getData(ModAttachments.MANA).current()==0,"Chaos affinity prevented mana overdraw death");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void deathPersistenceAndExactCountdown(GameTestHelper h){
        var p=player(h);p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.CHAOS.ordinal(),1200));
        for(int i=0;i<17;i++)AffinityEffects.tick(p);
        var saved=p.getData(ModAttachments.AFFINITY);p.setHealth(0);
        AffinityEffects.died(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(p,p.damageSources().generic()));
        for(int i=0;i<100;i++)AffinityEffects.tick(p);
        h.assertTrue(p.getData(ModAttachments.AFFINITY).equals(saved),"Death lost or advanced countdown");
        var restored=player(h);restored.restoreFrom(p,false);
        h.assertTrue(restored.getData(ModAttachments.AFFINITY).equals(saved),"Respawn failed to copy penalty");
        var tag=AffinityState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,saved).getOrThrow();
        restored.setData(ModAttachments.AFFINITY,AffinityState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,tag).getOrThrow());
        for(int i=0;i<1182;i++)AffinityEffects.tick(restored);
        h.assertTrue(restored.getData(ModAttachments.AFFINITY).penaltyTicks()==1,"Countdown duration changed");AffinityEffects.tick(restored);
        h.assertTrue(restored.getData(ModAttachments.AFFINITY).equals(AffinityState.NONE),"Penalty did not finish");h.succeed();
    }
    @GameTest(template="spell_arena") public static void retributionUsesBossDamageAndHitsBystanders(GameTestHelper h){
        var p=player(h);h.getLevel().addNewPlayer(p);
        var target=h.spawn(EntityType.IRON_GOLEM,5,4,10);target.setNoAi(true);
        var helmet=new ItemStack(Items.IRON_HELMET);p.setItemSlot(EquipmentSlot.HEAD,helmet);
        p.getAttribute(ModAttributes.SPELL_POWER).setBaseValue(100);
        try{
            var start=p.getBoundingBox().getCenter().add(0,0,-3);
            com.mcmagic.omnira.weaver.WeaverCombat.retributionRay(h.getLevel(),start,new Vec3(0,0,1));
            h.assertTrue(p.getHealth()==17 && target.getHealth()==97,"Ray missed owner/bystander or used caster power");
            h.assertTrue(helmet.getDamageValue()==(int)Math.ceil(helmet.getMaxDamage()*.1),"Boss durability loss missing");
            ChaosAffinity.retribution(p);h.assertTrue(p.getHealth()==14,"Random sphere ray missed its player or invulnerability blocked hit");
        }finally{target.discard();p.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void worldRotationEchoIgnoresItsOwnGate(GameTestHelper h){
        var p=player(h);roll(p,true);
        h.assertTrue(com.mcmagic.omnira.spell.world.WorldRotation.cast(p),"Rotation failed");
        long ready=p.getData(ModAttachments.WORLD_ROTATION_READY);p.setHealth(1);
        advance(p,4);
        h.assertTrue(p.getHealth()==20 && p.getData(ModAttachments.MANA).current()==0 && p.getData(ModAttachments.WORLD_ROTATION_READY)==ready,"Rotation echo failed or reset own gate");
        com.mcmagic.omnira.spell.world.WorldRotation.cancel(p.getUUID());h.succeed();
    }
}
