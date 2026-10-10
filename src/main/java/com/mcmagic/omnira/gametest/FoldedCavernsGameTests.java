package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_folded_caverns")
@PrefixGameTestTemplate(false)
public final class FoldedCavernsGameTests {
    @GameTest(template="spell_arena") public static void harvestingElementsAreAsymmetric(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        for(boolean spatial:new boolean[]{false,true})for(String operation:new String[]{"construction","dissociation"}){
            var block=(spatial?ModBlocks.SPACE_WARP_POINT:ModBlocks.TIME_WARP_POINT).get();
            level.setBlockAndUpdate(pos,block.defaultBlockState());
            com.mcmagic.omnira.spell.UtilitySpellEffects.applyBlock(
                    com.mcmagic.omnira.spell.SpellEffect.utility(operation,0,0,0),player,pos,Direction.UP,pos);
            boolean expected=spatial==operation.equals("dissociation");
            h.assertTrue(level.getBlockState(pos).is(expected
                    ?(spatial?ModBlocks.SPATIAL_REMNANT:ModBlocks.TEMPORAL_MOTE).get():block),
                    "Incorrect harvest for "+operation+" spatial="+spatial);
            level.removeBlock(pos,false);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void chargeTeleportsAfterSixtyTicks(GameTestHelper h){
        var level=h.getLevel();var source=h.absolutePos(new BlockPos(5,3,5));var target=h.absolutePos(new BlockPos(8,12,8));
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)h.setBlock(new BlockPos(x,10,z),Blocks.STONE);
        level.setBlockAndUpdate(source,ModBlocks.SPACE_WARP_POINT.get().defaultBlockState());level.setBlockAndUpdate(target,ModBlocks.SPACE_WARP_POINT.get().defaultBlockState());
        SpaceWarpIndex.get(level).add(source);SpaceWarpIndex.get(level).add(target);
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(5,3,6));pig.setNoAi(true);pig.setNoGravity(true);
        h.runAfterDelay(58,()->h.assertTrue(pig.getPersistentData().getLong("OmniraFoldCooldown")<=level.getGameTime(),"Fold fired before three seconds"));
        h.runAfterDelay(65,()->{
            h.assertTrue(pig.getPersistentData().getLong("OmniraFoldCooldown")>level.getGameTime()&&pig.getY()>target.getY()-2,"Fold did not reach upper platform after three seconds");
            h.assertTrue(!SpaceWarpBlockEntity.touches(pig.getBoundingBox(),Vec3.atCenterOf(target)),"Teleported entity remains inside destination sphere");
            pig.discard();level.removeBlock(source,false);level.removeBlock(target,false);h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void caveReliefAndNaturalPoints(GameTestHelper h){
        var floors=new java.util.HashSet<Integer>();var roofs=new java.util.HashSet<Integer>();int upper=0,lower=0;
        for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++){
            floors.add(FoldedTerrain.floor(x*32,z*32));roofs.add(FoldedTerrain.roof(x*32,z*32));
            var point=FoldedTerrain.point(x,z);if(point==null)continue;
            if(point.getY()>65)upper++;else lower++;
            h.assertTrue(com.mcmagic.omnira.spacetime.CorridorLayout.block(point.getX(),point.getY(),point.getZ()).is(ModBlocks.SPACE_WARP_POINT.get()),"Natural point overwritten by terrain");
        }
        h.assertTrue(floors.size()>8&&roofs.size()>6&&upper>0&&lower>0,"Cave remains flat or lacks upper/lower points");h.succeed();
    }
    @GameTest(template="spell_arena") public static void harvestAndBottleRemnant(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,3,4));var target=p.east(3);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        level.setBlockAndUpdate(p,ModBlocks.SPACE_WARP_POINT.get().defaultBlockState());
        h.assertTrue(com.mcmagic.omnira.spacetime.TimeWarpPointBlock.harvest(level,p,player),"Harvest failed");
        h.assertTrue(level.getBlockState(p).is(ModBlocks.SPATIAL_REMNANT.get()),"Wrong remnant");
        ((SpatialRemnantBlockEntity)level.getBlockEntity(p)).restoreDistance(37.5);
        h.assertTrue(!(ModItems.SPACE_WARP_POINT.get() instanceof BlockItem),"Warp item is directly placeable");
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,bottle);
        var hit=new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false);
        h.assertTrue(bottle.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit)).consumesAction(),"Bottle capture failed");
        h.assertTrue(level.getBlockState(p).isAir()&&com.mcmagic.omnira.item.bottle.PocketBottleItem.release(level,bottle,Vec3.atCenterOf(target))&&level.getBlockState(target).is(ModBlocks.SPATIAL_REMNANT.get()),"Bottle release failed");
        h.assertTrue(((SpatialRemnantBlockEntity)level.getBlockEntity(target)).distance()==37.5,"Bottle lost movement progress");
        h.assertTrue(!level.getBlockTicks().hasScheduledTick(target,ModBlocks.SPATIAL_REMNANT.get()),"Movement regeneration must not use a timer");h.succeed();
    }
    @GameTest(template="spell_arena") public static void remnantRecipeAndSphereContact(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.CONDENSATION_TYPE.get());
        h.assertTrue(recipes.stream().anyMatch(r->r.value().getResultItem(h.getLevel().registryAccess()).is(ModItems.SPATIAL_REMNANT.get())),"Remnant condensation missing");
        h.assertTrue(SpaceWarpBlockEntity.touches(new AABB(4.9,0,0,6,1,1),Vec3.ZERO),"Entity edge contact ignored");
        h.assertTrue(!SpaceWarpBlockEntity.touches(new AABB(5.1,0,0,6,1,1),Vec3.ZERO),"Sphere includes outside entity");h.succeed();
    }
    @GameTest(template="spell_arena") public static void safeLandingOutsideUpperPoint(GameTestHelper h){
        var level=h.getLevel();var target=h.absolutePos(new BlockPos(8,12,8));
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)h.setBlock(new BlockPos(x,10,z),Blocks.STONE);
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(8,3,8));pig.setNoAi(true);
        var landing=SpaceWarpBlockEntity.landing(level,pig,target);
        h.assertTrue(landing!=null&&landing.y==target.getY()-1,"Upper platform had no safe landing");
        h.assertTrue(!SpaceWarpBlockEntity.touches(pig.getBoundingBox().move(landing.subtract(pig.position())),Vec3.atCenterOf(target)),"Arrival still intersects target sphere");
        pig.discard();h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void continuousThreeSecondCharge(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(5,3,5));h.getLevel().setBlockAndUpdate(p,ModBlocks.SPACE_WARP_POINT.get().defaultBlockState());
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(5,3,6));pig.setNoAi(true);pig.setNoGravity(true);
        h.runAfterDelay(10,()->{
            h.assertTrue(pig.getPersistentData().contains("OmniraFoldStart"),"Point did not start charging");
            pig.moveTo(p.getX()+10,p.getY(),p.getZ());
        });
        h.runAfterDelay(15,()->pig.moveTo(p.getX()+.5,p.getY(),p.getZ()+1));
        h.runAfterDelay(20,()->{
            h.assertTrue(h.getLevel().getGameTime()-pig.getPersistentData().getLong("OmniraFoldStart")<10,"Leaving did not reset charge");
            pig.discard();h.getLevel().removeBlock(p,false);h.succeed();
        });
    }
}
