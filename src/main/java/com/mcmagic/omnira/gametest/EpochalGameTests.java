package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.archaeology.*;
import com.mcmagic.omnira.time.StilledTime;
import com.mcmagic.omnira.spacetime.CorridorLayout;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_epochal")
@PrefixGameTestTemplate(false)
public final class EpochalGameTests {
    @GameTest(template="spell_arena")
    public static void livingExemption(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,3,4));
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(StilledTime.BIOME);
        level.getChunkAt(p).fillBiomesFromNoise((x,y,z,s)->biome,level.getChunkSource().randomState().sampler());
        var cow=EntityType.COW.create(level);cow.moveTo(p.getCenter());
        var item=new ItemEntity(level,p.getX(),p.getY(),p.getZ(),new ItemStack(Items.STICK));
        h.assertTrue(!StilledTime.stopped(cow),"Living entities must remain active");
        h.assertTrue(StilledTime.stopped(item),"Dropped items must freeze");
        h.assertTrue(StilledTime.stopped(level,p),"Block time must stop");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void drillPersistence(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,5,5));
        HyperDrillBlockEntity.install(level,p,300);
        h.assertTrue(HyperDrillBlock.parts(level,p).size()==18,"Drill is incomplete");
        var be=(HyperDrillBlockEntity)level.getBlockEntity(p);
        h.assertTrue(HyperDrillItem.charge(be.item())==300,"Partial charge lost");
        for(int i=0;i<3;i++)be.interact(h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL),new ItemStack(com.mcmagic.omnira.registry.TimeNatureContent.SPATIAL_SHARD.get()));
        h.assertTrue(be.charge()==1200,"Crystal charge not additive");
        h.assertTrue(ArchaeologyContent.BRUSH.get().getDefaultInstance().getMaxDamage()==128,"Brush durability");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void strata(GameTestHelper h){
        for(boolean exposure:new boolean[]{false,true}){
            var column=new EpochalTerrain.Column(224,224,exposure);int[] counts=new int[5];int separators=0;
            for(int y=171;y<=224;y++){int era=EpochalTerrain.era(y,column);if(era<0)separators++;else counts[era]++;}
            h.assertTrue(separators==4,"Four single-block separators required");
            for(int count:counts)h.assertTrue(count>=10,"Stratum thinner than ten blocks");
        }
        h.assertTrue(CorridorLayout.block(0,170,0).getDestroySpeed(h.getLevel(),BlockPos.ZERO)<0,"Continent base not protected");
        h.assertTrue(CorridorLayout.block(0,103,0).getDestroySpeed(h.getLevel(),BlockPos.ZERO)<0,"Cave ceiling not protected");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void constantDescentAndStop(GameTestHelper h){
        var level=h.getLevel();var root=h.absolutePos(new BlockPos(5,8,5));
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(int y=-5;y<=2;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setBlock(root.offset(x,y,z),(y==-5?Blocks.BEDROCK:y==-2?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        var rig=ArchaeologyContent.MOVING_RIG.get().create(level);rig.start(root,player);level.addFreshEntity(rig);
        for(int i=0;i<10;i++)rig.tick();
        h.assertTrue(Math.abs(rig.getY()-(root.getY()-1))<.0001,"Air changed descent speed");
        for(int i=0;i<10;i++)rig.tick();
        h.assertTrue(Math.abs(rig.getY()-(root.getY()-2))<.0001,"Solid layer changed descent speed");
        for(int i=0;i<22;i++)rig.tick();
        h.assertTrue(rig.isRemoved(),"Rig did not stop above bedrock");
        var bottom=root.below(4);h.assertTrue(HyperDrillBlock.parts(level,bottom).size()==18,"Stopped rig is incomplete");
        h.assertTrue(((HyperDrillBlockEntity)level.getBlockEntity(bottom)).charge()==0,"Stopped rig retained charge");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(root).inflate(8)).isEmpty(),"Excavation spawned drops");h.succeed();
    }
}
