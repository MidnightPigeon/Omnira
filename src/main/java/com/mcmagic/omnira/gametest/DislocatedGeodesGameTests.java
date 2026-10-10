package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.geodes.*;
import com.mcmagic.omnira.spacetime.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_geodes")
@PrefixGameTestTemplate(false)
public final class DislocatedGeodesGameTests {
    private static void biome(net.minecraft.server.level.ServerLevel level,BlockPos pos,Holder<net.minecraft.world.level.biome.Biome> biome){
        // Biome lookup jitters into neighbouring quart cells, including across chunk boundaries.
        for(int x=(pos.getX()-8)>>4;x<=(pos.getX()+8)>>4;x++)for(int z=(pos.getZ()-8)>>4;z<=(pos.getZ()+8)>>4;z++)
            level.getChunk(x,z).fillBiomesFromNoise((a,b,c,s)->biome,level.getChunkSource().randomState().sampler());
    }
    @GameTest(template="spell_arena") public static void clusterRerollsEachBlockWithoutLeavingField(GameTestHelper h){
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(6,4,6));
        level.setBlockAndUpdate(at.below(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(at,DeflectionContent.CLUSTER.get().defaultBlockState());
        var pig=h.spawn(EntityType.PIG,new BlockPos(8,4,6));pig.setNoAi(true);pig.setNoGravity(true);pig.setYRot(0);
        var expected=net.minecraft.util.RandomSource.create(8120);level.random.setSeed(8120);
        var data=pig.getPersistentData();int sign=0,left=0,right=0;
        for(int step=0;step<64;step++){
            if(step%4==0){sign=expected.nextBoolean()?1:-1;if(sign<0)left++;else right++;}
            data.putLong("DeflectionSampleTick",level.getGameTime()-1);data.putString("DeflectionDimension",level.dimension().location().toString());
            data.putDouble("DeflectionX",pig.getX()-.25);data.putDouble("DeflectionY",pig.getY());data.putDouble("DeflectionZ",pig.getZ());data.putBoolean("DeflectionInside",true);
            float before=pig.getYRot();DeflectionMotion.sample(pig);
            h.assertTrue(Math.abs(pig.getYRot()-before-sign*1.25)<.001,"Random direction not renewed at distance boundary: "+step);
        }
        h.assertTrue(left>0&&right>0,"Continuous field traversal stayed one-sided");
        h.assertTrue(!data.contains("DeflectionCurveSign")&&data.getDouble("DeflectionRemainder")==0,"Finished segment retained direction");
        pig.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void floorBiomeAndSigilDoNotProtectFromClusters(GameTestHelper h){
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(7,4,7));
        var previous=level.getBiome(at);var biomes=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var geodes=biomes.getHolderOrThrow(DeflectionContent.BIOME);var plains=biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(Vec3.atBottomCenterOf(at));
        var block=DeflectionContent.BLOCK.get();var state=block.defaultBlockState();
        try{
            biome(level,at,plains);
            block.stepOn(level,at.below(),state,player);h.assertTrue(!player.getPersistentData().contains("DeflectionPending"),"Floor active outside geodes");
            biome(level,at,geodes);
            block.stepOn(level,at.below(),state,player);h.assertTrue(Math.abs(player.getPersistentData().getFloat("DeflectionPending"))==10,"Floor inactive in geodes");
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("charm",0,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.SPACETIME_ANCHORING_SIGIL.get()));
            DeflectionMotion.advance(player);h.assertTrue(!player.getPersistentData().contains("DeflectionPending"),"Sigil did not cancel floor steering");
            block.stepOn(level,at.east(),state,player);h.assertTrue(!player.getPersistentData().contains("DeflectionPending"),"Sigil failed to prevent new contact");
            biome(level,at,plains);
            level.setBlockAndUpdate(at.west().below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(at.west(),DeflectionContent.CLUSTER.get().defaultBlockState());
            var data=player.getPersistentData();data.putLong("DeflectionSampleTick",level.getGameTime()-1);data.putString("DeflectionDimension",level.dimension().location().toString());
            data.putDouble("DeflectionX",player.getX()-1);data.putDouble("DeflectionY",player.getY());data.putDouble("DeflectionZ",player.getZ());data.putBoolean("DeflectionInside",true);
            player.setYRot(0);DeflectionMotion.sample(player);h.assertTrue(Math.abs(player.getYRot())==5,"Sigil or biome incorrectly suppressed cluster");
        }finally{biome(level,at,previous);player.discard();}
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=30) public static void fractionalStepsAccumulateWithoutTurningAtRest(GameTestHelper h){
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(6,4,6));
        level.setBlockAndUpdate(at.below(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(at,DeflectionContent.CLUSTER.get().defaultBlockState());
        var pig=h.spawn(EntityType.PIG,new BlockPos(8,4,6));pig.setNoAi(true);pig.setNoGravity(true);pig.setYRot(0);
        var start=pig.position();var data=pig.getPersistentData();
        for(int i=1;i<=4;i++){
            data.putLong("DeflectionSampleTick",level.getGameTime()-1);data.putString("DeflectionDimension",level.dimension().location().toString());
            data.putDouble("DeflectionX",pig.getX());data.putDouble("DeflectionY",pig.getY());data.putDouble("DeflectionZ",pig.getZ());data.putBoolean("DeflectionInside",true);
            pig.setPos(start.add(0,0,i*.25));DeflectionMotion.sample(pig);
            h.assertTrue(Math.abs(Math.abs(pig.getYRot())-i*1.25)<.001,"Fractional movement did not curve continuously");
        }
        h.assertTrue(Math.abs(pig.getYRot())==5,"Four quarter-steps did not turn five degrees");
        h.runAfterDelay(9,()->{h.assertTrue(Math.abs(pig.getYRot())==5,"Standing still continued turning");pig.discard();h.succeed();});
    }
    @GameTest(template="spell_arena") public static void oldBiomeSourceLoadsGeodesWithoutTouchingUpperLayer(GameTestHelper h){
        try(var reader=h.getLevel().getServer().getResourceManager().getResource(net.minecraft.resources.ResourceLocation.parse("omnira:dimension/spacetime_corridor.json")).orElseThrow().openAsReader()){
            var data=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");
            data.remove("geodes_biome");
            var source=net.minecraft.world.level.biome.BiomeSource.CODEC.parse(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,h.getLevel().registryAccess()),data).getOrThrow();
            h.assertTrue(source.possibleBiomes().stream().anyMatch(b->b.is(DeflectionContent.BIOME)),"Old source omitted new biome");
            for(int x=-2048;x<=2048;x+=64)for(int z=-2048;z<=2048;z+=64)if(DislocatedTerrain.biome(x,z)){
                h.assertTrue(source.getNoiseBiome(x>>2,10,z>>2,null).is(DeflectionContent.BIOME),"Geodes region not selected");
                h.assertTrue(!source.getNoiseBiome(x>>2,50,z>>2,null).is(DeflectionContent.BIOME),"Geodes leaked into time continent");
                h.succeed();return;
            }
            h.fail("No geodes region found");
        }catch(java.io.IOException ex){throw new RuntimeException(ex);}
    }
    @GameTest(template="spell_arena") public static void floorTurnsOnlyOnNewContact(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(7,3,7));var level=h.getLevel();
        var previous=level.getBiome(pos);
        var geodes=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(DeflectionContent.BIOME);
        biome(level,pos,geodes);
        var pig=h.spawn(EntityType.PIG,new BlockPos(7,4,7));pig.setNoAi(true);pig.setNoGravity(true);
        var before=pig.position();var velocity=new Vec3(.1,.2,.3);pig.setDeltaMovement(velocity);
        pig.setYRot(0);
        var block=DeflectionContent.BLOCK.get();var state=block.defaultBlockState();
        block.stepOn(level,pos,state,pig);float pending=pig.getPersistentData().getFloat("DeflectionPending");
        h.assertTrue(pig.getYRot()==0&&Math.abs(pending)==10,"Floor must queue, not snap, ten degrees");
        block.stepOn(level,pos,state,pig);h.assertTrue(pig.getPersistentData().getFloat("DeflectionPending")==pending,"Same contact queued repeatedly");
        for(int i=1;i<=10;i++){DeflectionMotion.advance(pig);h.assertTrue(Math.abs(pig.getYRot())==i,"Floor turn was not progressive");}
        block.stepOn(level,pos.east(),state,pig);h.assertTrue(Math.abs(pig.getPersistentData().getFloat("DeflectionPending"))==10,"New block did not queue a turn");
        h.assertTrue(pig.position().equals(before)&&Math.abs(pig.getDeltaMovement().length()-velocity.length())<.00001&&pig.getDeltaMovement().y==velocity.y,"Deflection changed position or speed");
        h.assertTrue(!pig.getDeltaMovement().equals(velocity),"Travel did not bend with heading");
        biome(level,pos,previous);
        pig.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void growthAndStageRadii(GameTestHelper h){
        var root=h.absolutePos(new BlockPos(6,4,6));var level=h.getLevel();
        level.setBlockAndUpdate(root,DeflectionContent.MATRIX.get().defaultBlockState());
        var pig=h.spawn(EntityType.PIG,new BlockPos(7,5,6));pig.setNoAi(true);pig.setNoGravity(true);
        for(int i=0;i<4;i++){
            h.assertTrue(DeflectionMatrixBlock.grow(level,root,Direction.UP),"Growth failed at stage "+i);
            var state=level.getBlockState(root.above());h.assertTrue(state.is(DeflectionContent.stages()[i]),"Wrong growth stage");
            int radius=i+2;var center=Vec3.atCenterOf(root.above());
            pig.setPos(center.add(radius,0,0));h.assertTrue(DeflectionMotion.coverage(level,pig)==1,"Inside stage radius ignored");
            pig.setPos(center.add(radius+1,0,0));h.assertTrue(DeflectionMotion.coverage(level,pig)==0,"Stage radius too large");
        }
        h.assertTrue(!DeflectionMatrixBlock.grow(level,root,Direction.UP),"Mature cluster grew again");
        var cycled=com.mcmagic.omnira.time.RecurrenceGarden.next(level,root.above(),level.getBlockState(root.above()));
        h.assertTrue(cycled.is(DeflectionContent.SMALL.get()),"Garden cycle omitted new crystal");
        pig.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void overlappingClustersAddFiveDegreesEach(GameTestHelper h){
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(7,4,7));
        for(var p:new BlockPos[]{at.west(),at.east()}){
            level.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(p,DeflectionContent.CLUSTER.get().defaultBlockState());
        }
        var pig=h.spawn(EntityType.PIG,new BlockPos(7,4,8));pig.setNoAi(true);pig.setNoGravity(true);
        var before=pig.position();var data=pig.getPersistentData();
        pig.setYRot(0);
        data.putLong("DeflectionSampleTick",level.getGameTime()-1);data.putString("DeflectionDimension",level.dimension().location().toString());
        data.putDouble("DeflectionX",pig.getX()-1);data.putDouble("DeflectionY",pig.getY());data.putDouble("DeflectionZ",pig.getZ());data.putBoolean("DeflectionInside",true);
        DeflectionMotion.sample(pig);h.assertTrue(Math.abs(pig.getYRot())==10,"Two clusters did not stack to ten degrees");
        h.assertTrue(pig.position().equals(before),"Cluster changed position");
        float yaw=pig.getYRot();DeflectionMotion.sample(pig);h.assertTrue(yaw==pig.getYRot(),"Sampled movement twice in one tick");
        pig.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void terrainHasCurvedFloorAndTipMatrices(GameTestHelper h){
        int blocks=0,rough=0,matrices=0;
        int originX=0,originZ=0;boolean found=false;
        search:for(int x=-2048;x<=2048;x+=64)for(int z=-2048;z<=2048;z+=64)
            if(DislocatedTerrain.biome(x,z)&&DislocatedTerrain.biome(x-64,z-64)&&DislocatedTerrain.biome(x+64,z+64)){
                originX=x;originZ=z;found=true;break search;
            }
        h.assertTrue(found,"No geodes region found");
        for(int x=originX-128;x<=originX+128;x+=4)for(int z=originZ-128;z<=originZ+128;z+=4){
            if(!DislocatedTerrain.biome(x,z))continue;
            var floor=CorridorLayout.block(x,DislocatedTerrain.floor(x,z),z);
            if(floor.is(DeflectionContent.BLOCK.get()))blocks++;else rough++;
            h.assertTrue(!floor.isAir(),"Hole in curved floor");
            h.assertTrue(DislocatedTerrain.floor(x,z)==com.mcmagic.omnira.space.FoldedTerrain.floor(x,z),"Caves disagree on boundary height");
        }
        for(int x=originX-64;x<=originX+64;x++)for(int z=originZ-64;z<=originZ+64;z++){
            if(!DislocatedTerrain.biome(x,z))continue;
            for(int y=DislocatedTerrain.floor(x,z)+7;y<=DislocatedTerrain.floor(x,z)+15;y++)if(DislocatedTerrain.block(x,y,z).is(DeflectionContent.MATRIX.get())){
                matrices++;h.assertTrue(DeflectionContent.radius(DislocatedTerrain.block(x,y+1,z))>0,"Tip lacks exposed cluster");
            }
        }
        h.assertTrue(blocks>rough&&rough>0&&matrices>0,"Crystal replacement or tip matrices missing: "+blocks+" / "+rough+" / "+matrices);h.succeed();
    }
}
