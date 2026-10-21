package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.ManaEngineAccess;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_pulse_absorption")
@PrefixGameTestTemplate(false)
public final class PulseAbsorptionTests {
    private static FakePlayer player(GameTestHelper h,BlockPos relative){
        var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"pulse-test")){
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
        };
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(relative)));
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(100);
        p.setData(ModAttachments.MANA,new ManaState(0,100));return p;
    }
    private static void source(GameTestHelper h,BlockPos at){
        h.setBlock(at,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        var e=((ManaEngineAccess)h.getBlockEntity(at)).engineState();e.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
    }
    private static void close(GameTestHelper h,double a,double b){h.assertTrue(Math.abs(a-b)<.001,"Expected "+b+", got "+a);}
    @GameTest(template="spell_arena") public static void footCellSharesOneOutputAndReleasesIt(GameTestHelper h){
        var source=new BlockPos(5,3,3);source(h,source);h.setBlock(source.south(),PulseContent.WIRE.get());h.setBlock(source.south(2),PulseContent.ORB.get());
        var a=player(h,source.south());var b=player(h,source.south());var server=h.getLevel().getServer();
        PulseNetworks.updatePlayers(server,List.of(a,b));
        close(h,PulseNetworks.sourceOccupied(h.getLevel(),h.absolutePos(source),Direction.SOUTH),512);
        close(h,PulseNetworks.available(PulseOrbEntity.from(h.getBlockEntity(source.south(2)))),0);
        PulseNetworks.absorbPlayers(server);close(h,a.getData(ModAttachments.MANA).current(),26);close(h,b.getData(ModAttachments.MANA).current(),26);
        a.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(source.north())));b.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        PulseNetworks.updatePlayers(server,List.of(a,b));close(h,PulseNetworks.sourceOccupied(h.getLevel(),h.absolutePos(source),Direction.SOUTH),0);
        close(h,PulseNetworks.available(PulseOrbEntity.from(h.getBlockEntity(source.south(2)))),512);
        PulseNetworks.absorbPlayers(server);close(h,a.getData(ModAttachments.MANA).current(),26);h.succeed();
    }
    @GameTest(template="spell_arena") public static void absorptionSettlesAfterMachineAndBeforeIdleQuota(GameTestHelper h){
        var nodes=List.of(new PulseTransport.Node(1024,0,0,0,0,true,false),new PulseTransport.Node(0,0,0,0,0,false,true),
            new PulseTransport.Node(0,8192,0,0,0,false,false),new PulseTransport.Node(0,0,256,256,1,false,false));
        var result=PulseTransport.solve(nodes,List.of(new PulseTransport.Link(0,1),new PulseTransport.Link(1,2),new PulseTransport.Link(2,3)),new boolean[]{true,false,false,false});
        close(h,result.demand()[3],256);close(h,result.absorbed()[0],1024-256/.95);close(h,result.generation()[0],1024);close(h,result.free()[2],0);
        close(h,result.loss()[1],256/.95*.05);close(h,result.demand()[3]+result.loss()[1]+result.absorbed()[0],1024);h.succeed();
    }
    @GameTest(template="spell_arena") public static void groupedConsumersKeepAbsorptionSeparate(GameTestHelper h){
        var nodes=List.of(new PulseTransport.Node(1024,0,0,0,0,true,false),new PulseTransport.Node(0,8192,0,0,0,false,false),
            new PulseTransport.Node(0,0,256,256,1,false,false),new PulseTransport.Node(0,0,256,256,2,false,false));
        var r=PulseTransport.solve(nodes,List.of(new PulseTransport.Link(0,1),new PulseTransport.Link(1,2),new PulseTransport.Link(1,3)),new boolean[]{true,false,false,false});
        close(h,r.demand()[2],256);close(h,r.demand()[3],256);close(h,r.absorbed()[0],512);close(h,r.generation()[0],1024);close(h,r.free()[1],0);h.succeed();
    }
    @GameTest(template="spell_arena") public static void ordinaryRegenerationCannotCreateOrEraseOvercharge(GameTestHelper h){
        var mana=new ManaState(95,100).withCurrent(200);close(h,mana.current(),100);
        mana=mana.absorbPulse(51);close(h,mana.current(),151);close(h,mana.regenerate().current(),151);close(h,mana.spend(20).current(),131);
        close(h,mana.withMaximum(80).current(),151);h.assertTrue(mana.pulseOverloaded()&&!new ManaState(150,100).pulseOverloaded(),"Overflow threshold is not strict");
        var p=player(h,new BlockPos(5,3,5));p.setData(ModAttachments.MANA,new ManaState(1_500_001,1_000_000));
        var saved=new net.minecraft.nbt.CompoundTag();p.saveWithoutId(saved);var copy=player(h,new BlockPos(6,3,5));copy.load(saved);
        close(h,copy.getData(ModAttachments.MANA).current(),1_500_001);h.succeed();
    }
    @GameTest(template="spell_arena") public static void blastIsSphericalMagicAndPreservesContents(GameTestHelper h)throws Exception{
        var p=player(h,new BlockPos(5,3,5));p.setData(ModAttachments.MANA,new ManaState(150,100));
        // Fake players never tick away the normal login protection.
        var protection=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");protection.setAccessible(true);protection.setInt(p,0);
        PulseAbsorption.receive(p,1);close(h,p.getHealth(),100);close(h,p.getData(ModAttachments.MANA).current(),151);
        var near=h.spawn(EntityType.COW,new BlockPos(8,3,5));var far=h.spawn(EntityType.COW,new BlockPos(10,3,10));
        for(var mob:List.of(near,far)){mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);mob.setHealth(100);}
        var item=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),p.getX()+1,p.getY()+.9,p.getZ(),new ItemStack(Items.DIAMOND,3));h.getLevel().addFreshEntity(item);
        var stand=h.spawn(EntityType.ARMOR_STAND,new BlockPos(5,3,7));float standHealth=stand.getHealth();
        h.setBlock(new BlockPos(5,2,5),Blocks.CHEST);var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getBlockEntity(new BlockPos(5,2,5));chest.setItem(0,new ItemStack(Items.EMERALD,7));
        PulseAbsorption.receive(p,1);
        close(h,p.getHealth(),80);close(h,near.getHealth(),80);close(h,far.getHealth(),100);close(h,p.getData(ModAttachments.MANA).current(),100);
        h.assertTrue(near.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)&&near.getLastDamageSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION),"Blast lost magic/explosion semantics");
        h.assertTrue(p.getEffect(MobEffects.WEAKNESS).getAmplifier()==1&&p.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==1
            &&p.getEffect(MobEffects.WEAKNESS).getDuration()==3600&&!near.hasEffect(MobEffects.WEAKNESS)&&!near.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Penalty leaked to nearby creatures or has wrong level/duration");
        h.assertTrue(item.isAlive()&&item.getItem().getCount()==3&&item.getDeltaMovement().lengthSqr()>0&&near.getDeltaMovement().lengthSqr()>0,"Drops destroyed or missing knockback");
        close(h,stand.getHealth(),standHealth);h.assertTrue(chest.getItem(0).getCount()==7&&h.getBlockEntity(new BlockPos(5,2,5))==chest,"Terrain/container changed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void activeNativeFaceCannotAlsoExportFE(GameTestHelper h){
        var source=new BlockPos(5,3,3);source(h,source);
        h.setBlock(source.south(),ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        var engine=((ManaEngineAccess)h.getBlockEntity(source)).engineState();
        h.assertTrue(PulseOutput.nativeConnection(h.getLevel(),h.absolutePos(source),Direction.SOUTH),"Native face not recognized");
        var port=engine.energyPort(Direction.SOUTH);if(port!=null)h.assertTrue(port.extractEnergy(10000,false)==0&&!port.canExtract(),"Native source also exported FE");
        close(h,PulseNetworks.power(h.getLevel(),h.absolutePos(source.south())).allocated(),512);h.succeed();
    }
    @GameTest(template="spell_arena") public static void spacetimeAbsorptionOnlyUsesItsOwnOutput(GameTestHelper h){
        var root=h.absolutePos(new BlockPos(5,3,5));
        h.assertTrue(com.mcmagic.omnira.engine.SpacetimeEngineStructure.place(h.getLevel(),root),"Missing engine structure");
        var engine=com.mcmagic.omnira.engine.SpacetimeEngineBlock.controller(h.getLevel(),root);var base=engine.getBlockPos().below();
        var p=player(h,new BlockPos(5,3,5));p.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(base.east(2)));
        PulseNetworks.updatePlayers(h.getLevel().getServer(),List.of(p));
        for(var face:Direction.Plane.HORIZONTAL)close(h,PulseNetworks.sourceOccupied(h.getLevel(),base.relative(face),face),face==Direction.EAST?512:0);
        PulseNetworks.absorbPlayers(h.getLevel().getServer());close(h,p.getData(ModAttachments.MANA).current(),52);
        PulseNetworks.updatePlayers(h.getLevel().getServer(),List.of());h.succeed();
    }
}
