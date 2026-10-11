package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.space.phase.*;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("omnira_phase_charm") @PrefixGameTestTemplate(false)
public final class PhaseConcealmentGameTests {
    private static net.minecraft.world.entity.player.Player player(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(h.absoluteVec(new Vec3(5,3,5)));return p;
    }
    private static void equip(net.minecraft.world.entity.player.Player p){CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("charm",0,new ItemStack(ModItems.PHASE_CONCEALMENT_CHARM.get()));}
    @GameTest(template="spell_arena") public static void triggersOnlyAfterWornActualDamage(GameTestHelper h){
        var p=player(h);var mob=EntityType.ZOMBIE.create(h.getLevel());
        p.getInventory().setItem(0,new ItemStack(ModItems.PHASE_CONCEALMENT_CHARM.get()));
        p.hurt(p.damageSources().mobAttack(mob),2);h.assertTrue(!PhaseConcealment.active(p),"Carried charm grants phase");
        equip(p);p.invulnerableTime=0;float hp=p.getHealth();
        p.hurt(p.damageSources().mobAttack(mob),2);
        h.assertTrue(p.getHealth()<hp&&PhaseConcealment.active(p),"First hit did not deal damage then trigger phase");
        h.assertTrue(p.getEffect(ModEffects.PHASE_CONCEALMENT).getDuration()==100,"Wrong phase duration");
        hp=p.getHealth();p.invulnerableTime=0;p.hurt(p.damageSources().mobAttack(mob),2);
        h.assertTrue(p.getHealth()==hp,"Melee penetrated phase");h.succeed();
    }
    @GameTest(template="spell_arena") public static void directVersusAreaDamage(GameTestHelper h){
        var p=player(h);p.addEffect(new MobEffectInstance(ModEffects.PHASE_CONCEALMENT,100));
        var arrow=EntityType.ARROW.create(h.getLevel());var potion=EntityType.POTION.create(h.getLevel());
        h.assertTrue(PhaseConcealment.blocks(p,p.damageSources().arrow(arrow,null)),"Arrow bypasses phase");
        h.assertTrue(!PhaseConcealment.blocks(p,p.damageSources().indirectMagic(potion,null)),"Splash potion blocked");
        h.assertTrue(!PhaseConcealment.blocks(p,p.damageSources().explosion(null,null)),"Explosion blocked");
        h.assertTrue(!PhaseConcealment.blocks(p,p.damageSources().magic())&&!PhaseConcealment.blocks(p,p.damageSources().fall()),"Environmental damage blocked");
        var spell=SpellEntity.spawn(h.getLevel(),p,SpellEntity.Kind.BURST,p.position());
        h.assertTrue(!PhaseConcealment.blocks(p,SpellDamageSource.physical(p,spell,p,"spell")),"Area spell blocked");
        spell.directSingleTarget(true);
        h.assertTrue(PhaseConcealment.blocks(p,SpellDamageSource.physical(p,spell,p,"spell")),"Direct aim spell mistaken for area explosion");
        float hp=p.getHealth();new SpellEffect(false,0,0,true).apply(spell,p,p,1);
        h.assertTrue(p.getHealth()==hp,"Single-target potion spell bypasses phase");
        spell.directSingleTarget(false);p.invulnerableTime=0;new SpellEffect(false,0,0,true).apply(spell,p,p,1);
        h.assertTrue(p.getHealth()<hp,"Area potion spell did not damage player");spell.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void entityOnlyAndNoInheritedImmunity(GameTestHelper h){
        var p=player(h);equip(p);p.addEffect(new MobEffectInstance(ModEffects.PHASE_CONCEALMENT,100));
        var mob=EntityType.ZOMBIE.create(h.getLevel());mob.setPos(p.position().add(.2,0,0));p.setDeltaMovement(Vec3.ZERO);mob.push(p);
        h.assertTrue(!p.isPushable()&&!p.canCollideWith(mob)&&p.getDeltaMovement().equals(Vec3.ZERO),"Entity collision retained");
        h.assertTrue(!p.noPhysics&&!p.getAbilities().mayfly&&!com.mcmagic.omnira.time.TemporalDislocation.throughWalls(p),"Block phase or flight leaked");
        var wall=p.blockPosition();h.getLevel().setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.assertTrue(!h.getLevel().noCollision(p),"Block collision disappeared during phase");h.getLevel().setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.assertTrue(!com.mcmagic.omnira.spacetime.AnchoringSigilItem.equipped(p),"Inherited anchoring sigil identity");
        h.assertTrue(p.addEffect(new MobEffectInstance(MobEffects.WITHER,40))&&p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40)),"Inherited potion immunity");
        var before=p.position();p.removeEffect(ModEffects.PHASE_CONCEALMENT);
        h.assertTrue(p.isPushable()&&!PhaseConcealment.active(p)&&p.position().equals(before),"Expiry retains phase or rewinds position");h.succeed();
    }
    @GameTest(template="spell_arena") public static void expiresAfterExactlyOneHundredTicks(GameTestHelper h){
        var p=player(h);p.addEffect(new MobEffectInstance(ModEffects.PHASE_CONCEALMENT,100));var start=p.position();
        for(int i=0;i<99;i++)p.baseTick();
        h.assertTrue(PhaseConcealment.active(p)&&p.getEffect(ModEffects.PHASE_CONCEALMENT).getDuration()==1,"Phase expired early");
        p.baseTick();h.assertTrue(!PhaseConcealment.active(p)&&p.isPushable()&&p.position().equals(start),"Phase did not expire without teleporting");h.succeed();
    }
    @GameTest(template="spell_arena") public static void trueMeleeBlockedButTrueExplosionAllowed(GameTestHelper h){
        var p=player(h);equip(p);var mob=EntityType.ZOMBIE.create(h.getLevel());
        com.mcmagic.omnira.space.sea.VoidSeaEffects.trueDamage(p,2,p.damageSources().mobAttack(mob));
        h.assertTrue(p.getHealth()==18&&PhaseConcealment.active(p),"True damage did not trigger charm");
        h.assertTrue(!com.mcmagic.omnira.space.sea.VoidSeaEffects.trueDamage(p,10,p.damageSources().mobAttack(mob))&&p.getHealth()==18,"True melee bypassed phase");
        var jelly=com.mcmagic.omnira.space.sea.VoidSeaContent.JELLYFISH.get().create(h.getLevel());
        h.assertTrue(com.mcmagic.omnira.space.sea.VoidSeaEffects.trueDamage(p,8,p.damageSources().explosion(jelly,jelly))&&p.getHealth()==10,"True area explosion blocked");h.succeed();
    }
    @GameTest(template="spell_arena") public static void assemblyCenterAndEmptySlots(GameTestHelper h){
        var inv=new SimpleContainer(7);inv.setItem(6,new ItemStack(ModItems.SPACETIME_ANCHORING_SIGIL.get()));inv.setItem(4,new ItemStack(PhaseContent.CONTROL.get()));
        var input=new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(inv);
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(ModRecipes.ASSEMBLY_TYPE.get(),input,h.getLevel()).orElseThrow().value();
        h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).is(ModItems.PHASE_CONCEALMENT_CHARM.get()),"Wrong charm output");
        inv.setItem(1,new ItemStack(Items.STICK));h.assertTrue(!recipe.matches(input,h.getLevel()),"Extra component accepted");inv.setItem(1,ItemStack.EMPTY);
        inv.setItem(6,new ItemStack(PhaseContent.CONTROL.get()));inv.setItem(4,new ItemStack(ModItems.SPACETIME_ANCHORING_SIGIL.get()));
        h.assertTrue(!recipe.matches(input,h.getLevel()),"Center slot constraint lost");h.succeed();
    }
}
