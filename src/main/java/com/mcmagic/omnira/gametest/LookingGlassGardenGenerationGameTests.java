package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.time.LookingGlassGardenPiece;
import com.mcmagic.omnira.time.LookingGlassGardenPlan;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_garden_generation")
@PrefixGameTestTemplate(false)
public final class LookingGlassGardenGenerationGameTests {
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void authoredFixturesAndUnopenedLoot(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absolutePos(new BlockPos(96,8,96));
        var registered=level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.parse("omnira:looking_glass_garden"));
        helper.assertTrue(registered!=null,"Garden structure missing from registry");
        var piece=new LookingGlassGardenPiece(origin);var box=piece.getBoundingBox();
        var natural=origin.offset(1,0,1);
        level.setBlockAndUpdate(natural,Blocks.GOLD_BLOCK.defaultBlockState());
        for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++){
            level.getChunk(x,z);
            var chunk=new ChunkPos(x,z);
            var clip=new BoundingBox(chunk.getMinBlockX(),level.getMinBuildHeight(),chunk.getMinBlockZ(),
                    chunk.getMaxBlockX(),level.getMaxBuildHeight()-1,chunk.getMaxBlockZ());
            piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),
                    RandomSource.create(7),clip,chunk,origin);
        }
        helper.assertTrue(level.getBlockState(natural).is(Blocks.GOLD_BLOCK),"Natural garden terrain was copied into the structure");
        helper.assertTrue(LookingGlassGardenPlan.cells().size()==587,"Captured garden fixture count changed");
        var allowed=Set.of("omnira:mirror_rock","omnira:engraved_mirror_rock",
                "omnira:solidified_light_crystal_core","omnira:crystal_ball","omnira:wonderland_poker_stand");
        for(var cell:LookingGlassGardenPlan.cells()){
            helper.assertTrue(allowed.contains(cell.name()),"Natural sample block leaked into plan: "+cell.name());
            helper.assertTrue(level.getBlockState(cell.pos().offset(origin)).is(
                    com.mcmagic.omnira.shop.KirisameShopPlan.state(cell).getBlock()),"Missing fixture at "+cell.pos());
        }
        for(var offset:new BlockPos[]{new BlockPos(20,1,8),new BlockPos(32,1,20),
                new BlockPos(20,1,32),new BlockPos(8,1,20)}){
            var ball=(CrystalBallBlockEntity)level.getBlockEntity(offset.offset(origin));
            helper.assertTrue(ball!=null&&ball.hasPendingLoot(),"Garden treasure must start unopened: "+offset);
        }
        helper.assertTrue(level.getBlockState(origin.offset(20,1,20)).is(
                com.mcmagic.omnira.registry.ModBlocks.WONDERLAND_POKER_STAND.get()),"Central stand missing");
        helper.succeed();
    }
}
