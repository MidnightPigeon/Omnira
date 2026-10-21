package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.bottle.*;
import com.mcmagic.omnira.registry.*;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_bottle_spawner")
@PrefixGameTestTemplate(false)
public final class SpawnerBottleGameTests {
    private record Setup(FakePlayer player,ItemStack bottle,BlockPos source,CompoundTag expected){}
    private static Setup setup(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));
        level.setBlockAndUpdate(pos,Blocks.SPAWNER.defaultBlockState());
        var be=(SpawnerBlockEntity)level.getBlockEntity(pos);var tag=be.saveWithFullMetadata(level.registryAccess());
        var spawn=new CompoundTag();var entity=new CompoundTag();entity.putString("id","minecraft:blaze");entity.putBoolean("Silent",true);spawn.put("entity",entity);
        tag.put("SpawnData",spawn);var potentials=new ListTag();var entry=new CompoundTag();entry.putInt("weight",3);entry.put("data",spawn.copy());potentials.add(entry);tag.put("SpawnPotentials",potentials);
        tag.putShort("Delay",(short)137);tag.putShort("MinSpawnDelay",(short)241);tag.putShort("MaxSpawnDelay",(short)799);
        tag.putShort("SpawnCount",(short)3);tag.putShort("MaxNearbyEntities",(short)9);tag.putShort("RequiredPlayerRange",(short)21);tag.putShort("SpawnRange",(short)5);
        be.loadWithComponents(tag,level.registryAccess());
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"spawner-bottle"));player.setGameMode(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(1,3,1)));var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());player.setItemInHand(InteractionHand.MAIN_HAND,bottle);
        return new Setup(player,bottle,pos,be.saveWithFullMetadata(level.registryAccess()));
    }
    private static void capture(GameTestHelper h,Setup s){
        var context=new UseOnContext(s.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(s.source),Direction.UP,s.source,false));
        h.assertTrue(s.bottle.useOn(context).consumesAction()&&PocketBottleItem.filled(s.bottle)&&h.getLevel().getBlockState(s.source).isAir(),"Right-click did not move the spawner");
    }
    private static void assertRestored(GameTestHelper h,Setup s,BlockPos pos){
        h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof SpawnerBlockEntity,"Missing restored spawner");
        var actual=h.getLevel().getBlockEntity(pos).saveWithFullMetadata(h.getLevel().registryAccess());var expected=s.expected.copy();
        expected.putInt("x",pos.getX());expected.putInt("y",pos.getY());expected.putInt("z",pos.getZ());
        h.assertTrue(actual.equals(expected),"Spawn entity, potentials, timing, parameters or components changed");
    }
    @GameTest(template="spell_arena") public static void capturesCompleteSpawnerData(GameTestHelper h){
        var s=setup(h);capture(h,s);var storage=BottleStorage.get(h.getLevel());var id=s.bottle.get(ModDataComponents.BOTTLE_CAPTURE);
        h.assertTrue(storage.get(id).getCompound("BlockEntity").equals(s.expected),"Captured NBT changed");
        h.assertTrue(!storage.save(new CompoundTag(),h.getLevel().registryAccess()).getList("Captures",10).isEmpty(),"Capture not saved");
        h.assertTrue(!s.bottle.canFitInsideContainerItems(),"Filled spawner bottle entered a container");storage.remove(id);h.succeed();
    }
    @GameTest(template="spell_arena") public static void throwRestoresAndCannotReleaseTwice(GameTestHelper h){
        var s=setup(h);capture(h,s);var level=h.getLevel();var copy=s.bottle.copy();
        s.bottle.use(level,s.player,InteractionHand.MAIN_HAND);
        var thrown=level.getEntitiesOfClass(ThrownPocketBottle.class,s.player.getBoundingBox().inflate(3)).stream().filter(e->e.getOwner()==s.player).findFirst().orElseThrow();
        var target=h.absolutePos(new BlockPos(8,3,8));level.setBlockAndUpdate(target,Blocks.AIR.defaultBlockState());
        h.assertTrue(s.player.getMainHandItem().isEmpty()&&PocketBottleItem.release(level,thrown.getItem(),Vec3.atCenterOf(target),thrown.getUUID()),"Survival throw/release failed");
        assertRestored(h,s,target);h.assertTrue(!PocketBottleItem.release(level,copy,Vec3.atCenterOf(target.above())),"Duplicated bottle released twice");thrown.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void creativeCopiesRetainTemplate(GameTestHelper h){
        var s=setup(h);s.player.setGameMode(GameType.CREATIVE);capture(h,s);var level=h.getLevel();var id=s.bottle.get(ModDataComponents.BOTTLE_CAPTURE);
        for(int i=0;i<2;i++){
            s.player.getCooldowns().removeCooldown(ModItems.POCKET_MAGIC_BOTTLE.get());s.bottle.use(level,s.player,InteractionHand.MAIN_HAND);
            var thrown=level.getEntitiesOfClass(ThrownPocketBottle.class,s.player.getBoundingBox().inflate(3)).stream().filter(e->e.getOwner()==s.player).findFirst().orElseThrow();
            var target=h.absolutePos(new BlockPos(8+i*3,3,8));level.setBlockAndUpdate(target,Blocks.AIR.defaultBlockState());
            h.assertTrue(PocketBottleItem.release(level,thrown.getItem(),Vec3.atCenterOf(target),thrown.getUUID()),"Creative copy failed");assertRestored(h,s,target);thrown.discard();
        }
        h.assertTrue(BottleStorage.get(level).owns(id,null)&&s.player.getMainHandItem()==s.bottle,"Creative template lost");BottleStorage.get(level).remove(id);h.succeed();
    }
    @GameTest(template="spell_arena") public static void breakProtectionAndWrongBlockLeaveBottleEmpty(GameTestHelper h){
        var s=setup(h);java.util.function.Consumer<BlockEvent.BreakEvent> deny=e->{if(e.getPos().equals(s.source))e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(deny);
        try{h.assertTrue(!SpawnerBottleCapture.capture(s.player,s.source,s.bottle,Direction.UP),"Break protection ignored");}
        finally{NeoForge.EVENT_BUS.unregister(deny);}
        h.assertTrue(!PocketBottleItem.filled(s.bottle),"Denied capture filled bottle");assertRestored(h,s,s.source);
        h.assertTrue(!SpawnerBottleCapture.capture(s.player,s.source.above(),s.bottle,Direction.UP),"Captured ordinary block");h.succeed();
    }
    @GameTest(template="spell_arena") public static void blockedAndProtectedReleasePreserveCapture(GameTestHelper h){
        var s=setup(h);capture(h,s);var level=h.getLevel();var id=s.bottle.get(ModDataComponents.BOTTLE_CAPTURE);var data=BottleStorage.get(level).get(id);
        var target=h.absolutePos(new BlockPos(8,3,8));level.setBlockAndUpdate(target,Blocks.STONE.defaultBlockState());
        h.assertTrue(!SpawnerBottleCapture.release(level,data,Vec3.atCenterOf(target),s.player)&&level.getBlockState(target).is(Blocks.STONE),"Release overwrote solid block");
        level.setBlockAndUpdate(target,Blocks.WATER.defaultBlockState());
        java.util.function.Consumer<BlockEvent.EntityMultiPlaceEvent> deny=e->{if(e.getPos().equals(target))e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(deny);
        try{h.assertTrue(!SpawnerBottleCapture.release(level,data,Vec3.atCenterOf(target),s.player),"Place protection ignored");}
        finally{NeoForge.EVENT_BUS.unregister(deny);}
        h.assertTrue(level.getBlockState(target).is(Blocks.WATER)&&BottleStorage.get(level).owns(id,null),"Failed release lost replaced block or bottle contents");
        level.setBlockAndUpdate(target,Blocks.AIR.defaultBlockState());var pig=net.minecraft.world.entity.EntityType.PIG.create(level);pig.setPos(Vec3.atCenterOf(target));level.addFreshEntity(pig);
        h.assertTrue(!SpawnerBottleCapture.release(level,data,Vec3.atCenterOf(target),s.player),"Released inside creature");pig.discard();
        h.assertTrue(!SpawnerBottleCapture.release(level,data,Vec3.atCenterOf(target),null),"Released without responsible player");
        BottleStorage.get(level).remove(id);h.succeed();
    }
    @GameTest(template="spell_arena") public static void rejectsSpectatorsAndFilledBottle(GameTestHelper h){
        var s=setup(h);s.player.setGameMode(GameType.SPECTATOR);
        h.assertTrue(!SpawnerBottleCapture.capture(s.player,s.source,s.bottle,Direction.UP)&&!PocketBottleItem.filled(s.bottle),"Spectator captured spawner");
        s.player.setGameMode(GameType.SURVIVAL);capture(h,s);var target=h.absolutePos(new BlockPos(8,3,8));h.getLevel().setBlockAndUpdate(target,Blocks.SPAWNER.defaultBlockState());
        var id=s.bottle.get(ModDataComponents.BOTTLE_CAPTURE);
        h.assertTrue(!SpawnerBottleCapture.capture(s.player,target,s.bottle,Direction.UP)&&h.getLevel().getBlockState(target).is(Blocks.SPAWNER)&&s.bottle.get(ModDataComponents.BOTTLE_CAPTURE).equals(id),"Filled bottle captured a second spawner");
        BottleStorage.get(h.getLevel()).remove(id);h.succeed();
    }
}
