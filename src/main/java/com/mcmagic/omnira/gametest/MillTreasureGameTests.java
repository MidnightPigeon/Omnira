package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.mire.MillTreasures;
import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_mill_treasures")
@PrefixGameTestTemplate(false)
public final class MillTreasureGameTests {
    private static List<MillTreasures.Site> sites(BlockPos p){return List.of(
            new MillTreasures.Site(p,false,"omnira:chests/rusty_lake_mill_cellar"),
            new MillTreasures.Site(p.east(2),false,"omnira:chests/rusty_lake_mill_workshop"),
            new MillTreasures.Site(p.south(3),true,"omnira:chests/rusty_lake_mill_memory"),
            new MillTreasures.Site(p.south(3).east(2),true,"omnira:chests/rusty_lake_mill_echo"));}
    private static CrystalBallBlockEntity ball(GameTestHelper h,BlockPos p){return (CrystalBallBlockEntity)h.getLevel().getBlockEntity(p);}
    private static MillTreasures setup(GameTestHelper h,BlockPos p){
        var level=h.getLevel();for(var s:sites(p))level.setBlockAndUpdate(s.pos(),s.reversed()?Blocks.AIR.defaultBlockState():ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        var data=new MillTreasures();h.assertTrue(data.register(level,p,sites(p)),"Registration failed");h.assertTrue(!data.register(level,p,sites(p)),"Duplicate registration reset treasures");return data;
    }
    @GameTest(template="spell_arena")
    public static void phasePersistenceAndFiniteLoot(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));var data=setup(h,p);ball(h,p).setItem(0,new ItemStack(Items.DIAMOND,7));
        data.pulse(l,6000);h.assertTrue(l.isEmptyBlock(p)&&l.isEmptyBlock(p.east(2)),"Ordinary treasures stayed visible");
        for(var s:sites(p))if(s.reversed())h.assertTrue(ball(h,s.pos()).hasPendingLoot(),"Reversed treasure not misted");
        h.assertTrue(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(p).inflate(6)).isEmpty(),"Phase transition dropped items");
        var rich=ball(h,p.south(3));var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);rich.createMenu(1,player.getInventory(),player);
        h.assertTrue(!rich.isEmpty(),"Memory table missing");rich.clearContent();rich.setItem(0,new ItemStack(Items.EMERALD,3));
        data=MillTreasures.load(data.save(new CompoundTag(),l.registryAccess()),l.registryAccess());data.pulse(l,7200);
        h.assertTrue(ball(h,p).getItem(0).getCount()==7&&l.isEmptyBlock(p.south(3)),"Snapshot lost normal inventory");ball(h,p).clearContent();
        data.pulse(l,12000);h.assertTrue(!ball(h,p.south(3)).hasPendingLoot()&&ball(h,p.south(3)).getItem(0).getCount()==3,"Reversed loot rerolled or lost contents");
        data.pulse(l,13200);h.assertTrue(ball(h,p).isEmpty(),"Emptied normal treasure refilled");
        data=MillTreasures.load(data.save(new CompoundTag(),l.registryAccess()),l.registryAccess());data.pulse(l,18000);
        h.assertTrue(ball(h,p.south(3)).getItem(0).is(Items.EMERALD),"Hidden snapshot lost after reload");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void destroyedAndOccupiedSites(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));var data=setup(h,p);
        l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p.south(3),Blocks.DIAMOND_BLOCK.defaultBlockState());
        data.pulse(l,6000);h.assertTrue(l.getBlockState(p.south(3)).is(Blocks.DIAMOND_BLOCK),"Player obstruction overwritten");
        h.assertTrue(ball(h,p.south(3).east(2)).hasPendingLoot(),"Unobstructed treasure missing");
        l.setBlockAndUpdate(p.south(3),Blocks.AIR.defaultBlockState());data.pulse(l,6020);h.assertTrue(ball(h,p.south(3)).hasPendingLoot(),"Deferred spawn did not recover");
        l.setBlockAndUpdate(p.east(2),Blocks.GOLD_BLOCK.defaultBlockState());data.pulse(l,7200);
        h.assertTrue(l.isEmptyBlock(p)&&l.getBlockState(p.east(2)).is(Blocks.GOLD_BLOCK),"Removed normal resurrected or player block overwritten");
        l.setBlockAndUpdate(p.east(2),Blocks.AIR.defaultBlockState());data.pulse(l,7220);h.assertTrue(ball(h,p.east(2))!=null,"Hidden normal snapshot lost when obstructed");
        data.pulse(l,12000);var victim=ball(h,p.south(3));victim.setLootTable(null);victim.clearContent();l.setBlockAndUpdate(p.south(3),Blocks.AIR.defaultBlockState());
        data.pulse(l,12020);data.pulse(l,13200);data.pulse(l,18000);h.assertTrue(l.isEmptyBlock(p.south(3)),"Destroyed reverse treasure regenerated");h.succeed();
    }
}
