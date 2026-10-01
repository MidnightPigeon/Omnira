package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.time.TemporalDislocation;
import com.mcmagic.omnira.mire.TimePassage;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_dislocation")
@PrefixGameTestTemplate(false)
public final class TemporalDislocationGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"phase-test"),false);
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,cookie){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
        };
        p.setPos(h.absoluteVec(new Vec3(5,3,5)));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return p;
    }
    @GameTest(template="spell_arena") public static void independentFirstSnapshotsSurviveRefreshAndUpgrade(GameTestHelper h){
        var p=player(h);var first=p.position();
        p.addEffect(new MobEffectInstance(ModEffects.TIME_PASSAGE,200));
        p.setPos(first.add(2,0,0));var second=p.position();
        p.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_DISLOCATION,200));
        p.setPos(first.add(4,0,0));
        p.addEffect(new MobEffectInstance(ModEffects.TIME_PASSAGE,400,1));
        p.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_DISLOCATION,400,1));
        p.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_DISLOCATION,500,1));
        p.setHealth(7);p.getFoodData().setFoodLevel(6);
        p.removeEffect(ModEffects.TEMPORAL_DISLOCATION);
        TemporalDislocation.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(p));
        h.assertTrue(p.position().distanceTo(second)<.001,"Dislocation origin overwritten");
        h.assertTrue(p.getHealth()==7&&p.getFoodData().getFoodLevel()==6,"Dislocation restored non-position statistics");
        TimePassage.restore(p,0);
        h.assertTrue(p.position().distanceTo(first)<.001,"Time Passage snapshot mixed with dislocation or overwritten");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bodyCollisionAndMeleeButNotProjectileImmunity(GameTestHelper h){
        var p=player(h);p.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_DISLOCATION,200));
        var mob=net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel());mob.setPos(p.position().add(.2,0,0));
        p.setDeltaMovement(Vec3.ZERO);mob.push(p);
        h.assertTrue(p.getDeltaMovement().equals(Vec3.ZERO)&&!p.isPushable(),"Phased player still pushed");
        var attack=new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(p,p);
        TemporalDislocation.melee(attack);h.assertTrue(attack.isCanceled(),"Melee targeting not canceled");
        var melee=new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(p,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().mobAttack(mob),4));
        TemporalDislocation.damage(melee);h.assertTrue(melee.isCanceled(),"Melee damage penetrated dislocation");
        var magic=new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(p,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().magic(),2));
        TemporalDislocation.damage(magic);h.assertTrue(!magic.isCanceled(),"Phase unintentionally blocked ranged magic");
        h.assertTrue(!TemporalDislocation.throughWalls(p),"Level I received wall phase");h.succeed();
    }
    @GameTest(template="spell_arena") public static void flightAndWallPhaseAreTemporary(GameTestHelper h){
        var p=player(h);var start=p.position();
        p.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_DISLOCATION,200,1));
        h.assertTrue(p.getAbilities().mayfly&&p.getAbilities().flying&&p.getDeltaMovement().equals(Vec3.ZERO),"Flight not enabled immediately on grant");
        TemporalDislocation.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(p));
        h.assertTrue(p.mayFly()&&p.getAbilities().flying,"Level II did not grant creative flight");
        p.doTick();h.assertTrue(p.noPhysics,"Player tick overwrote wall phase");
        p.setPos(start.add(3,0,0));p.removeEffect(ModEffects.TEMPORAL_DISLOCATION);
        TemporalDislocation.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(p));
        h.assertTrue(!p.mayFly()&&!p.getAbilities().mayfly&&!p.noPhysics,"Temporary flight permission or no-clip leaked");
        h.assertTrue(p.position().distanceTo(start)<.001,"Did not return after phase");
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;
        p.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_DISLOCATION,200,1));
        TemporalDislocation.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(p));
        p.removeEffect(ModEffects.TEMPORAL_DISLOCATION);TemporalDislocation.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(p));
        h.assertTrue(p.mayFly()&&p.getAbilities().flying,"Existing creative flight revoked");h.succeed();
    }
    @GameTest(template="spell_arena") public static void buttonPressCurve(GameTestHelper h){
        h.assertTrue(com.mcmagic.omnira.aggregation.AggregationLayout.buttonDepth(0)==0,"Button begins depressed");
        h.assertTrue(com.mcmagic.omnira.aggregation.AggregationLayout.buttonDepth(2)==.04,"Button press invisible");
        h.assertTrue(com.mcmagic.omnira.aggregation.AggregationLayout.buttonDepth(6)==.02,"Release not interpolated");
        h.assertTrue(com.mcmagic.omnira.aggregation.AggregationLayout.buttonDepth(8)==0,"Button stuck down");h.succeed();
    }
    @GameTest(template="spell_arena") public static void naturalExpirationReturnsToFirstOrigin(GameTestHelper h){
        var p=player(h);var origin=p.position();
        p.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_DISLOCATION,200));p.setPos(origin.add(3,0,0));
        for(int i=0;i<199;i++)p.getEffect(ModEffects.TEMPORAL_DISLOCATION).tick(p,()->{});
        TemporalDislocation.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(p));
        h.assertTrue(p.position().distanceTo(origin)>1,"Rewind occurred before expiration");
        if(!p.getEffect(ModEffects.TEMPORAL_DISLOCATION).tick(p,()->{}))p.removeEffect(ModEffects.TEMPORAL_DISLOCATION);
        TemporalDislocation.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(p));
        h.assertTrue(p.position().distanceTo(origin)<.001&&!p.getPersistentData().contains(TemporalDislocation.RECORD),"Expired effect did not clean its snapshot");h.succeed();
    }
}
