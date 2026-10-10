package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.space.sea.*;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spatial_combat")
@PrefixGameTestTemplate(false)
public final class SpatialCombatGameTests {
    @GameTest(template="spell_arena")
    public static void physicalEvasionPreservesSpells(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var arrow=EntityType.ARROW.create(h.getLevel());
        for(var type:java.util.List.of(VoidSeaContent.JELLYFISH.get(),VoidSeaContent.MANTA.get())){
            var mob=type.create(h.getLevel());mob.setPos(h.absoluteVec(new Vec3(5,3,5)));
            h.getLevel().addFreshEntity(mob);float health=mob.getHealth();
            mob.hurt(h.getLevel().damageSources().playerAttack(player),2);
            h.assertTrue(mob.getHealth()==health,"Melee bypassed evasion");
            mob.hurt(h.getLevel().damageSources().arrow(arrow,player),2);
            h.assertTrue(mob.getHealth()==health,"Arrow bypassed evasion");
            var spell=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.PROJECTILE,mob.position());
            mob.hurt(SpellDamageSource.physical(mob,spell,player,"spell"),2);
            h.assertTrue(mob.getHealth()==health-2,"Physical spell projectile incorrectly evaded");
            mob.invulnerableTime=0;
            mob.hurt(h.getLevel().damageSources().magic(),1);
            h.assertTrue(mob.getHealth()==health-3,"Magic incorrectly evaded");
            spell.discard();mob.discard();
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void speciesDeathDrops(GameTestHelper h){
        var center=h.absoluteVec(new Vec3(5,3,5));
        for(var type:java.util.List.of(VoidSeaContent.JELLYFISH.get(),VoidSeaContent.MANTA.get())){
            var mob=type.create(h.getLevel());mob.setPos(center);h.getLevel().addFreshEntity(mob);
            mob.hurt(h.getLevel().damageSources().genericKill(),1000);
            var expected=type==VoidSeaContent.JELLYFISH.get()?VoidSeaContent.JELLY_ITEM.get():
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("omnira:unstable_spacetime_aggregate"));
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(2));
            h.assertTrue(drops.stream().filter(e->e.getItem().is(expected)).mapToInt(e->e.getItem().getCount()).sum()==1,"Wrong death drop for "+type);
            drops.forEach(ItemEntity::discard);mob.discard();
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void bladeDoesNotReflect(GameTestHelper h){
        var mirror=ModEntityTypes.DREAM_MIRROR.get().create(h.getLevel());
        mirror.setPos(h.absoluteVec(new Vec3(5,3,5)));h.getLevel().addFreshEntity(mirror);
        var player=h.makeMockPlayer(GameType.SURVIVAL);float health=player.getHealth();
        var blade=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.SPATIAL_BLADE,mirror.position());
        blade.setDeltaMovement(0,0,1);blade.configure(1,new SpellPayload(20,2));
        h.assertTrue(!blade.reflectFrom(mirror),"Blade trajectory reflected");
        blade.applyEffects(mirror);
        h.assertTrue(mirror.getHealth()==28 && player.getHealth()==health,"Blade payload reflected or blocked");
        h.assertTrue(blade.ownedBy(player)&&blade.getDeltaMovement().z==1,"Blade owner or motion changed");
        blade.discard();mirror.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void aimRayReturnsAlongIncomingPath(GameTestHelper h){
        var mirror=ModEntityTypes.DREAM_MIRROR.get().create(h.getLevel());
        mirror.setPos(h.absoluteVec(new Vec3(5,3,5)));h.getLevel().addFreshEntity(mirror);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(mirror.position().add(0,0,2));player.setYRot(180);player.setXRot(0);
        h.getLevel().addFreshEntity(player);
        var payload=new SpellPayload(20,0,java.util.List.of(new SpellEffect(false,1,0,true)));
        h.assertTrue(SpellCasting.cast(player,new SpellPattern(ElementType.AIR,null),payload,1),"Aim ray did not cast");
        h.assertTrue(player.getHealth()<20,"Reflected ray missed its caster");
        h.assertTrue(mirror.getHealth()==18,"Mirror lost nonphysical contact effects");
        player.discard();mirror.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void reflectedRayRespectsWalls(GameTestHelper h){
        var mirror=ModEntityTypes.DREAM_MIRROR.get().create(h.getLevel());
        mirror.setPos(h.absoluteVec(new Vec3(5,3,5)));h.getLevel().addFreshEntity(mirror);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(mirror.position().add(0,0,4));player.setYRot(180);player.setXRot(0);
        h.getLevel().addFreshEntity(player);
        var wall=net.minecraft.core.BlockPos.containing(mirror.getX(),player.getEyeY(),mirror.getZ()+2);
        h.getLevel().setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        SpellCasting.reflectDirectRay(player,mirror,new Vec3(mirror.getX(),player.getEyeY(),mirror.getZ()),
                new SpellPayload(20,0,java.util.List.of(new SpellEffect(false,1,0,true))),1,SpellCasting.Modifiers.NONE,6);
        h.assertTrue(player.getHealth()==20,"Reflected ray passed through a wall");
        h.getLevel().removeBlock(wall,false);player.discard();mirror.discard();h.succeed();
    }
}
