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
    @GameTest(template="spell_arena") public static void stormTotemRevivesBeforeAmberCapture(GameTestHelper h){
        for(var mode:new net.minecraft.world.level.GameType[]{net.minecraft.world.level.GameType.SURVIVAL,net.minecraft.world.level.GameType.CREATIVE,net.minecraft.world.level.GameType.SPECTATOR}){
            for(var hand:net.minecraft.world.InteractionHand.values()){
                var p=realPlayer(h);p.setGameMode(mode);p.setHealth(12);
                p.setItemInHand(hand,new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
                p.getInventory().setItem(5,new ItemStack(net.minecraft.world.item.Items.DIAMOND,3));
                top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("charm",0,new ItemStack(ModItems.SPACETIME_ANCHORING_SIGIL.get()));
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.PHASE_CONCEALMENT,100));
                com.mcmagic.omnira.spacetime.SpacetimeStorm.affect(h.getLevel(),p);
                h.assertTrue(p.isAlive()&&p.getHealth()==1&&p.getItemInHand(hand).isEmpty(),"Storm totem failed in "+mode+" / "+hand);
                h.assertTrue(p.getInventory().getItem(5).getCount()==3&&com.mcmagic.omnira.spacetime.AnchoringSigilItem.equipped(p),"Storm revival captured inventory or equipment");
                h.assertTrue(com.mcmagic.omnira.spacetime.AmberDirectory.get(h.getLevel()).grave(p.getUUID())==null,"Storm revival created death amber");
            }
        }
        var zombie=h.spawn(EntityType.ZOMBIE,5,2,5);zombie.setHealth(10);
        zombie.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        com.mcmagic.omnira.spacetime.SpacetimeStorm.affect(h.getLevel(),zombie);
        h.assertTrue(zombie.isAlive()&&!zombie.isRemoved()&&zombie.getHealth()==1&&zombie.getOffhandItem().isEmpty(),"Storm erased a totem-protected mob");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void overdrawIsIndependentAndNeverUsesTotem(GameTestHelper h){
        var p=realPlayer(h);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        var source=OmniraDamage.source(p,"overdraw");
        h.assertTrue(!((com.mcmagic.omnira.mixin.DamageRecordAccessor)p).omnira$tryTotem(source)&&p.getMainHandItem().getCount()==1&&p.getOffhandItem().getCount()==1,"Overdraw allowed native totem revival");
        OmniraDamage.overdraw(p);
        h.assertTrue(!p.isAlive()&&OmniraDamage.isOverdraw(p.getLastDamageSource())&&!OmniraDamage.isTrue(p.getLastDamageSource()),"Overdraw did not use independent fatal source");
        var text=p.getLastDamageSource().getLocalizedDeathMessage(p);
        h.assertTrue(text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents&&contents.getKey().equals("death.attack.omnira.overdraw"),"Overdraw death message incorrect");
        h.assertTrue(p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.TOTEM_OF_UNDYING))==0,"Overdraw consumed a revival totem");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void trueDamageTotemsUseNativeRevival(GameTestHelper h){
        for(var mode:new net.minecraft.world.level.GameType[]{net.minecraft.world.level.GameType.SURVIVAL,net.minecraft.world.level.GameType.CREATIVE,net.minecraft.world.level.GameType.SPECTATOR}){
            for(var hand:net.minecraft.world.InteractionHand.values()){
                var p=realPlayer(h);p.setGameMode(mode);p.setHealth(12);
                p.setItemInHand(hand,new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
                var source=OmniraDamage.source(p,"true_damage");
                OmniraDamage.trueDamage(p,3,source);
                h.assertTrue(p.getHealth()==9&&p.getItemInHand(hand).getCount()==1,"Nonlethal true damage consumed totem");
                p.setHealth(3);OmniraDamage.trueDamage(p,7,source);
                h.assertTrue(p.isAlive()&&p.getHealth()==1&&p.getItemInHand(hand).isEmpty(),"True damage totem failed in "+mode+" / "+hand);
                var regen=p.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
                var absorption=p.getEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);
                var fire=p.getEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
                h.assertTrue(regen!=null&&regen.getAmplifier()==1&&regen.getDuration()==900&&absorption!=null&&absorption.getAmplifier()==1&&absorption.getDuration()==100&&fire!=null&&fire.getDuration()==800,"Native revival effects changed");
                h.assertTrue(p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.TOTEM_OF_UNDYING))==1,"Native totem stat missing");
            }
        }
        var both=realPlayer(h);both.setHealth(3);
        for(var hand:net.minecraft.world.InteractionHand.values())both.setItemInHand(hand,new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        OmniraDamage.trueDamage(both,7,OmniraDamage.source(both,"true_damage"));
        h.assertTrue(both.getMainHandItem().isEmpty()&&both.getOffhandItem().getCount()==1&&both.getHealth()==1,"Totem hand order or single consumption changed");
        var nativeVoid=realPlayer(h);nativeVoid.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        h.assertTrue(!((com.mcmagic.omnira.mixin.DamageRecordAccessor)nativeVoid).omnira$tryTotem(nativeVoid.damageSources().fellOutOfWorld())&&nativeVoid.getOffhandItem().getCount()==1,"Vanilla void totem restriction changed");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void trueDamageTotemCancellationAndOverdraw(GameTestHelper h){
        var p=realPlayer(h);p.setHealth(3);p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        java.util.function.Consumer<LivingUseTotemEvent> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{OmniraDamage.trueDamage(p,7,OmniraDamage.source(p,"true_damage"));}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        h.assertTrue(!p.isAlive(),"Cancelled totem still revived player");
        var execute=realPlayer(h);execute.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        OmniraDamage.overdraw(execute);
        h.assertTrue(!execute.isAlive(),"Explicit execution incorrectly became ordinary totem-protected damage");
        var victim=realPlayer(h);var attacker=player(h);
        var spell=com.mcmagic.omnira.spell.SpellDamageSource.of(victim,net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL,attacker,attacker,"spell");
        var text=OmniraDamage.source(victim,spell).getLocalizedDeathMessage(victim);
        h.assertTrue(text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents&&contents.getKey().equals("death.attack.omnira.true_damage"),"Attributed true damage retained spell death text");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void absorptionCannotPayForTrueDamageOrPreventDeath(GameTestHelper h){
        var normal=realPlayer(h);normal.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        normal.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(200);
        normal.setHealth(20);normal.setAbsorptionAmount(100);
        h.assertTrue(normal.hurt(normal.damageSources().fellOutOfWorld(),3)&&normal.getHealth()==20&&normal.getAbsorptionAmount()==97,"Vanilla void damage control did not consume absorption");
        for(var mode:new net.minecraft.world.level.GameType[]{net.minecraft.world.level.GameType.SURVIVAL,net.minecraft.world.level.GameType.CREATIVE,net.minecraft.world.level.GameType.SPECTATOR}){
            var target=realPlayer(h);target.setGameMode(mode);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(200);
            target.setHealth(20);target.setAbsorptionAmount(100);target.invulnerableTime=20;
            var source=OmniraDamage.source(target,"true_damage");
            h.assertTrue(OmniraDamage.trueDamage(target,7,source)&&target.getHealth()==13&&target.getAbsorptionAmount()==100,"Absorption paid true damage in "+mode);
            h.assertTrue(OmniraDamage.trueDamage(target,13,source)&&target.getHealth()==0&&!target.isAlive()&&target.getAbsorptionAmount()==100,"Absorption prevented lethal true damage in "+mode);
        }
        var storm=realPlayer(h);storm.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        storm.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(200);
        storm.setHealth(100);storm.setAbsorptionAmount(100);
        com.mcmagic.omnira.spacetime.SpacetimeStorm.affect(h.getLevel(),storm);
        h.assertTrue(storm.getHealth()==60&&storm.getAbsorptionAmount()==100,"Storm consumed absorption instead of health");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void trueDamageBypassesCreativeAndSpectatorImmunity(GameTestHelper h){
        var creative=realPlayer(h);creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        creative.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(20);
        creative.setAbsorptionAmount(10);creative.invulnerableTime=20;
        creative.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,200,4));
        h.assertTrue(creative.isCreative()&&creative.getAbilities().invulnerable,"Creative setup lacks native invulnerability");
        float health=creative.getHealth();var source=OmniraDamage.source(creative,"true_damage");
        h.assertTrue(OmniraDamage.trueDamage(creative,3,source)&&creative.getHealth()==health-3&&creative.getAbsorptionAmount()==10,"Creative immunity blocked true damage");
        creative.setHealth(3);int[] deaths={0};
        java.util.function.Consumer<LivingDeathEvent> listener=e->{if(e.getEntity()==creative)deaths[0]++;};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try{h.assertTrue(OmniraDamage.trueDamage(creative,3,source)&&!creative.isAlive()&&deaths[0]==1,"Creative lethal damage did not trigger one death");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
        var spectator=realPlayer(h);spectator.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);health=spectator.getHealth();
        spectator.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.PHASE_CONCEALMENT,100));
        var jelly=com.mcmagic.omnira.space.sea.VoidSeaContent.JELLYFISH.get().create(h.getLevel());
        h.assertTrue(OmniraDamage.trueDamage(spectator,8,spectator.damageSources().explosion(jelly,jelly))&&spectator.getHealth()==health-8,"Spectator or phase immunity blocked jellyfish explosion");
        spectator.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);spectator.setHealth(100);
        com.mcmagic.omnira.spacetime.SpacetimeStorm.affect(h.getLevel(),spectator);
        h.assertTrue(spectator.getHealth()==60,"Spectator or phase immunity blocked storm");
        spectator.setHealth(3);source=OmniraDamage.source(spectator,"true_damage");
        h.assertTrue(OmniraDamage.trueDamage(spectator,3,source)&&!spectator.isAlive(),"Spectator lethal damage blocked");
        h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer realPlayer(GameTestHelper h){
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"real-damage-test");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;
        return p;
    }
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
        var count=new int[]{0};java.util.function.Consumer<LivingDeathEvent> death=e->{if(e.getEntity()==target){count[0]++;h.assertTrue(OmniraDamage.isOverdraw(e.getSource()),"Payment failure did not use overdraw type");}};
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
