package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.vehicle.*;
import com.mcmagic.omnira.spell.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.*;

@GameTestHolder("omnira_vehicle_safety")
@PrefixGameTestTemplate(false)
public final class VehicleSafetyGameTests {
    private static ServerPlayer rider(GameTestHelper h,Entity mount){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"vehicle-safety");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;
        p.setPos(mount.position());h.assertTrue(p.startRiding(mount),"Mount failed");return p;
    }
    private static CruiseOrbEntity orb(GameTestHelper h){
        var orb=ModEntityTypes.CRUISE_ORB.get().create(h.getLevel());orb.setPos(h.absoluteVec(new Vec3(8,20,8)));return orb;
    }
    @GameTest(template="spell_arena") public static void occupiedDamageAndUnoccupiedPickup(GameTestHelper h){
        var attacker=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(boolean stable:new boolean[]{false,true}){
            var orb=orb(h);var pilot=rider(h,orb);
            if(stable)orb.storage.upgrades.setStackInSlot(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
            for(int i=0;i<9;i++){
                var damage=new LivingIncomingDamageEvent(pilot,new net.neoforged.neoforge.common.damagesource.DamageContainer(pilot.damageSources().playerAttack(attacker),4));
                NeoForge.EVENT_BUS.post(damage);h.assertTrue(damage.isCanceled(),"Cabin passenger took attack damage");
                orb.hurt(pilot.damageSources().playerAttack(attacker),4);
                if(i==2)h.assertTrue(orb.crackStage()==(stable?0:1),"One attack counted twice, or stabilization failed");
                if(i<8)for(int j=0;j<10;j++)orb.tick();
            }
            h.assertTrue(orb.isRemoved()!=stable,"Occupied attack outcome wrong");
            if(stable){pilot.stopRiding();orb.hurt(attacker.damageSources().playerAttack(attacker),1);h.assertTrue(orb.isRemoved(),"Stabilized unoccupied vehicle cannot be collected");}
        }
        var empty=orb(h);empty.hurt(attacker.damageSources().playerAttack(attacker),1);h.assertTrue(empty.isRemoved(),"Unoccupied normal vehicle cannot be collected");
        var arrow=new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW,h.getLevel());
        empty=orb(h);empty.hurt(attacker.damageSources().arrow(arrow,attacker),1);h.assertTrue(empty.isRemoved(),"Unoccupied vehicle ignores player projectile pickup");
        empty=orb(h);empty.hurt(attacker.damageSources().explosion(attacker,attacker),1);h.assertTrue(!empty.isRemoved(),"Player explosion incorrectly collects vehicle");empty.discard();
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void cabinAirAndEnvironmentOnly(GameTestHelper h){
        var orb=orb(h);var pilot=rider(h,orb);
        var breathe=new LivingBreatheEvent(pilot,false,1,0);NeoForge.EVENT_BUS.post(breathe);
        h.assertTrue(breathe.canBreathe() && breathe.getRefillAirAmount()==pilot.getMaxAirSupply() && !orb.dismountsUnderwater(),"Cabin has no underwater air");
        for(var source:java.util.List.of(pilot.damageSources().lava(),pilot.damageSources().onFire(),pilot.damageSources().drown(),pilot.damageSources().inWall())){
            var damage=new LivingIncomingDamageEvent(pilot,new net.neoforged.neoforge.common.damagesource.DamageContainer(source,4));
            NeoForge.EVENT_BUS.post(damage);h.assertTrue(damage.isCanceled(),"Environment bypassed cabin");
        }
        var voidDamage=new LivingIncomingDamageEvent(pilot,new net.neoforged.neoforge.common.damagesource.DamageContainer(pilot.damageSources().fellOutOfWorld(),4));
        NeoForge.EVENT_BUS.post(voidDamage);h.assertTrue(!voidDamage.isCanceled(),"Cabin prevents void death");
        pilot.stopRiding();orb.discard();
        var broom=ModEntityTypes.CRYSTAL_BROOM.get().create(h.getLevel());broom.setPos(pilot.position());h.assertTrue(pilot.startRiding(broom),"Cannot mount broom");
        breathe=new LivingBreatheEvent(pilot,false,1,0);NeoForge.EVENT_BUS.post(breathe);
        h.assertTrue(!breathe.canBreathe() && !broom.dismountsUnderwater(),"Broom gains cabin air or forces underwater dismount");
        var damage=new LivingIncomingDamageEvent(pilot,new net.neoforged.neoforge.common.damagesource.DamageContainer(pilot.damageSources().lava(),4));
        NeoForge.EVENT_BUS.post(damage);h.assertTrue(!damage.isCanceled(),"Broom protects passenger");
        pilot.stopRiding();broom.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void broomMotionRecipeAndDismount(GameTestHelper h){
        h.assertTrue(Math.abs(CrystalBroomEntity.velocity(1,0,0,false,1,0).z+.05)<1e-8,"Starting speed not one metre per second");
        h.assertTrue(CrystalBroomEntity.velocity(0,0,0,false,0,0).y<0,"Broom hovers without lift");
        for(int ticks=1;ticks<=30;ticks++){
            double speed=.05+.05*Math.min(ticks-1,20)/20.0;
            h.assertTrue(Math.abs(CrystalBroomEntity.velocity(1,0,0,false,ticks,0).z+speed)<1e-8,"Forward acceleration incorrect");
            h.assertTrue(Math.abs(CrystalBroomEntity.velocity(2,0,0,false,ticks,0).z-speed)<1e-8,"Reverse acceleration incorrect");
            h.assertTrue(Math.abs(CrystalBroomEntity.velocity(16,0,0,false,0,ticks).y-speed)<1e-8,"Lift acceleration incorrect");
            h.assertTrue(CrystalBroomEntity.velocity(17,0,0,false,ticks,ticks).length()<=.10000001,"Diagonal lift exceeds speed cap");
        }
        h.assertTrue(CrystalBroomEntity.velocity(16,0,.1,true,21,21).y==-.02,"Underwater lift prevents sinking");
        var broom=ModEntityTypes.CRYSTAL_BROOM.get().create(h.getLevel());broom.setPos(h.absoluteVec(new Vec3(8,20,8)));var pilot=rider(h,broom);
        broom.input(pilot,8);broom.tickCount++;broom.tick();h.assertTrue(broom.getYRot()==3,"Right key does not steer");
        float heading=pilot.getYRot();broom.positionRider(pilot);
        h.assertTrue(pilot.getYRot()==heading+3,"Rider heading does not follow positioned mount");
        broom.positionRider(pilot);
        h.assertTrue(pilot.getYRot()==heading+3,"Repeated positioning doubles rider rotation");
        h.assertTrue(Math.abs(net.minecraft.util.Mth.rotLerp(.5F,broom.yRotO,broom.getYRot())-1.5F)<1e-5,"Turn has no intermediate render angle");
        broom.input(pilot,0);broom.tickCount++;broom.tick();broom.positionRider(pilot);
        h.assertTrue(pilot.getYRot()==heading+3,"Released steering continues rotating rider");
        broom.input(pilot,17);
        for(int tick=0;tick<21;tick++)broom.tick();
        h.assertTrue(Math.abs(broom.getDeltaMovement().length()-.1)<1e-5,"Held input does not reach top speed");
        broom.input(pilot,2);broom.tick();
        h.assertTrue(Math.abs(broom.getDeltaMovement().horizontalDistance()-.05)<1e-5,"Reversing retains acceleration");
        broom.input(pilot,0);broom.tick();
        h.assertTrue(broom.getDeltaMovement().horizontalDistance()==0,"Released movement persists");
        broom.input(pilot,16);broom.tick();
        h.assertTrue(Math.abs(broom.getDeltaMovement().y-.05)<1e-8,"Lift does not restart at base speed");
        broom.positionRider(pilot);
        double dismountY=broom.getY();
        broom.input(pilot,1|16|32);
        h.assertTrue(!pilot.isPassenger(),"Shift cannot dismount moving airborne broom");
        h.assertTrue(Math.abs(pilot.getY()-dismountY)<1e-6,"Dismount drops below the broom");
        var slots=new SimpleContainer(7);Item[] ingredients={Items.STICK,ModItems.SPIRITUAL_CRYSTAL.get(),Items.HAY_BLOCK,ModItems.RESONANCE_CRYSTAL.get(),Items.STICK,ModItems.SPIRITUAL_CRYSTAL.get(),ModItems.ANALYSIS_CRYSTAL.get()};
        for(int i=0;i<7;i++)slots.setItem(i,new ItemStack(ingredients[i]));
        var input=new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(slots);
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(ModRecipes.ASSEMBLY_TYPE.get(),input,h.getLevel());
        h.assertTrue(recipe.isPresent() && recipe.get().value().assemble(input,h.getLevel().registryAccess()).is(ModItems.CRYSTAL_BROOM.get()),"Shuffled broom recipe missing");
        slots.setItem(6,ItemStack.EMPTY);h.assertTrue(!recipe.get().value().matches(input,h.getLevel()),"Recipe ignores analysis core");
        broom.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void spellsLeaveCabinAndExplosionsDamageShell(GameTestHelper h)throws Exception{
        var orb=orb(h);var pilot=rider(h,orb);h.getLevel().addFreshEntity(orb);
        pilot.setPos(orb.position().add(0,CruiseOrbPanel.RIDER_Y,0));pilot.setYRot(0);pilot.setXRot(0);
        var cow=EntityType.COW.create(h.getLevel());cow.setPos(orb.getX(),pilot.getEyeY()-.7,orb.getZ()+5);cow.setNoGravity(true);h.getLevel().addFreshEntity(cow);
        var hit=SpellCasting.target(pilot,10,true);
        h.assertTrue(hit instanceof net.minecraft.world.phys.EntityHitResult entityHit && entityHit.getEntity()==cow,"Cabin prevents selecting outside target");
        var spell=com.mcmagic.omnira.spell.entity.SpellEntity.spawn(h.getLevel(),pilot,com.mcmagic.omnira.spell.entity.SpellEntity.Kind.PROJECTILE,pilot.getEyePosition());
        var contact=spell.getClass().getDeclaredMethod("canContact",Entity.class);contact.setAccessible(true);
        h.assertTrue(!(boolean)contact.invoke(spell,orb),"Own spell collides with cabin");
        spell.setDeltaMovement(0,0,.3);
        for(int i=0;i<8;i++)spell.tick();
        h.assertTrue(!spell.isRemoved() && spell.getZ()>orb.getBoundingBox().maxZ,"Outgoing projectile did not pass through cabin");
        var other=orb(h);h.assertTrue((boolean)contact.invoke(spell,other),"Enemy cabin cannot intercept spell");other.discard();spell.discard();
        SpellCasting.burst(h.getLevel(),pilot,1,SpellPayload.EMPTY,orb.position().add(3,1,0),false);
        h.assertTrue(orb.isAlive(),"External explosion instantly destroys cabin");
        var state=new net.minecraft.nbt.CompoundTag();orb.saveWithoutId(state);h.assertTrue(state.getInt("Impacts")==1,"External explosion is not exactly one collision");
        h.assertTrue(SpellCasting.cast(pilot,new SpellPattern(ElementType.WATER,ElementType.FIRE)),"Self burst failed");
        h.assertTrue(orb.isRemoved(),"Unstabilized self burst did not shatter cabin");
        var stable=orb(h);stable.storage.upgrades.setStackInSlot(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
        h.assertTrue(pilot.startRiding(stable),"Cannot remount stabilized cabin");stable.selfBurst();h.assertTrue(!stable.isRemoved(),"Self burst destroyed stabilized cabin");
        pilot.stopRiding();stable.discard();cow.discard();h.succeed();
    }
}
