package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.time.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_silt_revision")
@PrefixGameTestTemplate(false)
public final class TemporalSiltGameTests {
    private static void biome(GameTestHelper h,BlockPos pos,ResourceKey<Biome> key){
        var level=h.getLevel();var holder=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
        var chunk=level.getChunkAt(pos);chunk.fillBiomesFromNoise((x,y,z,s)->holder,level.getChunkSource().randomState().sampler());chunk.setUnsaved(true);
    }
    @GameTest(template="spell_arena")
    public static void sedimentTimers(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));
        var biomes=java.util.List.of(ResourceKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace("plains")),FleetingTime.BIOME,TemporalSiltBlock.FROZEN_TERRA);
        for(var key:biomes){
            pos=pos.above();
            biome(h,pos,key);
            for(boolean living:new boolean[]{false,true}){
                var block=(living?DreamContent.LIVING_TEMPORAL_SILT:DreamContent.TEMPORAL_SILT).get();
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,block.defaultBlockState());
                boolean frozen=key.equals(TemporalSiltBlock.FROZEN_TERRA);
                var target=pos;
                var ticks=(net.minecraft.world.ticks.LevelChunkTicks<Block>)level.getChunkAt(pos).getBlockTicks();
                var scheduled=ticks.getAll().filter(t->t.pos().equals(target)&&t.type()==block).findFirst();
                h.assertTrue(frozen?scheduled.isEmpty():scheduled.isPresent(),"Wrong protected-biome scheduling");
                if(!frozen)h.assertTrue(scheduled.orElseThrow().triggerTick()-level.getGameTime()==(key.equals(FleetingTime.BIOME)?3000:6000),"Wrong regional tick multiplier");
                if(!frozen)h.assertTrue(level.getBlockTicks().hasScheduledTick(pos,block),"Placement did not schedule sedimentation");
                block.defaultBlockState().tick(level,pos,level.random);
                h.assertTrue(level.getBlockState(pos).is(frozen?block:(living?TimeNatureContent.LIVING_SILT:TimeNatureContent.SILT).get()),"Wrong sediment conversion/protected biome");
            }
        }
        for(var b:new Block[]{TimeNatureContent.SILT.get(),TimeNatureContent.LIVING_SILT.get()})h.assertTrue(b instanceof com.mcmagic.omnira.mire.DecayingTemporalSiltBlock,"Sedimented soil missing contact-only decay");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void harvestAndReplant(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getCenter().add(0,1,2));
        for(var soil:new Block[]{DreamContent.TEMPORAL_SILT.get(),DreamContent.LIVING_TEMPORAL_SILT.get(),TimeNatureContent.SILT.get(),TimeNatureContent.LIVING_SILT.get()})
            for(var plant:new Block[]{TimeNatureContent.GRASS.get(),TimeNatureContent.FLOWER.get()}){
                level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,soil.defaultBlockState());
                var stack=new ItemStack(plant.asItem());player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter().add(0,.5,0),Direction.UP,pos,false)));
                h.assertTrue(level.getBlockState(pos.above()).is(plant),"Actual BlockItem placement failed on "+soil);
            }
        var silk=new ItemStack(Items.DIAMOND_HOE);silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        for(var tool:new ItemStack[]{new ItemStack(Items.SHEARS),silk}){
            var drops=Block.getDrops(TimeNatureContent.GRASS.get().defaultBlockState(),level,pos.above(),null,player,tool);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(TimeNatureContent.GRASS.get().asItem()),"Shears/Silk Touch did not preserve grass");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void solidificationRecipes(GameTestHelper h){
        var level=h.getLevel();var stacks=new java.util.ArrayList<ItemStack>();
        for(int i=0;i<9;i++)stacks.add(new ItemStack(i==4?TimeNatureContent.LIVING_SILT.get():DreamContent.LIGHT_CRYSTAL_CORE.get()));
        var input=CraftingInput.of(3,3,stacks);
        var recipe=(CraftingRecipe)level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira","solidified_light_crystal_core")).orElseThrow().value();
        h.assertTrue(recipe.matches(input,level),"Eight-core recipe rejected");var output=recipe.assemble(input,level.registryAccess());
        h.assertTrue(output.is(DreamContent.SOLIDIFIED_LIGHT_CRYSTAL_CORE.get().asItem())&&output.getCount()==8,"Wrong solidification result");
        for(var suffix:new String[]{"stairs","slab"}){
            var id=ResourceLocation.fromNamespaceAndPath("omnira","solidified_light_crystal_core_"+suffix+"_stonecutting");
            var cutting=(StonecutterRecipe)level.getRecipeManager().byKey(id).orElseThrow().value();
            var single=new SingleRecipeInput(new ItemStack(DreamContent.SOLIDIFIED_LIGHT_CRYSTAL_CORE.get()));
            h.assertTrue(cutting.matches(single,level),"Stonecutting input rejected");
            h.assertTrue(cutting.assemble(single,level.registryAccess()).getCount()==(suffix.equals("stairs")?1:2),"Wrong cutting yield");
        }
        h.succeed();
    }
}
