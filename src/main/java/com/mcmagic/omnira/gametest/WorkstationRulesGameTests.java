package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.recipe.AssemblyRecipe;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_workstation_rules")
@PrefixGameTestTemplate(false)
public final class WorkstationRulesGameTests {
    @GameTest(template="spell_arena")
    public static void crystalPanelSlots(GameTestHelper h) throws Exception {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new com.mcmagic.omnira.menu.CrystalProcessingTableMenu(1,player.getInventory(),new SimpleContainer(9));
        try(var stream=WorkstationRulesGameTests.class.getResourceAsStream("/assets/omnira/textures/gui/container/workstation_processing.png");
            var original=WorkstationRulesGameTests.class.getResourceAsStream("/assets/omnira/textures/gui/container/crystal_processing_table.png")){
            var image=javax.imageio.ImageIO.read(java.util.Objects.requireNonNull(stream));
            var oldImage=javax.imageio.ImageIO.read(java.util.Objects.requireNonNull(original));
            int[][] old={{58,36},{100,36},{30,85},{58,134},{100,134},{128,85},{65,78},{93,78},{79,99}};
            for(int i=0;i<9;i++){
                var slot=menu.getSlot(i);
                h.assertTrue(slot.getContainerSlot()==i,"UI changed material meaning");
                for(int x=0;x<16;x++)for(int y=0;y<16;y++)
                    h.assertTrue(image.getRGB(slot.x+x,slot.y+y)==oldImage.getRGB(old[i][0]+x,old[i][1]+y),"Original marker or item hit area differs for slot "+i);
                h.assertTrue(slot.y+16<124,"Material overlaps action buttons");
            }
        }
        for(int i=0;i<36;i++){
            var slot=menu.getSlot(9+i);
            int x=8+(i%9)*18,y=i<27?158+(i/9)*18:216;
            h.assertTrue(slot.x==x && slot.y==y,"Player inventory moved");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void miningTiers(GameTestHelper h) {
        var iron=new ItemStack(Items.IRON_PICKAXE);var stone=new ItemStack(Items.STONE_PICKAXE);
        for(var block:java.util.List.of(ModBlocks.CRYSTAL_PROCESSING_TABLE,ModBlocks.ANALYSIS_ARTISAN_TABLE,
                ModBlocks.ARCANE_ASSEMBLY_TABLE,ModBlocks.MANA_ENGINE,ModBlocks.CRYSTAL_CASING,
                ModBlocks.CRYSTAL_PEDESTAL,ModBlocks.RITUAL_ENERGY_CORE,ModBlocks.ADVANCED_RITUAL_ENERGY_CORE,ModBlocks.WAYMARK,
                ModBlocks.ADVANCED_CONDENSATION_TABLE,ModBlocks.PURE_VESSEL)) {
            var state=block.get().defaultBlockState();
            h.assertTrue(state.requiresCorrectToolForDrops(),"Crystal facility permits hand drops");
            h.assertTrue(iron.isCorrectToolForDrops(state) && !stone.isCorrectToolForDrops(state),"Wrong crystal facility tier");
        }
        var table=ModBlocks.SIMPLE_CONDENSATION_TABLE.get().defaultBlockState();
        h.assertTrue(!table.requiresCorrectToolForDrops(),"Condensation requires a tool");
        h.assertTrue(new ItemStack(Items.WOODEN_AXE).getDestroySpeed(table)>1,"Axe does not accelerate condensation table");
        h.assertTrue(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(DreamContent.SHADOW_ROCK.get().defaultBlockState()),"Rock requires excessive tier");
        h.assertTrue(stone.isCorrectToolForDrops(DreamContent.LIGHT_SOURCE_CRYSTAL.get().defaultBlockState()),"Natural crystal requires excessive tier");
        h.assertTrue(!iron.isCorrectToolForDrops(ModBlocks.PERMANENT_REINFORCED_VOID_CRYSTAL.get().defaultBlockState()),"Reinforced crystal lost diamond tier");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void multiTipRecipe(GameTestHelper h) {
        var recipe=(AssemblyRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira","assembly/multi_arcane_tip")).orElseThrow().value();
        var input=new SimpleContainer(7);
        for(int i=0;i<6;i++) input.setItem(i,new ItemStack(ModItems.ARCANE_DUST.get()));
        input.setItem(6,new ItemStack(ModItems.SPIRITUAL_CRYSTAL_TIP.get()));
        h.assertTrue(recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Tip recipe does not match");
        h.assertTrue(recipe.result().is(ModItems.MULTI_ARCANE_TIP.get()),"Wrong tip output");
        input.setItem(0,ItemStack.EMPTY);
        h.assertTrue(!recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Recipe accepts fewer than six dust");h.succeed();
    }
}
