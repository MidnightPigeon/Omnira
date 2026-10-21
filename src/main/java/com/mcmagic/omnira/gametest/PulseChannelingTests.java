package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.registry.*;
import java.util.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_pulse_channeling")
@PrefixGameTestTemplate(false)
public final class PulseChannelingTests {
    private static final BlockPos AT=new BlockPos(5,3,5);
    private static FakePlayer player(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"channel-test"));
        p.setGameMode(GameType.SURVIVAL);p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT.north(2).below())));
        p.getAttribute(ModAttributes.MAX_MANA).setBaseValue(1024);p.getAttribute(ModAttributes.MANA_REGEN).setBaseValue(1);
        p.setData(ModAttachments.MANA,new ManaState(100,1024));
        p.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(h.absolutePos(AT)));
        p.setOldPosAndRot();return p;
    }
    private static void hold(GameTestHelper h,FakePlayer p,int through){
        for(int tick=1;tick<=through;tick++)h.runAtTickTime(tick,()->
                PulseChanneling.input(p,true,h.absolutePos(AT),Direction.NORTH));
    }
    private static PulseOrbEntity orb(GameTestHelper h,BlockPos pos){return PulseOrbEntity.from(h.getBlockEntity(pos));}
    private static void close(GameTestHelper h,double actual,double expected){h.assertTrue(Math.abs(actual-expected)<.001,"Expected "+expected+", got "+actual);}
    private static void stop(FakePlayer p){PulseChanneling.stop(p);}
    private static void regen(FakePlayer p){p.tickCount=4;ManaEvents.tick(new PlayerTickEvent.Post(p));}

    @GameTest(template="spell_arena",timeoutTicks=150)
    public static void chargeRegenerationAttributesAndRelease(GameTestHelper h){
        h.setBlock(AT,PulseContent.ORB.get());var p=player(h);hold(h,p,109);
        h.runAtTickTime(95,()->{
            h.assertTrue(!PulseChanneling.active(p),"Charged before five seconds");close(h,PulseNetworks.available(orb(h,AT)),0);
            double before=p.getData(ModAttachments.MANA).current();regen(p);h.assertTrue(p.getData(ModAttachments.MANA).current()>before,"Charging suppressed regeneration");
        });
        h.runAtTickTime(103,()->{
            h.assertTrue(PulseChanneling.active(p),"Did not charge after five seconds");close(h,PulseNetworks.available(orb(h,AT)),512);
            double before=p.getData(ModAttachments.MANA).current();regen(p);close(h,p.getData(ModAttachments.MANA).current(),before);
            p.getAttribute(ModAttributes.MANA_REGEN).setBaseValue(2);
        });
        h.runAtTickTime(105,()->close(h,PulseNetworks.available(orb(h,AT)),1024));
        h.runAtTickTime(110,()->{
            double before=p.getData(ModAttachments.MANA).current();PulseChanneling.input(p,false,h.absolutePos(AT),Direction.NORTH);
            h.assertTrue(!PulseChanneling.active(p),"Release left a source");close(h,PulseNetworks.available(orb(h,AT)),0);
            regen(p);h.assertTrue(p.getData(ModAttachments.MANA).current()>before,"Release did not restore regeneration");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=140)
    public static void wireInjectionRespectsLossAndMachineDemand(GameTestHelper h){
        h.setBlock(AT,PulseContent.WIRE.get());h.setBlock(AT.south(),PulseContent.WIRE.get());h.setBlock(AT.south(2),PulseContent.ORB.get());
        h.setBlock(AT.south(3),ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        var p=player(h);hold(h,p,102);
        h.runAtTickTime(103,()->{
            close(h,PulseNetworks.input((PulseWireEntity)h.getBlockEntity(AT)),512);
            close(h,PulseNetworks.loss(h.getBlockEntity(AT)),0);close(h,PulseNetworks.loss(h.getBlockEntity(AT.south())),25.6);
            var power=PulseNetworks.power(h.getLevel(),h.absolutePos(AT.south(3)));
            h.assertTrue(power.connected()&&power.running(),"Temporary source did not run the machine");close(h,power.allocated(),486.4);
            close(h,PulseNetworks.occupied(orb(h,AT.south(2))),486.4);close(h,PulseNetworks.available(orb(h,AT.south(2))),0);
            stop(p);h.assertTrue(!PulseNetworks.power(h.getLevel(),h.absolutePos(AT.south(3))).running(),"Removed source still drives machine");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=140)
    public static void parallelPlayersAndEitherHandCancel(GameTestHelper h){
        h.setBlock(AT,PulseContent.ORB.get());var a=player(h);var b=player(h);hold(h,a,102);hold(h,b,102);
        h.runAtTickTime(103,()->{
            close(h,PulseNetworks.available(orb(h,AT)),1024);
            a.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STICK));PulseChanneling.tick(h.getLevel().getServer());
            h.assertTrue(!PulseChanneling.active(a)&&PulseChanneling.active(b),"Offhand did not cancel only its owner");close(h,PulseNetworks.available(orb(h,AT)),512);
            b.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));PulseChanneling.tick(h.getLevel().getServer());
            h.assertTrue(!PulseChanneling.active(b),"Main hand did not cancel");close(h,PulseNetworks.available(orb(h,AT)),0);h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=150)
    public static void missingHeartbeatStopsSource(GameTestHelper h){
        h.setBlock(AT,PulseContent.ORB.get());var p=player(h);hold(h,p,102);
        h.runAtTickTime(103,()->h.assertTrue(PulseChanneling.active(p),"Not charged"));
        h.runAtTickTime(120,()->{h.assertTrue(!PulseChanneling.active(p),"Missing heartbeat kept source alive");close(h,PulseNetworks.available(orb(h,AT)),0);h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=140)
    public static void aimAndReplacementInvalidateSource(GameTestHelper h){
        h.setBlock(AT,PulseContent.ORB.get());var a=player(h);var b=player(h);hold(h,a,102);hold(h,b,102);
        h.runAtTickTime(103,()->{
            a.setYHeadRot(a.getYHeadRot()+90);PulseChanneling.tick(h.getLevel().getServer());
            h.assertTrue(!PulseChanneling.active(a)&&PulseChanneling.active(b),"Looking away did not stop supply");
            h.setBlock(AT,Blocks.STONE);PulseChanneling.tick(h.getLevel().getServer());
            h.assertTrue(!PulseChanneling.active(b),"Replaced target kept source alive");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=140)
    public static void heldItemsAndWrongFacesCannotStart(GameTestHelper h){
        h.setBlock(AT,PulseContent.ORB.get());var a=player(h);var b=player(h);
        a.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STICK));b.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        hold(h,a,105);hold(h,b,105);
        h.assertTrue(!PulseChannelTargets.accepts(h.getLevel(),h.absolutePos(AT.north()),Direction.NORTH),"Air accepted");
        h.setBlock(AT.east(3),ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        h.assertTrue(PulseChannelTargets.accepts(h.getLevel(),h.absolutePos(AT.east(3)),Direction.NORTH)
                &&!PulseChannelTargets.accepts(h.getLevel(),h.absolutePos(AT.east(3)),Direction.SOUTH),"Receiver face check failed");
        h.runAtTickTime(106,()->{h.assertTrue(!PulseChanneling.active(a)&&!PulseChanneling.active(b),"Held item started supply");close(h,PulseNetworks.available(orb(h,AT)),0);stop(a);stop(b);h.succeed();});
    }
    private PulseChannelingTests(){}
}
