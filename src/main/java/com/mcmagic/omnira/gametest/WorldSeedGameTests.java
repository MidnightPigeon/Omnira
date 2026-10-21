package com.mcmagic.omnira.gametest;

import java.util.Arrays;
import com.mcmagic.omnira.myriad.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_world_seed")
@PrefixGameTestTemplate(false)
public final class WorldSeedGameTests {
    @GameTest(template="spell_arena") public static void raritiesAndNegativeCoordinateAlignment(GameTestHelper h) {
        h.assertTrue(new ItemStack(WorldSeedContent.DORMANT.get()).getRarity()==Rarity.UNCOMMON,"Dormant seed must be yellow");
        h.assertTrue(new ItemStack(WorldSeedContent.ACTIVE.get()).getRarity()==Rarity.RARE,"Active seed must be blue");
        h.assertTrue(WorldDeflection.origin(new BlockPos(-1,-1,-1)).equals(new BlockPos(-4,-4,-4)),"Negative coordinates rounded towards zero");
        h.assertTrue(WorldDeflection.origin(new BlockPos(7,7,7)).equals(new BlockPos(4,4,4)),"Not aligned to one biome cell");
        h.assertTrue(WorldDeflection.placementOrigin(new BlockPos(-1,-1,-1)).equals(new BlockPos(-4,-2,-4)),"Negative placement not lifted by two");
        for(int y=-8;y<8;y++)h.assertTrue(WorldDeflection.placementOrigin(new BlockPos(7,y,7)).equals(WorldDeflection.origin(new BlockPos(7,y,7)).above(2)),"Vertical offset depends on clicked height");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void raisedCubeBiomeFootprintAndUnbreakableProtection(GameTestHelper h) {
        var level=h.getLevel();var min=WorldDeflection.placementOrigin(h.absolutePos(new BlockPos(5,5,5)));
        var player=h.makeMockPlayer(GameType.SURVIVAL);var seed=new ItemStack(WorldSeedContent.ACTIVE.get(),2);
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.DEEP_DARK);
        var chunk=level.getChunkAt(min);var section=chunk.getSections()[level.getSectionIndex(min.getY())];
        var old=new java.util.ArrayList<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>();
        for(int i=0;i<64;i++)old.add(section.getNoiseBiome(i%4,i/16,i/4%4));
        var snapshot=new net.minecraft.world.level.block.state.BlockState[64];
        for(int i=0;i<64;i++) {
            level.setBlock(min.offset(i%4,i/16,i/4%4),Blocks.STONE.defaultBlockState(),2);
            snapshot[i]=(i/16<2?Blocks.DIRT:Blocks.AIR).defaultBlockState();
        }
        level.setBlock(min,Blocks.BEDROCK.defaultBlockState(),2);
        var outside=min.offset(4,0,0);level.setBlock(outside,Blocks.GOLD_BLOCK.defaultBlockState(),2);
        h.assertTrue(WorldDeflection.apply(level,min,biome,snapshot,player,seed),"Valid projection failed");
        h.assertTrue(seed.getCount()==1&&level.getBlockState(min).is(Blocks.BEDROCK),"Seed debit or unbreakable protection wrong");
        for(int i=1;i<64;i++)h.assertTrue(level.getBlockState(min.offset(i%4,i/16,i/4%4)).equals(snapshot[i]),"Cube or two-ground/two-air layers wrong");
        h.assertTrue(level.getBlockState(outside).is(Blocks.GOLD_BLOCK),"Changed blocks outside cube");
        int target=Math.floorMod(min.getY()>>2,4)*16+Math.floorMod(min.getZ()>>2,4)*4+Math.floorMod(min.getX()>>2,4);
        for(int i=0;i<64;i++)h.assertTrue(section.getNoiseBiome(i%4,i/16,i/4%4).equals(i==target||i==target+16?biome:old.get(i)),"Changed another biome palette cell");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void containersAreReplacedWithoutCopyingOldContents(GameTestHelper h) {
        var level=h.getLevel();var min=WorldDeflection.placementOrigin(h.absolutePos(new BlockPos(5,5,5)));
        level.setBlock(min,Blocks.CHEST.defaultBlockState(),2);
        ((ChestBlockEntity)level.getBlockEntity(min)).setItem(0,new ItemStack(Items.DIAMOND,32));
        var states=new net.minecraft.world.level.block.state.BlockState[64];Arrays.fill(states,Blocks.AIR.defaultBlockState());states[0]=Blocks.CHEST.defaultBlockState();
        var seed=new ItemStack(WorldSeedContent.ACTIVE.get());
        h.assertTrue(WorldDeflection.apply(level,min,level.getBiome(min),states,h.makeMockPlayer(GameType.CREATIVE),seed),"Chest blocked projection");
        h.assertTrue(seed.isEmpty()&&((ChestBlockEntity)level.getBlockEntity(min)).isEmpty(),"Preserved old inventory or creative consumption differed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void invalidAreaAndSnapshotDoNotConsume(GameTestHelper h) {
        var level=h.getLevel();var min=WorldDeflection.placementOrigin(h.absolutePos(new BlockPos(5,5,5)));var player=h.makeMockPlayer(GameType.SURVIVAL);
        var seed=new ItemStack(WorldSeedContent.ACTIVE.get());var states=new net.minecraft.world.level.block.state.BlockState[64];
        var biome=level.getBiome(min);level.setBlock(min,Blocks.GOLD_BLOCK.defaultBlockState(),2);
        h.assertTrue(!WorldDeflection.apply(level,min,biome,states,player,seed)&&!seed.isEmpty()&&level.getBlockState(min).is(Blocks.GOLD_BLOCK),"Null snapshot partially changed world");
        Arrays.fill(states,Blocks.STONE.defaultBlockState());
        h.assertTrue(!WorldDeflection.apply(level,new BlockPos(min.getX(),level.getMaxBuildHeight()+2,min.getZ()),biome,states,player,seed)&&seed.getCount()==1,"Out-of-height projection consumed seed");
        h.assertTrue(!WorldDeflection.apply(level,min.offset(1,0,0),biome,states,player,seed),"Accepted nonaligned cell");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sourceSurfaceSnapshotHasTwoGroundTwoAirLayers(GameTestHelper h) {
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(8,5,8));
        for(int x=-2;x<2;x++)for(int z=-2;z<2;z++)for(int y=-1;y<3;y++)level.setBlock(at.offset(x,y,z),(y<=0?Blocks.MOSS_BLOCK:Blocks.AIR).defaultBlockState(),2);
        var snapshot=WorldDeflection.sample(level,at,level.getBiome(at.above()));
        h.assertTrue(snapshot!=null&&snapshot.length==64,"Real source surface not found");
        for(int i=0;i<64;i++)h.assertTrue(snapshot[i].is(i/16<2?Blocks.MOSS_BLOCK:Blocks.AIR),"Source terrain layers not copied");h.succeed();
    }
    @GameTest(template="spell_arena") public static void raisedBiomeUpdatesAcrossSectionBoundary(GameTestHelper h) {
        var level=h.getLevel();var base=WorldDeflection.origin(h.absolutePos(new BlockPos(5,5,5)));
        var min=new BlockPos(base.getX(),Math.floorDiv(base.getY(),16)*16+14,base.getZ());
        var chunk=level.getChunkAt(min);int qx=min.getX()>>2,qy=min.getY()>>2,qz=min.getZ()>>2;
        var old=new java.util.HashMap<BlockPos,net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>();
        for(int x=qx&~3;x<(qx&~3)+4;x++)for(int y=qy&~3;y<(qy&~3)+8;y++)for(int z=qz&~3;z<(qz&~3)+4;z++)
            old.put(new BlockPos(x,y,z),chunk.getNoiseBiome(x,y,z));
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.DEEP_DARK);
        var snapshot=new net.minecraft.world.level.block.state.BlockState[64];Arrays.fill(snapshot,Blocks.STONE.defaultBlockState());
        h.assertTrue(WorldDeflection.apply(level,min,biome,snapshot,h.makeMockPlayer(GameType.SURVIVAL),new ItemStack(WorldSeedContent.ACTIVE.get())),"Raised section-edge projection failed");
        for(var entry:old.entrySet()) {
            var pos=entry.getKey();boolean changed=pos.getX()==qx&&pos.getZ()==qz&&(pos.getY()==qy||pos.getY()==qy+1);
            h.assertTrue(chunk.getNoiseBiome(pos.getX(),pos.getY(),pos.getZ()).equals(changed?biome:entry.getValue()),"Wrong biome cells across section edge");
        }
        h.succeed();
    }
    private WorldSeedGameTests() {}
}
