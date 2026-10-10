package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.IItemHandler;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

@GameTestHolder("omnira_alchemy")
@PrefixGameTestTemplate(false)
public final class AlchemyRefinementGameTests {
    private static ReactionRecipe recipe(GameTestHelper h,String id){return h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.REACTION_TYPE.get()).stream()
            .filter(r->r.id().equals(ResourceLocation.parse("omnira:reaction/"+id))).findFirst().orElseThrow().value();}
    private static ReactionPoolBlockEntity pool(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));
        h.assertTrue(ReactionPoolBlock.place(h.getLevel(),pos,Direction.NORTH),"Pool placement failed");
        return ReactionPoolBlock.controller(h.getLevel(),pos);
    }
    private static RoughAlchemyCauldronBlockEntity cauldron(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));h.getLevel().setBlockAndUpdate(pos,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
        return (RoughAlchemyCauldronBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void tick(GameTestHelper h,RoughAlchemyCauldronBlockEntity v,int ticks){
        for(int i=0;i<ticks;i++)RoughAlchemyCauldronBlockEntity.tick(h.getLevel(),v.getBlockPos(),v.getBlockState(),v);
    }
    private static int count(IItemHandler inventory,Item item){
        int count=0;for(int i=0;i<inventory.getSlots();i++){var s=inventory.getStackInSlot(i);if(s.is(item))count+=s.getCount();}return count;
    }
    private static void pair(GameTestHelper h,IItemHandler inventory){
        h.assertTrue(count(inventory,ModItems.TIME_MICROCORE.get())==1&&count(inventory,ModItems.SPACE_MICROCORE.get())==1,"Fixed pair was not delivered together exactly once");
    }
    @GameTest(template="spell_arena") public static void refineSpacetimeBatchesAndCatalyst(GameTestHelper h){
        var r=recipe(h,"refined_spacetime");var p=pool(h);
        p.items.setStackInSlot(0,new ItemStack(ModItems.TIME_WARP_POINT.get()));p.items.setStackInSlot(1,new ItemStack(ModItems.SPACE_WARP_POINT.get()));
        p.manualFluid.fill(r.fluid(),EXECUTE);
        h.assertTrue(r.fluid().getAmount()==10000&&r.fluidResult().getAmount()==1000&&r.duration()==2000&&r.requiresPool(),"Refinement contract incorrect");
        h.assertTrue(!p.start(r)&&!p.items.getStackInSlot(0).isEmpty(),"Missing catalyst consumed inputs");
        var catalyst=p.getBlockPos().offset(1,-1,1);h.getLevel().setBlockAndUpdate(catalyst,ModBlocks.SPACETIME_SPELL_CORE.get().defaultBlockState());
        h.assertTrue(p.start(r)&&p.items.getStackInSlot(0).isEmpty()&&p.items.getStackInSlot(1).isEmpty(),"Refinement failed to start and consume both points");
        p.advance(199);h.assertTrue(p.fluids.amount(r.fluid())==10000&&p.fluids.amount(r.fluidResult())==0,"Early refinement debit");
        p.advance(1);h.assertTrue(p.fluids.amount(r.fluid())==9000&&p.fluids.amount(r.fluidResult())==100,"Wrong refinement installment");
        var copy=new ReactionPoolBlockEntity(p.getBlockPos(),p.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(p.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());copy.advance(1800);
        h.assertTrue(!copy.reacting()&&copy.fluids.total()==1000&&copy.fluids.amount(r.fluidResult())==1000,"Refinement reload lost fluid accounting");
        h.assertTrue(h.getLevel().getBlockState(catalyst).is(ModBlocks.SPACETIME_SPELL_CORE.get()),"Catalyst consumed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void enhancedCoreInPrimitiveVessel(GameTestHelper h){
        var r=recipe(h,"enhanced_alchemical_energy_core");var v=cauldron(h);
        v.items.setStackInSlot(0,new ItemStack(ModItems.ROUGH_ALCHEMICAL_ENERGY_CORE.get()));v.tank.setFluid(r.fluid().copy());
        h.assertTrue(r.support().isEmpty()&&r.fluid().getAmount()==4000&&!r.requiresPool()&&v.start(r),"Enhanced core recipe invalid");
        h.assertTrue(v.items.getStackInSlot(0).isEmpty(),"Rough core not consumed immediately");
        tick(h,v,200);h.assertTrue(v.tank.getFluidAmount()==3600&&count(v.items,AlchemyMaterials.ENHANCED_CORE_ITEM.get())==0,"Wrong core installment");
        tick(h,v,1800);h.assertTrue(v.tank.isEmpty()&&count(v.items,AlchemyMaterials.ENHANCED_CORE_ITEM.get())==1,"Enhanced core not produced");
        var pos=v.getBlockPos().offset(3,0,0);h.getLevel().setBlockAndUpdate(pos,AlchemyMaterials.ENHANCED_CORE.get().defaultBlockState());
        h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof EnhancedAlchemicalEnergyCoreBlock.Core,"Enhanced core has no animated block entity");
        h.assertTrue(!v.start(recipe(h,"refined_spacetime")),"Primitive cauldron accepted refining recipe");h.succeed();
    }
    @GameTest(template="spell_arena") public static void enhancedCoreInPool(GameTestHelper h){
        var r=recipe(h,"enhanced_alchemical_energy_core");var p=pool(h);
        p.items.setStackInSlot(0,new ItemStack(ModItems.ROUGH_ALCHEMICAL_ENERGY_CORE.get()));p.manualFluid.fill(r.fluid(),EXECUTE);
        h.assertTrue(p.start(r),"Pool rejected enhanced core recipe");p.advance(2000);
        h.assertTrue(count(p.items,AlchemyMaterials.ENHANCED_CORE_ITEM.get())==1&&p.fluids.total()==0,"Pool did not produce enhanced core");h.succeed();
    }
    @GameTest(template="spell_arena") public static void guaranteedPairPrimitiveReload(GameTestHelper h){
        var r=recipe(h,"time_space_microcores");var v=cauldron(h);v.tank.setFluid(r.fluid().copy());
        h.assertTrue(r.results().isEmpty()&&r.guaranteedResults().size()==2&&!v.start(r),"Pair must be fixed and require catalyst");
        h.getLevel().setBlockAndUpdate(v.getBlockPos().below(),AlchemyMaterials.ENHANCED_CORE.get().defaultBlockState());
        h.assertTrue(v.start(r),"Primitive pair did not start");tick(h,v,200);
        h.assertTrue(v.tank.getFluidAmount()==900&&count(v.items,ModItems.TIME_MICROCORE.get())==0,"Pair delivered early");
        var copy=new RoughAlchemyCauldronBlockEntity(v.getBlockPos(),v.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(v.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());tick(h,copy,1800);
        pair(h,copy.items);h.assertTrue(!copy.working()&&copy.tank.isEmpty(),"Primitive pair did not finish");tick(h,copy,2000);pair(h,copy.items);h.succeed();
    }
    @GameTest(template="spell_arena") public static void guaranteedPairPoolReload(GameTestHelper h){
        var r=recipe(h,"time_space_microcores");var p=pool(h);p.manualFluid.fill(r.fluid(),EXECUTE);
        h.getLevel().setBlockAndUpdate(p.getBlockPos().below(),AlchemyMaterials.ENHANCED_CORE.get().defaultBlockState());
        h.assertTrue(p.start(r),"Pool pair did not start");p.advance(400);
        var copy=new ReactionPoolBlockEntity(p.getBlockPos(),p.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(p.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());copy.advance(1600);
        pair(h,copy.items);h.assertTrue(!copy.reacting()&&copy.fluids.total()==0,"Pool pair did not finish");copy.advance(2000);pair(h,copy.items);h.succeed();
    }
    @GameTest(template="spell_arena") public static void pairNeedsTwoSlotsAndReservesBoth(GameTestHelper h){
        var r=recipe(h,"time_space_microcores");var v=cauldron(h);v.tank.setFluid(r.fluid().copy());
        h.getLevel().setBlockAndUpdate(v.getBlockPos().below(),AlchemyMaterials.ENHANCED_CORE.get().defaultBlockState());
        v.items.setStackInSlot(0,new ItemStack(Items.STONE));v.items.setStackInSlot(1,new ItemStack(Items.DIRT));v.items.setStackInSlot(2,new ItemStack(Items.STICK));
        h.assertTrue(!v.start(r),"Two outputs reused one free slot");v.items.setStackInSlot(2,ItemStack.EMPTY);
        h.assertTrue(v.start(r),"Two free slots rejected");
        h.assertTrue(!v.items.insertItem(2,new ItemStack(Items.DIAMOND),false).isEmpty(),"Manual insert stole reserved pair slot");
        tick(h,v,2000);pair(h,v.items);h.succeed();
    }
    @GameTest(template="spell_arena") public static void fullPairMemberBlocksBothVessels(GameTestHelper h){
        var r=recipe(h,"time_space_microcores");var p=pool(h);p.manualFluid.fill(r.fluid(),EXECUTE);
        h.getLevel().setBlockAndUpdate(p.getBlockPos().below(),AlchemyMaterials.ENHANCED_CORE.get().defaultBlockState());
        for(var item:java.util.List.of(ModItems.TIME_MICROCORE.get(),ModItems.SPACE_MICROCORE.get())){
            p.items.setStackInSlot(0,new ItemStack(item,item.getDefaultMaxStackSize()));
            h.assertTrue(!p.start(r),"Full paired output allowed pool startup");
        }
        h.getLevel().setBlockAndUpdate(p.getBlockPos(),Blocks.AIR.defaultBlockState());
        var v=cauldron(h);v.tank.setFluid(r.fluid().copy());
        h.getLevel().setBlockAndUpdate(v.getBlockPos().below(),AlchemyMaterials.ENHANCED_CORE.get().defaultBlockState());
        for(var item:java.util.List.of(ModItems.TIME_MICROCORE.get(),ModItems.SPACE_MICROCORE.get())){
            v.items.setStackInSlot(0,new ItemStack(item,item.getDefaultMaxStackSize()));
            h.assertTrue(!v.start(r),"Full paired output allowed primitive startup");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void failedPairDeliversNeitherProduct(GameTestHelper h){
        var r=recipe(h,"time_space_microcores");var p=pool(h);p.manualFluid.fill(r.fluid(),EXECUTE);
        h.getLevel().setBlockAndUpdate(p.getBlockPos().below(),AlchemyMaterials.ENHANCED_CORE.get().defaultBlockState());
        h.assertTrue(p.start(r),"Pair did not start");p.advance(1000);p.manualFluid.drain(r.fluid(),EXECUTE);p.advance(200);
        h.assertTrue(!p.reacting()&&count(p.items,ModItems.TIME_MICROCORE.get())==0&&count(p.items,ModItems.SPACE_MICROCORE.get())==0,"Failed reaction delivered fixed outputs");
        var ops=h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var tag=(net.minecraft.nbt.CompoundTag)ReactionRecipe.CODEC.codec().encodeStart(ops,recipe(h,"dream_water")).getOrThrow();tag.remove("guaranteed_results");
        h.assertTrue(ReactionRecipe.CODEC.codec().parse(ops,tag).getOrThrow().guaranteedResults().isEmpty(),"Legacy recipes gained guaranteed outputs");h.succeed();
    }
    private AlchemyRefinementGameTests(){}
}
