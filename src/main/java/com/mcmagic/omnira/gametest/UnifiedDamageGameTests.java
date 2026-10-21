package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.combat.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.mana.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_unified_damage")
@PrefixGameTestTemplate(false)
public final class UnifiedDamageGameTests {
    private static FakePlayer player(GameTestHelper h){
        return new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"damage-test"));
    }
    @GameTest(template="spell_arena") public static void stormAndAmberRemainSpecial(GameTestHelper h){
        TemporalAmberGameTests.stormDeathSealsOnceAndNextStormErases(h);
    }
    @GameTest(template="spell_arena") public static void registryAndFormats(GameTestHelper h){
        var p=player(h);var registry=h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
        h.assertTrue(registry.getHolder(OmniraDamage.TRUE_DAMAGE).isPresent(),"Missing true damage registry entry");
        h.assertTrue(!registry.containsKey(net.minecraft.resources.ResourceLocation.parse("omnira:time_ending")),"Separate time-ending damage type retained");
        for(var attribute:java.util.List.of(ModAttributes.SPELL_POWER,ModAttributes.MANA_REGEN,ModAttributes.DAMAGE_DEALT,ModAttributes.DAMAGE_TAKEN,ModAttributes.GLIDING_SPEED))
            h.assertTrue(attribute.value() instanceof net.neoforged.neoforge.common.PercentageAttribute&&p.getAttributeValue(attribute)==1,"Percentage units or authored base changed");
        var source=OmniraDamage.source(p,"time_ending");
        h.assertTrue(source.is(OmniraDamage.TRUE_DAMAGE)&&source.getLocalizedDeathMessage(p).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                &&text.getKey().equals("death.attack.omnira.time_ending"),"Time-ending reason did not retain its message");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void incomingReductionsAdd(GameTestHelper h){
        var p=player(h);for(var entry:ArmorContent.PLATE.entrySet())p.setItemSlot(entry.getKey().getSlot(),new ItemStack(entry.getValue().get()));
        p.getAttribute(ModAttributes.DAMAGE_TAKEN).setBaseValue(.8);
        var event=new LivingDamageEvent.Pre(p,new DamageContainer(p.damageSources().magic(),10));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        h.assertTrue(Math.abs(event.getNewDamage()-6)<.001,"Armor and attribute reductions multiplied instead of adding");
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(com.mcmagic.omnira.space.sea.StellarCraftContent.SHIELD.get()));
        p.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
        h.assertTrue(Math.abs(CombatRules.incoming(p)-.35)<.001,"Raised field reduction did not join same factor");h.succeed();
    }
    @GameTest(template="spell_arena") public static void outgoingAndIncomingAreIndependent(GameTestHelper h){
        var attacker=player(h);var victim=player(h);victim.getAttribute(ModAttributes.DAMAGE_TAKEN).setBaseValue(1.5);
        attacker.getAttribute(ModAttributes.DAMAGE_DEALT).setBaseValue(0);
        var event=new LivingDamageEvent.Pre(victim,new DamageContainer(victim.damageSources().playerAttack(attacker),10));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.getNewDamage()==0,"Vulnerability revived zero outgoing damage");
        attacker.getAttribute(ModAttributes.DAMAGE_DEALT).setBaseValue(1.2);victim.getAttribute(ModAttributes.DAMAGE_TAKEN).setBaseValue(.8);
        victim.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.ELEMENTAL.ordinal(),0));victim.setData(ModAttachments.MANA,new ManaState(100,100));
        event=new LivingDamageEvent.Pre(victim,new DamageContainer(victim.damageSources().playerAttack(attacker),10));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        h.assertTrue(Math.abs(event.getNewDamage()-7.6)<.001&&victim.getData(ModAttachments.MANA).current()==80,"Flat reduction order or payment changed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void trueDamageIgnoresDefenseAndReportsActualLoss(GameTestHelper h){
        var victim=player(h);victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(20);victim.setAbsorptionAmount(10);
        victim.invulnerableTime=20;victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,200,4));
        var attacker=player(h);attacker.getAttribute(ModAttributes.DAMAGE_DEALT).setBaseValue(0);
        var source=OmniraDamage.source(victim,victim.damageSources().playerAttack(attacker));
        int[] notifications={0};
        java.util.function.Consumer<LivingDamageEvent.Post> listener=e->{if(e.getEntity()==victim){notifications[0]++;h.assertTrue(e.getSource().is(OmniraDamage.TRUE_DAMAGE)&&e.getNewDamage()==2,"Incorrect true damage notification");}};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try{
            for(double multiplier:new double[]{0,2}){
                victim.getAttribute(ModAttributes.DAMAGE_TAKEN).setBaseValue(multiplier);float before=victim.getHealth();
                h.assertTrue(OmniraDamage.trueDamage(victim,2,source)&&victim.getHealth()==before-2&&victim.getAbsorptionAmount()==10,"Defense modified fixed true damage");
            }
        }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
        h.assertTrue(notifications[0]==2&&victim.getKillCredit()==attacker&&victim.getLastDamageSource()==source,"Notifications or attacker attribution missing");
        var pre=new LivingDamageEvent.Pre(victim,new DamageContainer(source,2));pre.setNewDamage(99);CombatRules.apply(pre,true);
        h.assertTrue(pre.getNewDamage()==2,"Standard true damage hook applies vulnerability");h.succeed();
    }
    @GameTest(template="spell_arena") public static void paymentAndExecution(GameTestHelper h){
        var p=player(h);p.setHealth(10);p.getAttribute(ModAttributes.DAMAGE_TAKEN).setBaseValue(0);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.PHASE_CONCEALMENT,100));
        h.assertTrue(OmniraDamage.payHealth(p,3,"true_damage")&&p.getHealth()==7,"Self payment was evaded or scaled");
        var target=h.spawn(EntityType.COW,5,2,5);target.setHealth(7);
        var count=new int[]{0};java.util.function.Consumer<LivingDeathEvent> death=e->{if(e.getEntity()==target){count[0]++;h.assertTrue(e.getSource().is(OmniraDamage.TRUE_DAMAGE),"Execution used another damage type");}};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(death);
        try{h.assertTrue(!OmniraDamage.payHealth(target,8,"true_damage")&&!target.isAlive()&&count[0]==1,"Insufficient health did not execute exactly once");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(death);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void sourceContextAndPureTrueKillLoot(GameTestHelper h){
        var p=player(h);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.PHASE_CONCEALMENT,100));
        var cow=h.spawn(EntityType.COW,4,2,4);
        h.assertTrue(!OmniraDamage.trueDamage(p,2,p.damageSources().mobAttack(cow)),"Single-target phase protection lost");
        var explosion=OmniraDamage.source(p,p.damageSources().explosion(cow,cow));
        h.assertTrue(explosion.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)&&OmniraDamage.trueDamage(p,2,explosion),"Explosion context lost on shared type");
        cow.setHealth(2);OmniraDamage.trueDamage(cow,2,cow.damageSources().playerAttack(p));
        h.assertTrue(cow.getKillCredit()==p,"Pure true kill has no player credit");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,cow.getBoundingBox().inflate(3)).isEmpty(),"Pure true kill drops no experience");h.succeed();
    }
}
