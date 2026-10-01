package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_flight_lifetime")
@PrefixGameTestTemplate(false)
public final class FlightLifetimeGameTests {
    private static ServerPlayer player(GameTestHelper h,int x) {
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"flight-test");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;
        p.setPos(h.absoluteVec(new Vec3(x,100,4)));return p;
    }
    private static SpellEntity flight(GameTestHelper h,ServerPlayer player,List<String> keywords) {
        var f=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.SELF_FLIGHT,player.position());
        f.configure(5,new SpellPayload(20,0,List.of(),keywords));h.assertTrue(player.startRiding(f,true),"Mount failed");return f;
    }
    @GameTest(template="spell_arena")
    public static void flowingWaterCannotChangeVelocity(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(8,5,8));var level=h.getLevel();
        level.setBlock(p.west(),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
        level.setBlock(p,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,3),2);
        level.setBlock(p.east(),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,7),2);
        h.assertTrue(level.getFluidState(p).getFlow(level,p).lengthSqr()>0,"Fixture has no current");
        var owner=player(h,2);var velocity=new Vec3(0,.01,.3);
        for(var kind:List.of(SpellEntity.Kind.PROJECTILE,SpellEntity.Kind.SELF_FLIGHT,SpellEntity.Kind.FLYING_BLOCK)) {
            var spell=SpellEntity.spawn(level,owner,kind,Vec3.atLowerCornerOf(p).add(.5,.1,.5));spell.setDeltaMovement(velocity);
            spell.updateFluidHeightAndDoFluidPushing();
            for(boolean down:new boolean[]{false,true}) {spell.onInsideBubbleColumn(down);spell.onAboveBubbleCol(down);}
            h.assertTrue(spell.getDeltaMovement().distanceToSqr(velocity)<1e-12,"Current changed "+kind);
            h.assertTrue(!spell.isPushedByFluid()&&!spell.isPushedByFluid(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()),"Fluid overload not disabled");spell.discard();
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=430)
    public static void durationFadeInfusionAndLandingProtection(GameTestHelper h) {
        var normalPlayer=player(h,3);var normal=flight(h,normalPlayer,List.of("enhancement","enhancement"));
        var delayPlayer=player(h,7);var delayed=flight(h,delayPlayer,List.of("delay","delay"));
        var infusedPlayer=player(h,11);var infused=flight(h,infusedPlayer,List.of("infusion"));
        h.assertTrue(normal.duration()==400&&delayed.duration()==1200&&infused.duration()==-1,"Flight modifier rules differ");
        h.runAtTickTime(370,()->{
            h.assertTrue(normal.opacity(0)>0&&normal.opacity(0)<1,"Flight does not fade before expiry");
            h.assertTrue(delayed.opacity(0)==1&&infused.opacity(0)==1,"Extended/infinite flight faded prematurely");
        });
        h.runAtTickTime(410,()->{
            h.assertTrue(normal.isRemoved()&&!normalPlayer.isPassenger(),"Normal flight did not expire");
            var buff=normalPlayer.getEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);
            h.assertTrue(buff!=null&&buff.getDuration()==40&&buff.getAmplifier()==0,"Missing two-second slow falling");
            h.assertTrue(!delayed.isRemoved()&&!infused.isRemoved(),"Extended/infinite flight expired");
            delayed.trigger(true);h.assertTrue(delayPlayer.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"Collision termination lacks slow falling");
            infusedPlayer.stopRiding();h.assertTrue(infusedPlayer.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"Dismount lacks slow falling");infused.discard();h.succeed();
        });
    }
}
