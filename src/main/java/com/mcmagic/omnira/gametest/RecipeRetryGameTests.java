package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.recipe.RecipeRetryGate;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_recipe_retry") @PrefixGameTestTemplate(false)
public final class RecipeRetryGameTests {
    @GameTest(template="spell_arena") public static void workstationManualStackLimits(GameTestHelper h){
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(int index=0;index<6;index++){
            var work=new com.mcmagic.omnira.workstation.CrystalWorkstationEntity(h.absolutePos(new net.minecraft.core.BlockPos(4,3,4)),com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(index).get().defaultBlockState());work.setLevel(h.getLevel());
            var menu=new com.mcmagic.omnira.workstation.CrystalWorkstationMenu(1,player.getInventory(),work);
            for(int slot=0;slot<2;slot++)h.assertTrue(menu.getSlot(slot).getMaxStackSize()==(index%3==1?1:64),"Wrong manual capacity: machine "+index+", slot "+slot);
            if(index%3!=0)continue;
            menu.setCarried(new ItemStack(Items.COAL,64));menu.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            h.assertTrue(work.getItem(1).getCount()==64&&menu.getCarried().isEmpty(),"Manual fuel stack truncated: "+index);
            menu.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            h.assertTrue(work.getItem(1).isEmpty()&&menu.getCarried().getCount()==64,"Fuel pickup lost items");menu.setCarried(ItemStack.EMPTY);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void furnaceCatalogueWakesAndResumes(GameTestHelper h){
        var level=h.getLevel();int checked=0;
        for(var holder:level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING)){
            var choices=holder.value().getIngredients().getFirst().getItems();if(choices.length==0)continue;
            var input=choices[0].copyWithCount(1);
            var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,new net.minecraft.world.item.crafting.SingleRecipeInput(input),level).orElseThrow().value();
            var expected=recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(input),level.registryAccess());
            for(boolean automatic:new boolean[]{false,true}){
                var work=new com.mcmagic.omnira.workstation.CrystalWorkstationEntity(h.absolutePos(new net.minecraft.core.BlockPos(4,3,4)),com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(3).get().defaultBlockState());work.setLevel(level);
                work.setItem(0,new ItemStack(Items.BARRIER));for(int i=0;i<20;i++)work.tick(true,16);
                work.setItem(0,ItemStack.EMPTY);work.setItem(2,new ItemStack(Items.BARRIER,64));
                if(automatic){var handler=new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(work,net.minecraft.core.Direction.UP);h.assertTrue(handler.insertItem(0,input.copy(),false).isEmpty(),"Automated input rejected: "+holder.id());}
                else work.setItem(0,input.copy());
                for(int i=0;i<5;i++)work.tick(true,16);
                h.assertTrue(ItemStack.matches(work.getItem(0),input),"Blocked output consumed input");work.removeItem(2,64);
                for(int i=0;i<Math.max(1,recipe.getCookingTime()/2);i++)work.tick(true,16);
                h.assertTrue(ItemStack.matches(work.getItem(2),expected)&&work.getItem(0).isEmpty(),"Idle/resume failed: "+holder.id()+", automated="+automatic);checked++;
            }
        }
        org.slf4j.LoggerFactory.getLogger("OmniraRecipeAudit").info("Validated {} idle furnace manual/automation catalogue cases",checked);h.succeed();
    }
    @GameTest(template="spell_arena") public static void unchangedInputsDoNotRetry(GameTestHelper h){
        var gate=new RecipeRetryGate();var item=new ItemStack(Items.STICK,4);var fluid=new FluidStack(Fluids.WATER,1000);
        int retries=0;
        for(int tick=0;tick<200;tick++){
            gate.begin(h.getLevel());gate.item(item);gate.fluid(fluid);gate.value(Blocks.STONE.defaultBlockState());
            if(gate.changed())retries++;
        }
        h.assertTrue(retries==1,"Unchanged idle inputs triggered "+retries+" recipe checks");
        item.shrink(1);gate.begin(h.getLevel());gate.item(item);gate.fluid(fluid);gate.value(Blocks.STONE.defaultBlockState());
        h.assertTrue(gate.changed(),"In-place item mutation was missed");
        fluid.shrink(100);gate.begin(h.getLevel());gate.item(item);gate.fluid(fluid);gate.value(Blocks.STONE.defaultBlockState());
        h.assertTrue(gate.changed(),"In-place fluid mutation was missed");
        gate.begin(h.getLevel());gate.item(item);gate.fluid(fluid);gate.value(Blocks.AIR.defaultBlockState());
        h.assertTrue(gate.changed(),"Catalyst change was missed");
        gate.invalidate();gate.begin(h.getLevel());gate.item(item);gate.fluid(fluid);gate.value(Blocks.AIR.defaultBlockState());
        h.assertTrue(gate.changed(),"Completion did not allow retry");h.succeed();
    }
    @GameTest(template="spell_arena") public static void recipeReloadInvalidatesGate(GameTestHelper h){
        var gate=new RecipeRetryGate();var level=h.getLevel();var manager=level.getRecipeManager();
        gate.begin(level);gate.item(new ItemStack(Items.STICK));gate.changed();
        gate.begin(level);gate.item(new ItemStack(Items.STICK));h.assertTrue(!gate.changed(),"Identical replacement stack retried");
        manager.replaceRecipes(new java.util.ArrayList<>(manager.getRecipes()));
        gate.begin(level);gate.item(new ItemStack(Items.STICK));h.assertTrue(gate.changed(),"Same-manager recipe replacement was missed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void idleCauldronWakesForCatalyst(GameTestHelper h){
        var level=h.getLevel();var manager=level.getRecipeManager();var old=new java.util.ArrayList<>(manager.getRecipes());
        try{
            var recipe=new com.mcmagic.omnira.alchemy.ReactionRecipe(java.util.List.of(new com.mcmagic.omnira.alchemy.ReactionRecipe.InputItem(net.minecraft.world.item.crafting.Ingredient.of(Items.STICK),1)),
                    new FluidStack(Fluids.WATER,1000),net.minecraft.world.item.crafting.Ingredient.of(Blocks.STONE),java.util.List.of(new ItemStack(Items.DIAMOND)),FluidStack.EMPTY,20);
            var recipes=new java.util.ArrayList<>(old);recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceLocation.parse("omnira_test:retry_probe"),recipe));manager.replaceRecipes(recipes);
            var pos=h.absolutePos(new net.minecraft.core.BlockPos(4,3,4));level.setBlockAndUpdate(pos,com.mcmagic.omnira.registry.ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
            var vessel=(com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity)level.getBlockEntity(pos);
            vessel.items.setStackInSlot(0,new ItemStack(Items.STICK));vessel.tank.setFluid(new FluidStack(Fluids.WATER,1000));level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());
            for(int i=0;i<20;i++)com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,vessel.getBlockState(),vessel);
            h.assertTrue(!vessel.working(),"Missing catalyst started work");
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity.tick(level,pos,vessel.getBlockState(),vessel);
            h.assertTrue(vessel.working(),"Idle machine ignored newly placed catalyst");h.succeed();
        }finally{manager.replaceRecipes(old);}
    }
}
