package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("omnira_dragon_spell")
@PrefixGameTestTemplate(false)
public final class DragonSpellGameTests {
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"dragon-spell-test"));
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,2,2)));
        h.getLevel().addNewPlayer(player);
        return player;
    }
    @GameTest(template="spell_arena")
    public static void dragonPartsReceivePhysicalSpellDamage(GameTestHelper h) {
        var player=player(h);
        var dragon=h.spawn(EntityType.ENDER_DRAGON,5,4,5);
        var spell=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.PROJECTILE,dragon.position());
        spell.configure(1,new SpellPayload(0,0,List.of(new SpellEffect(false,1,0))));
        var body=java.util.Arrays.stream(dragon.getSubEntities()).filter(p->p.name.equals("body")).findFirst().orElseThrow();
        h.assertTrue(SpellCasting.validTarget(dragon.head) && SpellCasting.validTarget(body),"Dragon parts excluded from targeting");
        spell.applyEffects(dragon.head);
        h.assertTrue(Math.abs(dragon.getHealth()-194)<.01,"Head did not receive six physical damage");
        dragon.invulnerableTime=0;
        spell.applyEffects(body);
        h.assertTrue(Math.abs(dragon.getHealth()-191.5)<.01,"Body damage reduction lost");
        dragon.discard();spell.discard();player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void projectileContactAndWardAttachment(GameTestHelper h) {
        var player=player(h);
        var dragon=h.spawn(EntityType.ENDER_DRAGON,5,4,5);
        var center=dragon.position();
        for(var part:dragon.getSubEntities()) part.setPos(center.add(8,0,0));
        dragon.head.setPos(center);
        var projectile=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.PROJECTILE,dragon.head.getBoundingBox().getCenter());
        projectile.configure(1,new SpellPayload(0,0,List.of(new SpellEffect(false,1,0))));
        projectile.tick();
        h.assertTrue(projectile.isRemoved() && dragon.getHealth()==194,"Projectile contact did not damage dragon head");
        var ward=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.WARD,center);
        ward.follow(dragon.head);
        h.assertTrue(ward.follows(dragon),"Ward attached to part instead of living bearer");
        dragon.discard();ward.discard();player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void burstDoesNotRepeatEffectsAcrossDragonParts(GameTestHelper h) {
        var player=player(h);
        var dragon=h.spawn(EntityType.ENDER_DRAGON,5,4,5);
        var center=dragon.position();
        for(var part:dragon.getSubEntities()) part.setPos(center);
        // Healing has no hurt cooldown, so duplicate part application would be observable.
        dragon.setHealth(100);
        SpellCasting.burst(h.getLevel(),player,1,new SpellPayload(0,0,List.of(new SpellEffect(true,1,0))),center,true);
        h.assertTrue(Math.abs(dragon.getHealth()-108)<.01,"Burst applied healing more or less than once");
        dragon.discard();player.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void spellPowerScalesPotionAmountsAndLevels(GameTestHelper h) {
        var player=player(h);
        var cow=h.spawn(EntityType.COW,4,2,4);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);
        cow.setHealth(4);
        new SpellEffect(true,1,0).apply(player,player,cow,1.5);
        h.assertTrue(cow.getHealth()==16,"Healing II should restore 12 at 150% power");
        cow.setHealth(40);cow.invulnerableTime=0;
        new SpellEffect(false,1,0,true).apply(player,player,cow,1.5);
        h.assertTrue(cow.getHealth()==22,"Harming II should deal 18 at 150% power");
        new SpellEffect(true,0,800).apply(player,player,cow,1.5);
        h.assertTrue(cow.getEffect(MobEffects.REGENERATION).getDuration()==800 && cow.getEffect(MobEffects.REGENERATION).getAmplifier()==1,"Regeneration must scale level, not duration");
        new SpellEffect(false,0,800).apply(player,player,cow,1.5);
        h.assertTrue(cow.getEffect(MobEffects.POISON).getDuration()==800 && cow.getEffect(MobEffects.POISON).getAmplifier()==1,"Physical poison conversion must use damage and preserve duration");
        h.assertTrue(new SpellEffect(true,0,800).sustainedAmplifier(1)==0,"Base regeneration lost its original level gap");
        h.assertTrue(new SpellEffect(true,0,800).sustainedAmplifier(2)==1,"Exact doubling threshold rounded too high");
        h.assertTrue(new SpellEffect(true,0,800).sustainedAmplifier(2.1)==2,"Healing amount did not round up to the next tier");
        h.assertTrue(new SpellEffect(false,2,800).sustainedAmplifier(1)==1,"Physical enhanced damage should map 12 damage to Poison II");
        h.assertTrue(new SpellEffect(false,2,800,true).sustainedAmplifier(1)==2,"Infused damage lost its level gap");
        h.assertTrue(new SpellEffect(true,0,800).sustainedAmplifier(.1)==0,"Low power created a negative amplifier");
        cow.removeEffect(MobEffects.REGENERATION);
        new SpellEffect(true,0,800,true).apply(player,player,cow,1.5);
        h.assertTrue(cow.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()==1 && cow.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration()==800,"Resistance did not follow regeneration level and duration");
        var zombie=h.spawn(EntityType.ZOMBIE,6,2,6);zombie.setHealth(4);
        new SpellEffect(false,1,0,true).apply(player,player,zombie,1.5);
        h.assertTrue(zombie.getHealth()==16,"Undead harm-to-heal inversion lost");
        cow.discard();zombie.discard();player.discard();h.succeed();
    }
}
