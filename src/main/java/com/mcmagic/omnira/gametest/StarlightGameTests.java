package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.sea.*;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_starlight")
@PrefixGameTestTemplate(false)
public final class StarlightGameTests {
    @GameTest(template="spell_arena",timeoutTicks=200) public static void lowSeaLighthouseHasSlopingFoot(GameTestHelper h){
        BlockPos anchor=null;
        for(int x=-2048;x<=2048&&anchor==null;x+=32)for(int z=-2048;z<=2048;z+=32){
            if(VoidSeaTerrain.biome(x,z)&&com.mcmagic.omnira.spacetime.TimeBiomeLayout.sample(x+8192,z-4096).edgeDistance()>40){
                anchor=new BlockPos(x-12,VoidLighthousePiece.BASE_Y,z-12);break;
            }
        }
        h.assertTrue(anchor!=null,"No interior ocean for lighthouse test");
        int low=0,high=0;var profile=new com.google.gson.JsonArray();
        for(int dx=-24;dx<=24;dx++)for(int dz=-24;dz<=24;dz++){
            int x=anchor.getX()+12+dx,z=anchor.getZ()+12+dz;
            int floor=VoidSeaTerrain.floor(x,z),top=VoidLighthousePiece.islandTop(anchor,x,z);
            h.assertTrue(top>=floor&&top<=anchor.getY(),"Invalid island column");
            if(top>=35)low++;if(top>=VoidSeaTerrain.SEA_LEVEL+1)high++;
            if(dz==0){var c=new com.google.gson.JsonObject();c.addProperty("x",dx);c.addProperty("floor",floor);c.addProperty("top",top);profile.add(c);}
        }
        h.assertTrue(low>high*1.4,"Island foot still has vertical pillar walls");
        h.assertTrue(VoidLighthousePiece.BASE_Y==42,"Tower did not follow lowered sea");
        var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(h.getLevel());
        var piece=new VoidLighthousePiece(anchor);var saved=piece.createTag(context);
        var restored=new VoidLighthousePiece(context,saved);
        h.assertTrue(saved.equals(restored.createTag(context)),"Island anchor lost on reload");
        var level=h.getLevel();
        restored.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),level.random,
                restored.getBoundingBox(),new net.minecraft.world.level.ChunkPos(anchor),anchor);
        h.assertTrue(level.getBlockState(anchor.offset(12,19,12)).is(StarlightContent.SOURCE.get()),"Tower placed at stale height");
        h.assertTrue(level.getBlockState(anchor.offset(12,17,12)).is(com.mcmagic.omnira.registry.ModBlocks.CRYSTAL_BALL.get()),"Upper loot ball lost");
        try{
            var path=java.nio.file.Path.of("../../build/previews/lighthouse-island-profile.json");java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path,profile.toString());
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void dustIsThinInstantHarvest(GameTestHelper h) {
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,4,4));var s=StarlightContent.DUST.get().defaultBlockState();
        l.setBlockAndUpdate(p,s);
        h.assertTrue(s.getDestroySpeed(l,p)==0,"Scatter must break in one hit");
        h.assertTrue(s.getShape(l,p).max(Direction.Axis.Y)<=1.0/16,"Scatter floats above the floor");
        var drops=net.minecraft.world.level.block.Block.getDrops(s,l,p,l.getBlockEntity(p));
        h.assertTrue(drops.size()==1&&drops.getFirst().is(StarlightContent.DUST_ITEM.get())&&drops.getFirst().getCount()==1,"Wrong harvested powder");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void sourceSphereAndRemoval(GameTestHelper h) {
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,3,5));
        l.setBlockAndUpdate(p,StarlightContent.SOURCE.get().defaultBlockState());
        ((StarlightEntity)l.getBlockEntity(p)).onLoad();
        Vec3 c=Vec3.atCenterOf(p);
        h.assertTrue(StarlightEntity.protectedAt(l,c.add(0,64,0)),"Vertical radius missing");
        h.assertTrue(!StarlightEntity.protectedAt(l,c.add(0,64.01,0)),"Beyond sphere protected");
        h.assertTrue(!StarlightEntity.protectedAt(l,c.add(46,46,0)),"Used cube instead of sphere");
        var zombie=EntityType.ZOMBIE.create(l);zombie.setPos(c);
        var event=new MobSpawnEvent.PositionCheck(zombie,l,MobSpawnType.NATURAL,null);
        StarlightEntity.preventSpawn(event);h.assertTrue(event.getResult()==MobSpawnEvent.PositionCheck.Result.FAIL,"Hostile spawn allowed");
        var jelly=VoidSeaContent.JELLYFISH.get().create(l);jelly.setPos(c);
        var neutral=new MobSpawnEvent.PositionCheck(jelly,l,MobSpawnType.NATURAL,null);
        StarlightEntity.preventSpawn(neutral);h.assertTrue(neutral.getResult()==MobSpawnEvent.PositionCheck.Result.DEFAULT,"Neutral spawn denied");
        l.removeBlock(p,false);h.assertTrue(!StarlightEntity.protectedAt(l,c),"Broken source still protects");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void darkWaterAndSupport(GameTestHelper h) {
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(7,5,7));
        for(var q:BlockPos.betweenClosed(p.offset(-2,-2,-2),p.offset(2,2,2)))
            l.setBlockAndUpdate(q,Blocks.STONE.defaultBlockState());
        l.setBlockAndUpdate(p,Blocks.WATER.defaultBlockState());
        h.runAfterDelay(30,()->{
            h.assertTrue(StarlightEntity.eligible(l,p.above(10),p),"Dark supported water rejected: light="+l.getMaxLocalRawBrightness(p));
            h.assertTrue(StarlightEntity.place(l,p.above(10),p),"Stardust not placed");
            h.assertTrue(l.getBlockState(p).getLightEmission(l,p)==12&&l.getFluidState(p).isSource(),"Light or fluid not retained");
            h.assertTrue(!(StarlightContent.DUST_ITEM.get() instanceof BlockItem),"Harvested dust is placeable");
            h.assertTrue(!StarlightEntity.eligible(l,p,p),"Existing dust overwritten");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void lighthouseTemplateIntegrity(GameTestHelper h) {
        var cells=VoidLighthousePiece.CELLS;
        h.assertTrue(cells.stream().filter(c->c.name().equals("omnira:star_source")).count()==1,"Exactly one source required");
        h.assertTrue(cells.stream().filter(c->c.name().equals("omnira:stardust")).count()>=4,"Island lights missing");
        for(var c:cells)KirisameShopPlan.state(c);
        h.assertTrue(cells.stream().filter(c->c.name().endsWith("_stairs")).count()==17,"Staircase incomplete");
        h.assertTrue(cells.stream().anyMatch(c->c.pos().equals(new BlockPos(12,19,12))&&c.name().equals("omnira:star_source")),"Source outside lantern chamber");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=260) public static void generationWaitsTenSeconds(GameTestHelper h) {
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(7,5,7));var source=p.above(10);
        for(var q:BlockPos.betweenClosed(p.offset(-2,-2,-2),p.offset(2,2,2)))l.setBlockAndUpdate(q,Blocks.STONE.defaultBlockState());
        l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        l.setBlockAndUpdate(source,StarlightContent.SOURCE.get().defaultBlockState());
        h.runAfterDelay(190,()->h.assertTrue(l.getBlockState(p).isAir(),"Stardust emitted before 10 seconds"));
        h.runAfterDelay(230,()->{
            h.assertTrue(l.getBlockState(p).is(StarlightContent.DUST.get()),"Dark ground not seeded after interval");
            l.removeBlock(source,false);h.succeed();
        });
    }
}
