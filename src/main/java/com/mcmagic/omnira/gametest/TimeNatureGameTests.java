package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.time.*;
import com.mcmagic.omnira.spacetime.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_time_nature")
@PrefixGameTestTemplate(false)
public final class TimeNatureGameTests {
    @GameTest(template="spell_arena")
    public static void soilsAndRecipes(GameTestHelper h){
        for(var soil:new Block[]{DreamContent.TEMPORAL_SILT.get(),DreamContent.LIVING_TEMPORAL_SILT.get(),TimeNatureContent.SILT.get(),TimeNatureContent.LIVING_SILT.get()})h.assertTrue(soil.defaultBlockState().is(TemporalSoils.SILTS),"Missing shared silt tag");
        h.assertTrue(TemporalSoils.timeTree(Blocks.END_STONE.defaultBlockState()),"Time sapling cannot use end stone");
        h.assertTrue(!TemporalSoils.timeTree(DreamContent.MOSSY_SHADOW_ROCK.get().defaultBlockState()),"Shadow rock admitted a time sapling through dirt tag");
        h.assertTrue(TemporalSoils.shadowTree(DreamContent.SHADOW_ROCK.get().defaultBlockState()),"Shadow tree lost native substrate");
        h.assertTrue(TimeNatureContent.LOG.get().defaultBlockState().is(BlockTags.LOGS),"Time logs absent from wood tags");
        for(String id:new String[]{"time_warp_point","excited_rough_spatial_crystal_block"}){
            var recipe=h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira","condensation/"+id)).orElseThrow();
            h.assertTrue(recipe.value() instanceof com.mcmagic.omnira.recipe.CondensationRecipe c&&c.advanced()&&c.manaCost()==300,"Wrong advanced recipe cost");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void crystalStagesAndHarvest(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(6,4,6));level.setBlockAndUpdate(pos,TimeNatureContent.SPATIAL_MATRIX.get().defaultBlockState());
        var stages=new Block[]{TimeNatureContent.SMALL_SPATIAL_BUD.get(),TimeNatureContent.MEDIUM_SPATIAL_BUD.get(),TimeNatureContent.LARGE_SPATIAL_BUD.get(),TimeNatureContent.SPATIAL_CLUSTER.get()};
        for(var stage:stages){h.assertTrue(SpatialMatrixBlock.grow(level,pos,Direction.UP),"Growth step failed");h.assertTrue(level.getBlockState(pos.above()).is(stage),"Wrong growth order");}
        h.assertTrue(!SpatialMatrixBlock.grow(level,pos,Direction.UP),"Mature cluster grew again");
        h.assertTrue(Block.getDrops(level.getBlockState(pos),level,pos,null).isEmpty(),"Matrix has drops");
        var drops=Block.getDrops(DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState(),level,pos,null);
        h.assertTrue(drops.size()==1&&drops.getFirst().is(DreamContent.SPATIAL_CRYSTAL.get().asItem()),"Excited block did not become rough crystal");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void dormantSaplingAndPersistentLeaves(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(6,3,6));
        level.setBlockAndUpdate(pos.below(),TimeNatureContent.SILT.get().defaultBlockState());level.setBlockAndUpdate(pos,Blocks.OAK_SAPLING.defaultBlockState());
        for(int n=0;n<30;n++)((SaplingBlock)Blocks.OAK_SAPLING).performBonemeal(level,level.random,pos,level.getBlockState(pos));
        h.assertTrue(level.getBlockState(pos).is(Blocks.OAK_SAPLING)&&level.getBlockState(pos).getValue(SaplingBlock.STAGE)==0,"Bone meal bypassed temporal dormancy");
        var leaf=TimeNatureContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE,7).setValue(LeavesBlock.PERSISTENT,false);
        level.setBlockAndUpdate(pos.above(3),leaf);leaf.randomTick(level,pos.above(3),level.random);
        h.assertTrue(level.getBlockState(pos.above(3)).is(TimeNatureContent.LEAVES.get()),"Time leaves decayed");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void bloomAndSeed(GameTestHelper h){
        var level=h.getLevel();var soil=h.absolutePos(new BlockPos(6,2,6));
        level.setBlockAndUpdate(soil,DreamContent.TEMPORAL_SILT.get().defaultBlockState());
        h.assertTrue(TemporalBloom.spread(level,soil),"Bloom failed outside forest");
        h.assertTrue(level.getBlockState(soil.above()).is(TimeNatureContent.GRASS.get())||level.getBlockState(soil.above()).is(TimeNatureContent.FLOWER.get()),"Bloom produced another plant");
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var stack=new ItemStack(TimeNatureContent.TIME_SEED.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var target=soil.offset(2,0,0);level.setBlockAndUpdate(target,DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState());
        stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(target.getCenter(),Direction.UP,target,false)));
        h.assertTrue(level.getBlockState(target).is(TimeNatureContent.SPATIAL_MATRIX.get()),"Seed did not create matrix");h.succeed();
    }
    private static net.minecraft.server.level.ServerLevel forest(GameTestHelper h){
        var level=h.getLevel();var pos=forestPos(h);var holder=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(FleetingTime.BIOME);
        var center=new net.minecraft.world.level.ChunkPos(pos);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var chunk=level.getChunk(center.x+x,center.z+z);
            chunk.fillBiomesFromNoise((qx,qy,qz,sampler)->holder,level.getChunkSource().randomState().sampler());chunk.setUnsaved(true);
        }
        return level;
    }
    private static BlockPos forestPos(GameTestHelper h){
        return h.absolutePos(new BlockPos(8,300,8));
    }
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void regionalTimersAndMovement(GameTestHelper h){
        var level=forest(h);var pos=forestPos(h);level.getChunkAt(pos);
        h.assertTrue(level.getBiome(pos).is(FleetingTime.BIOME),"Forest registry or source missing");
        var zombie=EntityType.ZOMBIE.create(level);zombie.setPos(pos.getCenter());zombie.setNoAi(true);zombie.setNoGravity(true);
        zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,100));var before=zombie.position();
        level.tickNonPassenger(zombie);
        h.assertTrue(zombie.getEffect(MobEffects.MOVEMENT_SPEED).getDuration()==98,"Potion time did not advance twice");
        FleetingTime.extra(zombie,()->zombie.move(MoverType.SELF,new Vec3(1,0,0)),true);
        h.assertTrue(zombie.position().distanceToSqr(before)<.0001,"Bonus tick moved the entity");
        h.assertTrue(!FleetingTime.inBonus(),"Bonus context leaked");zombie.discard();h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void timeTreeShapeAndGeneration(GameTestHelper h){
        var level=forest(h);var pos=forestPos(h).atY(225);level.getChunkAt(pos);
        for(var at:BlockPos.betweenClosed(pos.offset(-6,0,-6),pos.offset(6,20,6)))level.setBlock(at,Blocks.AIR.defaultBlockState(),2);
        level.setBlockAndUpdate(pos.below(),TimeNatureContent.SILT.get().defaultBlockState());
        var config=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(ResourceLocation.fromNamespaceAndPath("omnira","time_tree"));
        h.assertTrue(config.place(level,level.getChunkSource().getGenerator(),level.random,pos),"Time tree failed to grow");
        h.assertTrue(level.getBlockState(pos).is(TimeNatureContent.LOG.get()),"Missing tree trunk");
        h.assertTrue(level.getBlockState(pos.below()).is(TimeNatureContent.SILT.get()),"Time tree replaced its substrate");
        int logs=0,leaves=0;for(var at:BlockPos.betweenClosed(pos.offset(-6,0,-6),pos.offset(6,20,6))){var s=level.getBlockState(at);if(s.is(TimeNatureContent.LOG.get()))logs++;if(s.is(TimeNatureContent.LEAVES.get()))leaves++;}
        h.assertTrue(logs>=17&&leaves>=80,"Incomplete eucalyptus crown or branches");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void acceleratedFurnace(GameTestHelper h){
        var level=forest(h);var pos=forestPos(h).offset(14,-1,0);var chunk=new net.minecraft.world.level.ChunkPos(pos);
        level.getChunkAt(pos);level.setChunkForced(chunk.x,chunk.z,true);
        level.setBlockAndUpdate(pos,Blocks.FURNACE.defaultBlockState());
        var furnace=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)level.getBlockEntity(pos);
        furnace.setItem(0,new ItemStack(Items.SAND,8));furnace.setItem(1,new ItemStack(Items.COAL,2));
        h.runAfterDelay(50,()->{
            try{
                int elapsed=furnace.saveWithoutMetadata(level.registryAccess()).getShort("CookTime");
                h.assertTrue(elapsed>=85&&elapsed<=102,"Furnace was not ticking at double speed: "+elapsed);h.succeed();
            }finally{level.removeBlock(pos,false);level.setChunkForced(chunk.x,chunk.z,false);}
        });
    }
}
