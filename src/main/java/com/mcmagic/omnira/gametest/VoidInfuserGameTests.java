package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.reversal.*;
import com.mcmagic.omnira.space.abyss.AbyssContent;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_void_infusion")
@PrefixGameTestTemplate(false)
public final class VoidInfuserGameTests {
    private static ItemStack stack(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));}
    private static ReversalBlockEntity machine(GameTestHelper h,boolean infusion){
        var pos=h.absolutePos(new BlockPos(3,3,3));
        h.getLevel().setBlockAndUpdate(pos,(infusion?ReversalContent.INFUSER:ReversalContent.MACHINE).get().defaultBlockState());
        return (ReversalBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void ticks(ReversalBlockEntity be,int count){for(int i=0;i<count;i++)be.tick();}
    private static void conversion(GameTestHelper h,ReversalBlockEntity be,String input,String output){
        be.setItem(0,stack(input));be.setItem(1,ItemStack.EMPTY);be.setItem(2,stack("omnira:spatial_crystal_shard"));
        ticks(be,be.duration()-1);h.assertTrue(be.getItem(1).isEmpty(),"Completed early: "+input);
        int charged=be.energy();be.tick();
        h.assertTrue(be.getItem(0).isEmpty()&&be.getItem(1).is(stack(output).getItem())&&be.getItem(1).getCount()==1&&be.energy()==charged-1,"Wrong conversion or energy: "+input);
    }
    @GameTest(template="spell_arena") public static void allConversionsAndBothSaplingRecipes(GameTestHelper h){
        var be=machine(h,true);
        for(String[] pair:new String[][]{{"omnira:mirror_rock","omnira:void_mirror_rock"},{"omnira:light_condensate","omnira:shadow_rock"},{"minecraft:slime_ball","minecraft:ender_pearl"},{"minecraft:obsidian","minecraft:crying_obsidian"}})
            conversion(h,be,pair[0],pair[1]);
        for(String s:new String[]{"temporal_silt","living_temporal_silt","sedimented_temporal_silt","sedimented_living_temporal_silt","rotted_temporal_silt"})
            conversion(h,be,"omnira:"+s,"omnira:rotted_temporal_silt");
        var saplings=BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.SAPLINGS);
        int count=0;
        for(var item:saplings){conversion(h,be,BuiltInRegistries.ITEM.getKey(item.value()).toString(),"omnira:shadow_sapling");count++;}
        h.assertTrue(count>=8,"Missing sapling tag support");
        be=machine(h,false);
        for(var item:saplings)conversion(h,be,BuiltInRegistries.ITEM.getKey(item.value()).toString(),"omnira:time_sapling");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void energyCapPersistenceAndOutputProtection(GameTestHelper h){
        var be=machine(h,true);
        String[] fuels={"rough_spatial_crystal_block","excited_rough_spatial_crystal_block","spatial_crystal_shard","deflection_crystal"};
        int[] values={1,2,4,6};
        for(int i=0;i<fuels.length;i++)h.assertTrue(ReversalBlockEntity.spaceFuel(stack("omnira:"+fuels[i]))==values[i],"Wrong fuel value");
        h.assertTrue(!be.canPlaceItem(2,stack("omnira:time_microcore")),"Temporal fuel accepted");
        be.setItem(2,new ItemStack(stack("omnira:deflection_crystal").getItem(),11));ticks(be,11);
        h.assertTrue(be.energy()==60&&be.getItem(2).getCount()==1,"Overflow fuel lost");
        be.setItem(2,stack("omnira:spatial_crystal_shard"));be.tick();
        h.assertTrue(be.energy()==64,"Energy cap not reached");
        be.setItem(2,stack("omnira:rough_spatial_crystal_block"));be.tick();
        h.assertTrue(be.energy()==64&&be.getItem(2).getCount()==1,"Fuel consumed at cap");be.setItem(2,ItemStack.EMPTY);
        be.setItem(0,new ItemStack(Items.SLIME_BALL));be.setItem(1,new ItemStack(Items.STONE));ticks(be,200);
        h.assertTrue(be.progress()==0&&be.energy()==64&&be.getItem(0).getCount()==1,"Blocked output consumed resources");
        be.setItem(1,new ItemStack(Items.ENDER_PEARL,16));ticks(be,200);
        h.assertTrue(be.energy()==64&&be.getItem(0).getCount()==1,"Full output consumed resources");
        be.setItem(1,ItemStack.EMPTY);ticks(be,75);
        var saved=be.saveWithFullMetadata(h.getLevel().registryAccess());var copy=new ReversalBlockEntity(be.getBlockPos(),be.getBlockState());
        copy.setLevel(h.getLevel());copy.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(copy.infusion()&&copy.progress()==75&&copy.energy()==64,"Infuser persistence lost");ticks(copy,125);
        h.assertTrue(copy.getItem(1).is(Items.ENDER_PEARL)&&copy.energy()==63,"Resumed operation incorrect");
        be.setItem(0,new ItemStack(Items.OBSIDIAN));be.tick();h.assertTrue(be.progress()==1,"Changed input inherited progress");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void assemblyIsShapelessWithFixedCenter(GameTestHelper h){
        var recipe=(AssemblyRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:assembly/void_infuser")).orElseThrow().value();
        var input=new SimpleContainer(7);
        input.setItem(0,stack("omnira:repaired_mechanical_component"));input.setItem(2,stack("omnira:dense_void_mass"));
        input.setItem(4,stack("omnira:repaired_mechanical_component"));input.setItem(5,stack("omnira:repaired_mechanical_component"));
        input.setItem(6,stack("omnira:void_crystal_ball"));
        h.assertTrue(recipe.matches(new AssemblyRecipe.Input(input),h.getLevel())&&recipe.result().is(ReversalContent.INFUSER_ITEM.get()),"Shapeless assembly rejected");
        input.setItem(6,stack("omnira:dense_void_mass"));input.setItem(2,stack("omnira:void_crystal_ball"));
        h.assertTrue(!recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Center not fixed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void portsMenusAndMassToolSupport(GameTestHelper h){
        var be=machine(h,true);var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new ReversalMenu(1,player.getInventory(),be);
        h.assertTrue(menu.infusion&&menu.duration()==200&&menu.getType()==ReversalContent.INFUSER_MENU.get(),"Wrong infuser menu");
        h.assertTrue(be.getSlotsForFace(Direction.UP)[0]==0&&be.getSlotsForFace(Direction.DOWN)[0]==1&&be.getSlotsForFace(Direction.NORTH)[0]==2,"Wrong automation ports");
        h.assertTrue(be.canPlaceItem(0,new ItemStack(Items.SLIME_BALL))&&!be.canPlaceItem(0,stack("omnira:time_warp_point")),"Recipe types mixed");
        player.getInventory().setItem(9,new ItemStack(Items.SLIME_BALL));menu.quickMoveStack(player,3);
        h.assertTrue(be.getItem(0).is(Items.SLIME_BALL),"Shift input failed");
        var state=AbyssContent.MASS.get().defaultBlockState();
        h.assertTrue(AbyssContent.MASS.get().asItem() instanceof BlockItem&&new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state)&&!new ItemStack(Items.WOODEN_AXE).isCorrectToolForDrops(state),"Mass must be placeable and pickaxe-mineable");
        var drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),be.getBlockPos(),null,player,new ItemStack(Items.WOODEN_PICKAXE));
        h.assertTrue(drops.size()==1&&drops.getFirst().is(AbyssContent.MASS.get().asItem()),"Mass drop missing");h.succeed();
    }
}
