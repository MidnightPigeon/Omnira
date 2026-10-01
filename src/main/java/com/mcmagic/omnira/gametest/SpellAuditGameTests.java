package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_spell_audit")
@PrefixGameTestTemplate(false)
public final class SpellAuditGameTests {
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h) {
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"audit-test"));
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(8,3,8)));p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);return p;
    }
    private static String key(DamageSource source,LivingEntity victim) {
        return ((net.minecraft.network.chat.contents.TranslatableContents)source.getLocalizedDeathMessage(victim).getContents()).getKey();
    }
    @GameTest(template="spell_arena")
    public static void allModifierPairsAndElements(GameTestHelper h) {
        Item[] modifiers={Items.AIR,Items.CLOCK,Items.IRON_BLOCK,ModItems.SPIRITUAL_CRYSTAL.get()};
        Item[] elements={ModItems.SHARP_BREATH.get(),ModItems.HEALING_DEW.get(),ModItems.DISSOCIATION_THREAD.get(),ModItems.CONSTRUCTION_MATRIX.get()};
        for(var a:modifiers)for(var b:modifiers)for(int i=0;i<elements.length;i++) {
            var inventory=new net.minecraft.world.SimpleContainer(9);inventory.setItem(2,new ItemStack(a));inventory.setItem(3,new ItemStack(b));inventory.setItem(4,new ItemStack(elements[i]));
            var effect=com.mcmagic.omnira.menu.CrystalProcessingTableMenu.composeEffects(inventory).getFirst();
            int delay=(a==Items.CLOCK?1:0)+(b==Items.CLOCK?1:0),enhancement=(a==Items.IRON_BLOCK?1:0)+(b==Items.IRON_BLOCK?1:0),infusion=(a==modifiers[3]?1:0)+(b==modifiers[3]?1:0);
            if(i<2) {
                h.assertTrue(effect.healing()==(i==1)&&effect.infused()==(infusion>0)&&effect.amplifier()==(delay>0?0:1)+enhancement,"Damage/healing composition mismatch");
                h.assertTrue(effect.duration()==(delay==0?0:800+(delay-1)*1200),"Potion delay mismatch");
                if(i==0)h.assertTrue(effect.physicalDamage(1.5)==(delay==0&&infusion==0?(6+3*enhancement)*1.5:0),"Uninfused physical amount mismatch");
            } else h.assertTrue(effect.enhancement()==enhancement&&effect.delay()==delay&&effect.infusion()==infusion,"Utility composition mismatch");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void burstHitsCasterAlliesAndOnlyItsRadius(GameTestHelper h) {
        var caster=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);caster.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(8,3,8)));h.getLevel().addFreshEntity(caster);
        var ally=h.spawn(EntityType.WOLF,9,3,8);ally.setOwnerUUID(caster.getUUID());
        var enemy=h.spawn(EntityType.COW,7,3,8);var outside=h.spawn(EntityType.COW,15,3,8);
        float own=caster.getHealth(),friendly=ally.getHealth(),other=enemy.getHealth(),far=outside.getHealth();
        var harm=new SpellPayload(20,0,List.of(new SpellEffect(false,1,0)));
        SpellCasting.burst(h.getLevel(),caster,1,harm,caster.getBoundingBox().getCenter(),true);
        h.assertTrue(caster.getHealth()<own&&ally.getHealth()<friendly&&enemy.getHealth()<other,"Burst omitted caster or friendly creature: "+caster.getHealth()+","+ally.getHealth()+","+enemy.getHealth());
        h.assertTrue(outside.getHealth()==far,"Burst hit outside radius");
        var source=enemy.getLastDamageSource();h.assertTrue(source.is(DamageTypeTags.IS_EXPLOSION)&&!source.is(DamageTypeTags.BYPASSES_ARMOR)&&!source.is(DamageTypeTags.IS_PROJECTILE),"Physical burst has wrong armor/explosion tags");
        h.assertTrue(key(source,enemy).equals("death.attack.omnira.spell"),"Wrong spell death message");
        caster.invulnerableTime=0;float before=caster.getHealth();
        SpellCasting.burst(h.getLevel(),caster,1,harm,outside.position(),true);
        h.assertTrue(caster.getHealth()==before,"Legacy self flag applied effects outside sphere");caster.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void physicalAndMagicKeepDifferentArmorRules(GameTestHelper h) {
        var caster=player(h);var target=h.spawn(EntityType.COW,6,3,6);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);target.getAttribute(Attributes.ARMOR).setBaseValue(20);target.setHealth(40);
        var spell=SpellEntity.spawn(h.getLevel(),caster,SpellEntity.Kind.PROJECTILE,caster.position());
        new SpellEffect(false,1,0).apply(spell,caster,target,1);
        h.assertTrue(target.getHealth()>34&&!target.getLastDamageSource().is(DamageTypeTags.BYPASSES_ARMOR),"Ordinary physical spell bypassed armor");
        h.assertTrue(key(target.getLastDamageSource(),target).equals("death.attack.omnira.spell"),"Physical message missing");
        target.setHealth(40);target.invulnerableTime=0;new SpellEffect(false,1,0,true).apply(spell,caster,target,1);
        h.assertTrue(target.getHealth()==28&&target.getLastDamageSource().is(DamageTypes.INDIRECT_MAGIC),"Infusion lost vanilla magic semantics");
        h.assertTrue(key(target.getLastDamageSource(),target).equals("death.attack.omnira.spell"),"Magic message differs");
        target.setHealth(40);target.invulnerableTime=0;spell.configure(1,new SpellPayload(20,6));spell.applyEffects(target);
        h.assertTrue(target.getHealth()>34&&!target.getLastDamageSource().is(DamageTypeTags.BYPASSES_ARMOR),"Legacy raw damage still uses magic");
        target.invulnerableTime=0;SpellEffect.utility("dissociation",0,0,0).apply(spell,caster,target,1);
        h.assertTrue(key(target.getLastDamageSource(),target).equals("death.attack.omnira.dissociation"),"Dissociation message missing");
        spell.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void reflectionMessagesAndThornsImmunity(GameTestHelper h) {
        var mirror=h.spawn(ModEntityTypes.DREAM_MIRROR.get(),8,3,8);var target=h.spawn(EntityType.COW,6,3,6);
        var source=h.getLevel().damageSources().mobAttack(target);
        mirror.returnPhysicalDamage(target,source,1);
        h.assertTrue(key(target.getLastDamageSource(),target).equals("death.attack.omnira.reflection"),"Direct reflection message wrong");
        float before=target.getHealth();mirror.hurt(h.getLevel().damageSources().thorns(target),4);
        h.assertTrue(mirror.getHealth()==30&&target.getHealth()==before&&mirror.getTarget()==null,"Thorns reflected or provoked mirror");
        var kill=SpellDamageSource.of(target,DamageTypes.GENERIC_KILL,target,target,"mined");
        h.assertTrue(key(kill,target).equals("death.attack.omnira.mined")&&kill.is(DamageTypes.GENERIC_KILL),"Mining message changed kill type");
        var spell=SpellEntity.spawn(h.getLevel(),mirror,SpellEntity.Kind.PROJECTILE,mirror.position());target.invulnerableTime=0;new SpellEffect(false,1,0).apply(spell,mirror,target,1);
        h.assertTrue(key(target.getLastDamageSource(),target).equals("death.attack.omnira.spell")&&target.getLastDamageSource().getEntity()==mirror,"Mirror projectile mislabeled direct reflection");spell.discard();h.succeed();
    }
}
