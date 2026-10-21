package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_alchemy")
@PrefixGameTestTemplate(false)
public final class ReactionCatalystFailureGameTests {
    private static ReactionRecipe recipe(Item input,Item output,Ingredient support){
        return new ReactionRecipe(List.of(new ReactionRecipe.InputItem(Ingredient.of(input),1)),
                new FluidStack(Fluids.WATER,1000),support,List.of(new ItemStack(output)),FluidStack.EMPTY,2000);
    }
    private static void tick(GameTestHelper h,RoughAlchemyCauldronBlockEntity vessel,int count){
        for(int i=0;i<count;i++)RoughAlchemyCauldronBlockEntity.tick(h.getLevel(),vessel.getBlockPos(),vessel.getBlockState(),vessel);
    }
    private static List<SpellEntity> blasts(GameTestHelper h,BlockPos pos){
        return h.getLevel().getEntitiesOfClass(SpellEntity.class,new AABB(pos).inflate(1));
    }
    private static void clearBlasts(GameTestHelper h,BlockPos pos){for(var blast:blasts(h,pos))blast.discard();}

    @GameTest(template="spell_arena") public static void cauldronCatalystLossCancelsBeforeDuringAndAfterProcessing(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        level.setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        var reaction=recipe(Items.DIAMOND,Items.EMERALD,Ingredient.of(Blocks.BRICKS));
        for(int elapsed:new int[]{0,200,2000}){
            clearBlasts(h,pos);level.setBlockAndUpdate(pos.below(),Blocks.BRICKS.defaultBlockState());
            var vessel=(RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);
            for(int i=0;i<4;i++)vessel.items.setStackInSlot(i,ItemStack.EMPTY);
            vessel.items.setStackInSlot(0,new ItemStack(Items.DIAMOND));vessel.tank.setFluid(reaction.fluid().copy());
            h.assertTrue(vessel.start(reaction),"Catalytic cauldron did not start");
            for(var item:List.of(Items.STONE,Items.DIRT,Items.SAND,Items.GRAVEL))
                h.assertTrue(vessel.insert(new ItemStack(item)).isEmpty(),"Working cauldron locked a material slot");
            tick(h,vessel,elapsed);
            int remaining=vessel.tank.getFluidAmount();
            var loaded=new RoughAlchemyCauldronBlockEntity(pos,vessel.getBlockState());
            loaded.loadWithComponents(vessel.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
            level.setBlockEntity(loaded);
            level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());tick(h,loaded,1);
            h.assertTrue(!loaded.working()&&loaded.progress()==0,"Missing catalyst retained a saved reaction");
            h.assertTrue(loaded.tank.getFluidAmount()==remaining,"Failure refunded or consumed stored liquid");
            h.assertTrue(java.util.stream.IntStream.range(0,4).allMatch(i->loaded.items.getStackInSlot(i).getCount()==1),"Failure changed unrelated stored materials");
            h.assertTrue(blasts(h,pos).size()==1&&blasts(h,pos).getFirst().burstRadius()==10,"Wrong primitive blast count or radius");
            tick(h,loaded,2);h.assertTrue(blasts(h,pos).size()==1,"Cancelled reaction exploded repeatedly");
            level.setBlockAndUpdate(pos.below(),Blocks.BRICKS.defaultBlockState());tick(h,loaded,1);
            h.assertTrue(!loaded.working(),"Restoring catalyst resumed cancelled work");
        }
        clearBlasts(h,pos);h.succeed();
    }

    @GameTest(template="spell_arena") public static void poolChecksCatalystWhileUnpoweredAndKeepsIndependentJobs(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(12,3,12));
        h.assertTrue(ReactionPoolBlock.place(level,pos,Direction.NORTH),"Pool placement failed");
        var pool=ReactionPoolBlock.controller(level,pos);
        var support=Ingredient.of(Blocks.BRICKS);var first=recipe(Items.DIAMOND,Items.EMERALD,support);
        var second=recipe(Items.GOLD_INGOT,Items.QUARTZ,support);
        var independent=new ReactionRecipe(List.of(new ReactionRecipe.InputItem(Ingredient.of(Items.STICK),1)),
                FluidStack.EMPTY,Ingredient.EMPTY,List.of(new ItemStack(Items.FLINT)),FluidStack.EMPTY,2000);
        var corner=pos.offset(1,-1,1);var other=pos.offset(-1,-1,-1);
        level.setBlockAndUpdate(corner,Blocks.BRICKS.defaultBlockState());level.setBlockAndUpdate(other,Blocks.BRICKS.defaultBlockState());
        pool.manualFluid.fill(first.fluid(),IFluidHandler.FluidAction.EXECUTE);
        pool.items.setStackInSlot(0,new ItemStack(Items.DIAMOND));pool.items.setStackInSlot(1,new ItemStack(Items.GOLD_INGOT));
        pool.items.setStackInSlot(2,new ItemStack(Items.STICK));
        h.assertTrue(pool.start(first)&&pool.start(second)&&pool.start(independent),"Parallel reactions did not start");
        pool.advance(200);h.assertTrue(pool.fluids.total()==800,"Wrong parallel fluid debit");
        level.setBlockAndUpdate(corner,Blocks.AIR.defaultBlockState());
        ReactionPoolBlockEntity.tick(level,pos,pool.getBlockState(),pool);
        h.assertTrue(pool.reactionCount()==3&&blasts(h,pos).isEmpty(),"Alternative catalyst was ignored");
        var loaded=new ReactionPoolBlockEntity(pos,pool.getBlockState());
        loaded.loadWithComponents(pool.saveWithFullMetadata(level.registryAccess()),level.registryAccess());level.setBlockEntity(loaded);
        level.setBlockAndUpdate(other,Blocks.AIR.defaultBlockState());
        ReactionPoolBlockEntity.tick(level,pos,loaded.getBlockState(),loaded);
        h.assertTrue(!loaded.powered()&&loaded.reactionCount()==1,"Unpowered pool retained invalid jobs or cancelled independent work");
        h.assertTrue(loaded.fluids.total()==800,"Pool failure refunded consumed liquid");
        h.assertTrue(blasts(h,pos).size()==1&&blasts(h,pos).getFirst().burstRadius()==20,"Simultaneous failures must produce one large blast");
        ReactionPoolBlockEntity.tick(level,pos,loaded.getBlockState(),loaded);
        h.assertTrue(blasts(h,pos).size()==1,"Pool failure repeated");
        loaded.advance(1800);h.assertTrue(!loaded.reacting()&&loaded.items.getStackInSlot(0).is(Items.FLINT),"Independent reaction failed to finish");
        clearBlasts(h,pos);h.succeed();
    }

    @GameTest(template="spell_arena") public static void catalystAccidentsRetainBothExistingDamageScales(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        for(boolean large:new boolean[]{false,true}){
            var pig=EntityType.PIG.create(level);h.assertTrue(pig!=null,"Pig creation failed");
            pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);pig.setHealth(100);
            pig.setPos(pos.getX()+1.5,pos.getY()+.5,pos.getZ()+.5);level.addFreshEntity(pig);
            ReactionFailure.blast(level,pos,large);
            h.assertTrue(pig.getHealth()==(large?70:85)&&pig.getDeltaMovement().lengthSqr()>0,"Accident damage or knockback changed");
            h.assertTrue(level.getBlockState(pos.below()).is(Blocks.STONE),"Accident changed terrain");
            pig.discard();clearBlasts(h,pos);
        }
        h.succeed();
    }
    private ReactionCatalystFailureGameTests(){}
}
