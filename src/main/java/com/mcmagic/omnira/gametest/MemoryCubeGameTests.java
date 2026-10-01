package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.MemoryCubeBlock;
import com.mcmagic.omnira.item.MemoryCubeBlockItem;
import com.mcmagic.omnira.mire.MemoryCubeBlockEntity;
import com.mcmagic.omnira.mire.MemoryCubeRewards;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_memory_cube")
@PrefixGameTestTemplate(false)
public final class MemoryCubeGameTests {
    @GameTest(template="spell_arena") public static void placedStateAndDailyExtraction(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,2,4));
        level.setBlockAndUpdate(pos,ModBlocks.MEMORY_CUBE.get().defaultBlockState());
        h.assertTrue(level.getBlockEntity(pos) instanceof MemoryCubeBlockEntity,"Memory cube lacks its state holder");
        var corrupted=ModBlocks.MEMORY_CUBE.get().getCloneItemStack(level,pos,level.getBlockState(pos));
        h.assertTrue(!MemoryCubeBlockItem.peaceful(corrupted),"Corrupted state lost when collected");
        level.setBlockAndUpdate(pos,level.getBlockState(pos).setValue(MemoryCubeBlock.PEACEFUL,true));
        var peaceful=ModBlocks.MEMORY_CUBE.get().getCloneItemStack(level,pos,level.getBlockState(pos));
        h.assertTrue(MemoryCubeBlockItem.peaceful(peaceful),"Peaceful state lost when collected");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(MemoryCubeRewards.extract(level,player,true),"First peaceful extraction failed");
        h.assertTrue(!MemoryCubeRewards.extract(level,player,true),"Peaceful extraction repeated on the same day");
        h.assertTrue(MemoryCubeRewards.extract(level,player,false),"Corrupted extraction shared peaceful limit");
        h.assertTrue(!MemoryCubeRewards.extract(level,player,false),"Corrupted extraction repeated on the same day");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void eachOpeningProducesSixItems(GameTestHelper h){
        var random=RandomSource.create(19);
        for(boolean peaceful:new boolean[]{true,false})for(int run=0;run<64;run++){
            var drops=MemoryCubeRewards.roll(h.getLevel(),h.absoluteVec(net.minecraft.world.phys.Vec3.ZERO),random,peaceful);
            h.assertTrue(drops.size()==6,"Memory opening must produce six independent rewards");
            for(var stack:drops)h.assertTrue(stack.getCount()==1&&!stack.isEmpty(),"Memory reward missing");
        }
        h.assertTrue(ModItems.PEACEFUL_MEMORY.get()!=ModItems.CORRUPTED_MEMORY.get(),"Memory states merged into one item");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void peacefulTimePoolCoversCurrentMaterials(GameTestHelper h){
        var expected=java.util.Set.of("omnira:time_seed","omnira:time_flower","omnira:time_sapling",
                "omnira:reborn_rottenleaf_lily","omnira:decayed_rottenleaf_lily","omnira:timeflow_eel",
                "omnira:chronal_blindfish","omnira:time_warp_point","omnira:time_microcore");
        var found=new java.util.HashSet<String>();var random=RandomSource.create(1701);
        int timeRolls=0;
        for(int i=0;i<512;i++)for(var stack:MemoryCubeRewards.roll(h.getLevel(),h.absoluteVec(net.minecraft.world.phys.Vec3.ZERO),random,true)){
            String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if(expected.contains(id)){found.add(id);timeRolls++;}
        }
        h.assertTrue(found.equals(expected),"Time material pool is incomplete: "+found);
        h.assertTrue(timeRolls>1300&&timeRolls<1800,"Peaceful memory no longer splits time and light evenly");
        h.succeed();
    }
}
