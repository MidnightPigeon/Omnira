package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.alchemy.AlchemyMaterials;
import com.mcmagic.omnira.paradox.ParadoxContent;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spacetime.*;
import com.mcmagic.omnira.recipe.AssemblyRecipe;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_paradox_sources")
@PrefixGameTestTemplate(false)
public final class ParadoxSourceGameTests {
    private static CatalysisBlockEntity chamber(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(4,2,4));h.assertTrue(CatalysisBlock.place(h.getLevel(),pos,Direction.NORTH),"Chamber placement failed");
        return CatalysisBlock.controller(h.getLevel(),pos);
    }
    private static void ticks(CatalysisBlockEntity chamber,int count){for(int i=0;i<count;i++)chamber.tick();}
    private static void insert(CatalysisBlockEntity chamber,boolean genesis){
        var base=new ItemStack(genesis?ParadoxContent.STABLE_AGGREGATE.get():ParadoxContent.STABLE_SOURCE.get());
        if(!chamber.items.insertItem(0,base,false).isEmpty())throw new AssertionError("Item substrate rejected");
        chamber.items.setStackInSlot(1,new ItemStack(genesis?ParadoxContent.GENESIS.get():ModItems.PARADOX_DUST.get()));
    }
    @GameTest(template="spell_arena") public static void singleFlowOnAnySurfaceSlot(GameTestHelper h){
        var recipe=(AssemblyRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:assembly/unstable_paradox_source")).orElseThrow().value();
        for(int i=0;i<6;i++){
            var input=new SimpleContainer(7);input.setItem(6,new ItemStack(ParadoxContent.ORE.get()));input.setItem(i,new ItemStack(ParadoxContent.BOTTLED_FLOW.get()));
            h.assertTrue(recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Flow restricted to one surface slot");
            h.assertTrue(recipe.assemble(new AssemblyRecipe.Input(input),h.getLevel().registryAccess()).is(ParadoxContent.UNSTABLE_SOURCE.get()),"Wrong product");
            input.setItem((i+1)%6,new ItemStack(ParadoxContent.BOTTLED_FLOW.get()));
            h.assertTrue(!recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Extra material accepted");
            input.setItem(6,new ItemStack(ParadoxContent.BOTTLED_FLOW.get()));h.assertTrue(!recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Ore center lost");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void stableReactionsUseTenBucketsAndAFrame(GameTestHelper h){
        Item[][] pairs={{ParadoxContent.UNSTABLE_SOURCE.get(),ParadoxContent.STABLE_SOURCE.get()},
                {ModItems.UNSTABLE_SPACETIME_AGGREGATE.get(),ParadoxContent.STABLE_AGGREGATE.get()}};
        for(var pair:pairs){
            var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream().map(r->r.value())
                    .filter(r->r.results().stream().anyMatch(s->s.is(pair[1]))).findFirst().orElseThrow();
            h.assertTrue(recipe.items().size()==2&&recipe.items().get(0).count()==1&&recipe.items().get(0).ingredient().test(new ItemStack(pair[0]))
                    &&recipe.items().get(1).count()==1&&recipe.items().get(1).ingredient().test(new ItemStack(ModItems.GRID_FRAME.get())),"Stabilization items wrong");
            h.assertTrue(recipe.fluid().getFluid()==AlchemyMaterials.DILUTE.source.get()
                    &&recipe.fluid().getAmount()==10000&&recipe.requiresPool()&&recipe.support().isEmpty()&&recipe.duration()==2000,"Stabilization liquid or catalyst wrong");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void unstableWearAndThrownItemIdentity(GameTestHelper h){
        var source=new ItemStack(ParadoxContent.UNSTABLE_SOURCE.get());
        h.assertTrue(source.getMaxDamage()==128&&UnstableAggregateItem.isAggregate(source),"Not the aggregate durability family");
        for(int i=0;i<1279;i++)h.assertTrue(!UnstableAggregateItem.wear(source),"Expired before aggregate lifetime");
        h.assertTrue(UnstableAggregateItem.wear(source),"Aggregate lifetime did not expire");
        for(var item:java.util.List.of(ParadoxContent.STABLE_SOURCE.get(),ParadoxContent.STABLE_AGGREGATE.get(),ParadoxContent.GENESIS.get())){
            var stack=new ItemStack(item);h.assertTrue(!(item instanceof BlockItem)&&!stack.isDamageableItem()&&!UnstableAggregateItem.isAggregate(stack),"Stable material inherited unstable behavior");
        }
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(3,3,3)));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ParadoxContent.UNSTABLE_SOURCE.get()));
        ParadoxContent.UNSTABLE_SOURCE.get().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        var thrown=h.getLevel().getEntitiesOfClass(ThrownAggregate.class,player.getBoundingBox().inflate(4));
        h.assertTrue(thrown.stream().anyMatch(e->e.getItem().is(ParadoxContent.UNSTABLE_SOURCE.get()))&&player.getMainHandItem().isEmpty(),"Throw lost its material or consumption");
        thrown.forEach(net.minecraft.world.entity.Entity::discard);h.succeed();
    }
    @GameTest(template="spell_arena") public static void dustGrowsFourAndRetainsInputs(GameTestHelper h){
        var be=chamber(h);insert(be,false);be.tank.fill(new FluidStack(AlchemyMaterials.DILUTE.source.get(),4000),FluidAction.EXECUTE);
        ticks(be,799);h.assertTrue(be.items.getStackInSlot(2).isEmpty()&&be.stage()==4,"Dust harvested before fourth catalysis");
        ticks(be,1);h.assertTrue(be.items.getStackInSlot(2).is(ModItems.PARADOX_DUST.get())&&be.items.getStackInSlot(2).getCount()==4,"Dust yield is not four");
        h.assertTrue(be.items.getStackInSlot(0).getCount()==1&&be.items.getStackInSlot(1).getCount()==1&&be.stage()==1&&be.tank.getFluidAmount()==3600,"Inputs, reset or consumption incorrect");
        ticks(be,500);h.assertTrue(be.tank.getFluidAmount()==3600,"Occupied output consumed fluid");h.succeed();
    }
    @GameTest(template="spell_arena") public static void genesisNeedsRefinedFluidAndYieldsOne(GameTestHelper h){
        var be=chamber(h);insert(be,true);be.tank.setFluid(new FluidStack(AlchemyMaterials.DILUTE.source.get(),4000));
        ticks(be,800);h.assertTrue(be.plan()==null&&be.items.getStackInSlot(2).isEmpty()&&be.tank.getFluidAmount()==4000,"Genesis grew using dilute spacetime");
        be.tank.setFluid(new FluidStack(AlchemyMaterials.REFINED.source.get(),4000));ticks(be,800);
        h.assertTrue(be.items.getStackInSlot(2).is(ParadoxContent.GENESIS.get())&&be.items.getStackInSlot(2).getCount()==1&&be.tank.getFluidAmount()==3600,"Genesis yield or fluid cost incorrect");
        h.assertTrue(be.items.getStackInSlot(0).is(ParadoxContent.STABLE_AGGREGATE.get())&&be.items.getStackInSlot(1).is(ParadoxContent.GENESIS.get()),"Consumed genesis inputs");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bothSourcesPauseWithoutFluidAcrossReload(GameTestHelper h){
        var be=chamber(h);
        for(boolean genesis:new boolean[]{false,true}){
            be.items.setStackInSlot(0,ItemStack.EMPTY);be.items.setStackInSlot(2,ItemStack.EMPTY);insert(be,genesis);
            var liquid=genesis?AlchemyMaterials.REFINED.source.get():AlchemyMaterials.DILUTE.source.get();
            be.tank.setFluid(new FluidStack(liquid,100));ticks(be,200);
            h.assertTrue(be.stage()==2&&be.tank.isEmpty()&&be.naturalRule()==null,"Source invented natural growth");
            for(int i=0;i<200;i++)be.naturalTick(net.minecraft.util.RandomSource.create(i));ticks(be,2000);
            be.loadWithComponents(be.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            h.assertTrue(be.stage()==2&&be.items.getStackInSlot(2).isEmpty(),"Dry mode or reload changed progress");
            be.tank.fill(new FluidStack(liquid,300),FluidAction.EXECUTE);ticks(be,600);
            h.assertTrue(be.items.getStackInSlot(2).getCount()==(genesis?1:4)&&be.stage()==1,"Refill did not resume paused cycle");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void substrateAcceptanceUsesRecipeData(GameTestHelper h){
        var be=chamber(h);
        h.assertTrue(be.items.isItemValid(0,new ItemStack(ParadoxContent.STABLE_SOURCE.get()))&&be.items.isItemValid(0,new ItemStack(ParadoxContent.STABLE_AGGREGATE.get())),"Item substrates rejected");
        h.assertTrue(!be.items.isItemValid(0,new ItemStack(Items.APPLE))&&!be.items.isItemValid(0,new ItemStack(ParadoxContent.UNSTABLE_SOURCE.get())),"Unspecified substrate accepted");
        h.assertTrue(be.items.isItemValid(0,new ItemStack(Items.DIRT)),"Existing block substrate rejected");h.succeed();
    }
    @GameTest(template="spell_arena") public static void genesisLoreIsPurpleAndItalic(GameTestHelper h){
        var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
        ParadoxContent.GENESIS.get().appendHoverText(new ItemStack(ParadoxContent.GENESIS.get()),Item.TooltipContext.of(h.getLevel()),lines,TooltipFlag.NORMAL);
        h.assertTrue(lines.size()==1&&lines.getFirst().getStyle().isItalic()
                &&net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.DARK_PURPLE).equals(lines.getFirst().getStyle().getColor()),"Genesis lore style incorrect");
        h.succeed();
    }
    private ParadoxSourceGameTests(){}
}
