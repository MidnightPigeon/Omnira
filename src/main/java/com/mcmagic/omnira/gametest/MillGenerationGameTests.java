package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.mire.*;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_mill_generation")
@PrefixGameTestTemplate(false)
public final class MillGenerationGameTests {
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void capturedPlanAndRegistration(GameTestHelper h){
        var l=h.getLevel();var origin=h.absolutePos(new BlockPos(96,10,0));var piece=new MillPiece(origin);
        var b=piece.getBoundingBox();
        var naturalSite=origin.offset(1,2,1);
        l.setBlockAndUpdate(naturalSite,Blocks.GOLD_BLOCK.defaultBlockState());
        // Exercise independent chunk clips, including reload-equivalent placement ordering.
        for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++){
            l.getChunk(x,z);var clip=new BoundingBox(x*16,l.getMinBuildHeight(),z*16,x*16+15,l.getMaxBuildHeight()-1,z*16+15);
            piece.postProcess(l,l.structureManager(),l.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(7),clip,new ChunkPos(x,z),origin);
        }
        h.assertTrue(MillPlan.CELLS.size()==2190,"Natural sample cells leaked into the structure");
        h.assertTrue(MillStructure.placementY(100)==99&&MillPlan.PLAN.get("building_lift").getAsInt()==1,"Building lift must be stored in the blueprint, not added twice");
        h.assertTrue(l.getBlockState(naturalSite).is(Blocks.GOLD_BLOCK),"Structure replaced surrounding biome terrain");
        h.assertTrue(l.getEntitiesOfClass(TimeflowEel.class,new AABB(b.minX(),b.minY(),b.minZ(),
                b.maxX()+1,b.maxY()+1,b.maxZ()+1)).isEmpty(),"Captured fish spawned with the structure");
        h.assertTrue(l.getBlockState(origin.offset(16,3,14)).is(com.mcmagic.omnira.registry.ModBlocks.MEMORY_CUBE.get()),"Bridge relic missing");
        var ball=(CrystalBallBlockEntity)l.getBlockEntity(origin.offset(29,1,27));
        h.assertTrue(ball.hasPendingLoot(),"Generated treasure not misted");MillPlan.initialize(ball);
        for(var c:MillPlan.CELLS){
            var p=c.pos().offset(origin);
            var expected=net.minecraft.world.level.block.Block.updateFromNeighbourShapes(KirisameShopPlan.state(c),l,p);
            h.assertTrue(l.getBlockState(p).equals(expected),"Blueprint cell differs: "+c.pos()+" expected "+expected+" actual "+l.getBlockState(p));
        }
        h.assertTrue(ball.getPersistentData().hasUUID(MillTreasures.TOKEN),"Generated mill failed to register");
        var treeData=MireTrees.get(l).save(new net.minecraft.nbt.CompoundTag(),l.registryAccess());
        for(var raw:treeData.getList("Trees",10)){
            var root=BlockPos.of(((net.minecraft.nbt.CompoundTag)raw).getLong("Root"));
            h.assertTrue(!b.isInside(root),"Sample tree registered as part of the generated mill");
        }
        MillTreasures.get(l).pulse(l,6000);
        for(var site:MillPlan.sites(origin))h.assertTrue(site.reversed()?l.getBlockEntity(site.pos()) instanceof CrystalBallBlockEntity:l.isEmptyBlock(site.pos()),"Generated timeline did not switch");
        h.succeed();
    }
}
