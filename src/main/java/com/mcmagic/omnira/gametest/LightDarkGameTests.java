package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_light_dark") @PrefixGameTestTemplate(false)
public final class LightDarkGameTests {
    @GameTest(template="spell_arena") public static void microcoresComposeWithoutChangingTargets(GameTestHelper h) {
        var slots=new net.minecraft.world.SimpleContainer(9);
        slots.setItem(0,new ItemStack(ModItems.AIR_MICROCORE.get()));slots.setItem(1,new ItemStack(ModItems.AIR_MICROCORE.get()));
        slots.setItem(2,new ItemStack(ModItems.LIGHT_MICROCORE.get()));slots.setItem(3,new ItemStack(Items.IRON_BLOCK));slots.setItem(4,new ItemStack(ModItems.DARK_MICROCORE.get()));
        var crystal=com.mcmagic.omnira.menu.CrystalProcessingTableMenu.previewCrystal(slots);var payload=SpellPayload.of(crystal);
        h.assertTrue(payload.baseCost()==50 && payload.keywords().containsAll(java.util.List.of("holy","dark_breath","enhancement")),"New components missing");
        h.assertTrue(payload.effects().getFirst().darkness() && payload.effects().getFirst().enhancement()==1,"Dark enhancement not composed");
        h.assertTrue(com.mcmagic.omnira.menu.CrystalProcessingTableMenu.mayPlace(4,new ItemStack(ModItems.DARK_MICROCORE.get())),"Dark element tag missing");
        h.assertTrue(SpellPattern.fromMicrocores(new ItemStack(ModItems.LIGHT_MICROCORE.get()),new ItemStack(ModItems.AIR_MICROCORE.get())).isEmpty(),"Light incorrectly opened a target tier");h.succeed();
    }
    @GameTest(template="spell_arena") public static void holyCloudActualLifeStealAndSelfDamage(GameTestHelper h) {
        var owner=h.spawn(EntityType.COW,new BlockPos(12,3,12));owner.setNoAi(true);owner.setNoGravity(true);owner.setHealth(5);
        var target=h.spawn(EntityType.COW,new BlockPos(4,3,4));target.setNoAi(true);target.setNoGravity(true);
        var cloud=ModEntityTypes.SHADOW_MIST.get().create(h.getLevel());
        cloud.configureSpell(owner,SpellEffect.utility("dark_breath",0,0,0),1,1,true);cloud.setPos(target.position());
        for(int i=0;i<20;i++)cloud.tick();
        h.assertTrue(Math.abs(target.getHealth()-8.5F)<.01 && Math.abs(owner.getHealth()-8)<.01,"Holy cloud must deal 1.5 and heal ceil(2.25)=3");
        h.assertTrue(cloud.diameter(0)==3 && cloud.lifetime()==400,"Wrong fixed radius or lifetime");
        target.discard();owner.setPos(cloud.position());owner.setHealth(8);owner.invulnerableTime=0;
        for(int i=0;i<20;i++)cloud.tick();
        h.assertTrue(Math.abs(owner.getHealth()-6.5F)<.01,"Self damage generated lifesteal");cloud.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void holyPreservesPotionTiersAndContactRelations(GameTestHelper h) {
        var cow=h.spawn(EntityType.COW,new BlockPos(3,3,3));var zombie=h.spawn(EntityType.ZOMBIE,new BlockPos(8,3,8));
        HolyMagic.touch(cow,cow);HolyMagic.touch(cow,zombie);
        h.assertTrue(cow.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING) && zombie.isOnFire(),"Holy contact classification incorrect");
        var effect=SpellEffect.compose(true,1,1);effect.apply(null,cow,cow,1,true);
        var regen=cow.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
        h.assertTrue(regen!=null && regen.getAmplifier()==effect.sustainedAmplifier(1) && regen.getDuration()==effect.duration(),"Holy altered sustained potion");h.succeed();
    }
    @GameTest(template="spell_arena") public static void impactRangeDoesNotScaleWithPower(GameTestHelper h) {
        var owner=h.spawn(EntityType.COW,new BlockPos(2,3,2));var center=h.absoluteVec(new net.minecraft.world.phys.Vec3(8,3,8));
        SpellCasting.impactBurst(h.getLevel(),owner,3,SpellPayload.EMPTY,center,null,null);
        var bursts=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.spell.entity.SpellEntity.class,new net.minecraft.world.phys.AABB(center,center).inflate(1));
        h.assertTrue(bursts.stream().anyMatch(e->Math.abs(e.burstRadius()-1.7)<.001 && e.power()==3),"Ancestor radius or damage power is wrong");
        bursts.forEach(net.minecraft.world.entity.Entity::discard);
        SpellCasting.burst(h.getLevel(),owner,3,SpellPayload.EMPTY,center,false);
        h.assertTrue(h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.spell.entity.SpellEntity.class,new net.minecraft.world.phys.AABB(center,center).inflate(1)).stream().anyMatch(e->e.burstRadius()==3 && e.power()==3),"Spell power changed authored radius or was lost");h.succeed();
    }
    @GameTest(template="spell_arena") public static void cloudSpawnsOncePerBurstAndPersists(GameTestHelper h) {
        var owner=h.spawn(EntityType.COW,new BlockPos(2,3,2));var center=h.absoluteVec(new net.minecraft.world.phys.Vec3(8,3,8));
        h.spawn(EntityType.COW,new BlockPos(8,3,8));h.spawn(EntityType.COW,new BlockPos(9,3,8));
        var payload=new SpellPayload(40,0,java.util.List.of(SpellEffect.utility("dark_breath",1,1,1)),java.util.List.of("dark_breath"));
        SpellCasting.burst(h.getLevel(),owner,2,payload,center,false);
        var clouds=h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.entity.ShadowMist.class,new net.minecraft.world.phys.AABB(center,center).inflate(10));
        h.assertTrue(clouds.size()==1 && clouds.getFirst().diameter(0)==3 && clouds.getFirst().lifetime()==800,"Burst spawned clouds per victim or power enlarged cloud");
        var data=new net.minecraft.nbt.CompoundTag();clouds.getFirst().saveWithoutId(data);
        var restored=ModEntityTypes.SHADOW_MIST.get().create(h.getLevel());restored.load(data);
        h.assertTrue(restored.spellCloud() && restored.lifetime()==800 && !restored.consumeForConversion(),"Spell cloud persistence/conversion isolation failed");h.succeed();
    }
}
