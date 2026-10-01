package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.neoforge.gametest.*;
import java.util.ArrayList;

@GameTestHolder("omnira_decoration_stairs")
@PrefixGameTestTemplate(false)
public final class DecorationStairGameTests {
    private static BlockState stair(Block block,Direction facing,Half half) {
        return block.defaultBlockState().setValue(StairBlock.FACING,facing).setValue(StairBlock.HALF,half).setValue(StairBlock.SHAPE,StairsShape.STRAIGHT);
    }
    @GameTest(template="spell_arena",timeoutTicks=400) public static void everyFamilyConnectsToOtherMaterials(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));
        var others=new ArrayList<Block>();DecorationVariants.families().forEach(f->others.add(f.stairs().get()));
        others.add(Blocks.OAK_STAIRS);others.add(Blocks.STONE_STAIRS);others.add(Blocks.QUARTZ_STAIRS);
        int checks=0;
        for(var family:DecorationVariants.families())for(var other:others)for(var half:Half.values())for(var facing:Direction.Plane.HORIZONTAL) {
            for(var d:Direction.Plane.HORIZONTAL)level.setBlock(pos.relative(d),Blocks.AIR.defaultBlockState(),2);
            var current=stair(family.stairs().get(),facing,half);
            level.setBlock(pos,current,2);
            for(boolean left:new boolean[]{true,false})for(boolean outer:new boolean[]{true,false}) {
                var neighbor=pos.relative(outer?facing:facing.getOpposite());
                var direction=left?facing.getCounterClockWise():facing.getClockWise();
                level.setBlock(neighbor,stair(other,direction,half),2);
                var connected=Block.updateFromNeighbourShapes(current,level,pos);
                var expected=outer?(left?StairsShape.OUTER_LEFT:StairsShape.OUTER_RIGHT):(left?StairsShape.INNER_LEFT:StairsShape.INNER_RIGHT);
                h.assertTrue(connected.getValue(StairBlock.SHAPE)==expected,"Mixed corner failed: "+family.stem()+" / "+other+" "+half+" "+facing+" "+expected);
                level.setBlock(neighbor,stair(other,direction,half==Half.TOP?Half.BOTTOM:Half.TOP),2);
                h.assertTrue(Block.updateFromNeighbourShapes(current,level,pos).getValue(StairBlock.SHAPE)==StairsShape.STRAIGHT,"Opposite halves should not force a turn");
                level.setBlock(neighbor,Blocks.AIR.defaultBlockState(),2);checks++;
            }
            level.setBlock(pos.relative(facing),stair(other,facing,half),2);
            h.assertTrue(Block.updateFromNeighbourShapes(current,level,pos).getValue(StairBlock.SHAPE)==StairsShape.STRAIGHT,"Parallel stairs should stay straight");
        }
        h.assertTrue(checks==DecorationVariants.families().size()*others.size()*2*4*4,"Incomplete mixed-stair matrix");h.succeed();
    }
    @GameTest(template="spell_arena") public static void batchPlacementReconcilesAndRemovalRestoresStraight(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));
        level.setBlock(pos,stair(DreamContent.SPIRITUAL_CRYSTAL_STAIRS.get(),Direction.NORTH,Half.BOTTOM),2);
        level.setBlock(pos.north(),stair(Blocks.QUARTZ_STAIRS,Direction.WEST,Half.BOTTOM),2);
        h.runAfterDelay(3,()->{
            h.assertTrue(level.getBlockState(pos).getValue(StairBlock.SHAPE)==StairsShape.OUTER_LEFT,"Batch placement left a stale straight state");
            level.setBlockAndUpdate(pos.north(),Blocks.AIR.defaultBlockState());
            h.assertTrue(level.getBlockState(pos).getValue(StairBlock.SHAPE)==StairsShape.STRAIGHT,"Removing neighbor did not restore straight stair");
            level.setBlockAndUpdate(pos.south(),stair(DreamContent.ENGRAVED_MIRROR_ROCK_STAIRS.get(),Direction.EAST,Half.BOTTOM));
            h.assertTrue(level.getBlockState(pos).getValue(StairBlock.SHAPE)==StairsShape.INNER_RIGHT,"Replacement with a different material did not form an inner corner");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void reversePlacementAndBlockingMatchVanilla(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));
        level.setBlockAndUpdate(pos.west(),stair(DreamContent.ENGRAVED_MIRROR_ROCK_STAIRS.get(),Direction.SOUTH,Half.TOP));
        level.setBlockAndUpdate(pos,stair(DreamContent.SPIRITUAL_CRYSTAL_STAIRS.get(),Direction.WEST,Half.TOP));
        h.runAfterDelay(3,()->{
            h.assertTrue(level.getBlockState(pos).getValue(StairBlock.SHAPE)==StairsShape.OUTER_LEFT,"Reverse placement order failed");
            level.setBlockAndUpdate(pos.north(),stair(Blocks.OAK_STAIRS,Direction.WEST,Half.TOP));
            h.assertTrue(level.getBlockState(pos).getValue(StairBlock.SHAPE)==StairsShape.STRAIGHT,"Vanilla side guard must prevent a false corner");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void variantRecipesAndLightMatchBase(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        for(var family:DecorationVariants.families())for(var block:new Block[]{family.stairs().get(),family.slab().get()}) {
            var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            h.assertTrue(manager.byKey(id).isPresent(),"Missing variant crafting: "+id);
            h.assertTrue(block.defaultBlockState().getLightEmission()==family.base().get().defaultBlockState().getLightEmission(),"Variant lost base luminosity: "+id);
            var tag=block instanceof StairBlock?net.minecraft.tags.BlockTags.STAIRS:net.minecraft.tags.BlockTags.SLABS;
            h.assertTrue(block.defaultBlockState().is(tag),"Variant missing vanilla shape tag: "+id);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void sableMassAndFractionalBuoyancy(GameTestHelper h) {
        if(!net.neoforged.fml.ModList.get().isLoaded("sable")){h.succeed();return;}
        var pos=h.absolutePos(new BlockPos(4,3,4));
        for(var family:DecorationVariants.families()) {
            var base=family.base().get().defaultBlockState();var stair=family.stairs().get().defaultBlockState();
            var slab=family.slab().get().defaultBlockState();var doubled=slab.setValue(SlabBlock.TYPE,SlabType.DOUBLE);
            double mass=dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getMass(h.getLevel(),pos,base);
            for(var state:new BlockState[]{stair,slab,doubled}) {
                double expected=state==doubled?mass:mass*.5;
                double actual=dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getMass(h.getLevel(),pos,state);
                h.assertTrue(Math.abs(actual-expected)<1e-8,"Mass ratio incorrect: "+family.stem()+" "+state+" base="+mass+" actual="+actual);
            }
            h.assertTrue(dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getVolume(slab)==.5,"Slab volume not automatically halved");
            h.assertTrue(dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getVolume(stair)==.5,"Stair volume must follow Sable's native rule");
            if(family.base()==DreamContent.MIRROR_ROCK || family.base()==DreamContent.ENGRAVED_MIRROR_ROCK)
                h.assertTrue(mass==0,"Mirror materials must be weightless");
            h.assertTrue(dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getVolume(doubled)==1,"Double slab volume not restored");
            var material=dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getFloatingMaterial(base);
            if(material!=null) {
                h.assertTrue(dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getFloatingScale(slab)==.5,"Single slab floating multiplier incorrect");
                h.assertTrue(dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getFloatingScale(doubled)==1,"Double slab floating multiplier incorrect");
                h.assertTrue(dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getFloatingScale(stair)==1,"Stair floating multiplier incorrect");
                h.assertTrue(material.equals(dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getFloatingMaterial(slab)),"Slab changed floating material");
                if(family.base()==DreamContent.LIGHT_SOURCE_CRYSTAL)h.assertTrue(material.liftStrength()==5,"Fractional buoyancy must not nerf the source crystal");
            }
        }
        h.succeed();
    }
}
