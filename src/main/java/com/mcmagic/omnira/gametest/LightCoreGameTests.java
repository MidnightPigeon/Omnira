package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.recipe.*;
import com.mcmagic.omnira.block.entity.AssemblyAccess;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_light_core")
@PrefixGameTestTemplate(false)
public final class LightCoreGameTests {
    private static Recipe<?> recipe(GameTestHelper h,String name) {
        return h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira",name)).orElseThrow().value();
    }
    @GameTest(template="spell_arena")
    public static void recipesAndAttributes(GameTestHelper h) {
        var core=new ItemStack(ModItems.CRUDE_LIGHT_CORE.get());
        var crystal=new ItemStack(ModItems.ANALYSIS_CRYSTAL.get());crystal.setDamageValue(127);
        var forward=(CraftingRecipe)recipe(h,"crystal_analysis/light");
        var input=CraftingInput.of(3,2,java.util.List.of(core,core,crystal,core,core,ItemStack.EMPTY));
        h.assertTrue(forward.matches(input,h.getLevel()),"Four-core analysis failed");
        h.assertTrue(forward.assemble(input,h.getLevel().registryAccess()).is(ModItems.LIGHT_MICROCORE.get()),"Wrong light microcore output");
        h.assertTrue(forward.getRemainingItems(input).get(2).isEmpty(),"Last durability not consumed");
        var reverse=(CraftingRecipe)recipe(h,"crystal_analysis/light_reverse");
        var ri=CraftingInput.of(2,1,java.util.List.of(new ItemStack(ModItems.LIGHT_MICROCORE.get()),new ItemStack(ModItems.ANALYSIS_CRYSTAL.get())));
        h.assertTrue(reverse.matches(ri,h.getLevel()),"Reverse mismatch");
        var output=reverse.assemble(ri,h.getLevel().registryAccess());
        h.assertTrue(output.is(core.getItem()) && output.getCount()==8,"Wrong reverse count");
        h.assertTrue(reverse.getRemainingItems(ri).get(1).getDamageValue()==1,"Reverse wear incorrect");
        var analysis=(AnalysisRecipe)recipe(h,"analysis/light");
        h.assertTrue(analysis.matches(new SingleRecipeInput(core.copyWithCount(16)),h.getLevel())
                && !analysis.matches(new SingleRecipeInput(core.copyWithCount(15)),h.getLevel())
                && analysis.dustMin()==16 && analysis.dustMax()==16,"Analysis amounts wrong");
        var source=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("omnira","light_source_crystal"));
        var crafting=(CraftingRecipe)recipe(h,"crude_light_core");
        h.assertTrue(crafting.matches(CraftingInput.of(2,2,java.util.Collections.nCopies(4,new ItemStack(source))),h.getLevel()),"2x2 recipe failed");
        var container=new SimpleContainer(7);container.setItem(6,new ItemStack(source));
        h.assertTrue(((AssemblyRecipe)recipe(h,"assembly/crude_light_core")).matches(new AssemblyRecipe.Input(container),h.getLevel()),"Single crystal forging failed");
        var elements=(AssemblyRecipe)recipe(h,"assembly/spell_core_elements");
        Item[] cores={ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.FIRE_MICROCORE.get(),ModItems.AIR_MICROCORE.get()};
        for(int shift=0;shift<6;shift++) {
            container.clearContent();container.setItem(6,core);
            for(int i=0;i<4;i++) container.setItem((i+shift)%6,new ItemStack(cores[i]));
            h.assertTrue(elements.matches(new AssemblyRecipe.Input(container),h.getLevel()),"Outer order restricted");
        }
        container.setItem(0,new ItemStack(ModItems.ANALYSIS_CRYSTAL.get()));
        h.assertTrue(!elements.matches(new AssemblyRecipe.Input(container),h.getLevel()),"Invalid element set accepted");
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"light-core-test"));
        var spellCore=new ItemStack(ModItems.TEST_SPELL_CORE.get());
        var modifiers=((com.mcmagic.omnira.item.SpellCoreItem)spellCore.getItem()).getAttributeModifiers(new top.theillusivec4.curios.api.SlotContext("spell_core",player,0,false,true),ResourceLocation.fromNamespaceAndPath("omnira","test_core"),spellCore);
        modifiers.forEach((a,m)->player.getAttribute(a).addTransientModifier(m));
        h.assertTrue(player.getAttributeValue(ModAttributes.MAX_MANA)>100
                && player.getAttributeValue(ModAttributes.SPELL_POWER)>1
                && player.getAttributeValue(ModAttributes.MANA_REGEN)>1,"Core attributes did not increase");
        modifiers.forEach((a,m)->player.getAttribute(a).removeModifier(m.id()));
        h.assertTrue(player.getAttributeValue(ModAttributes.MAX_MANA)==100,"Unequip leaves bonus");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void forgingConsumesEntireCrystal(GameTestHelper h) {
        var pos=new net.minecraft.core.BlockPos(3,2,3);h.setBlock(pos,ModBlocks.ARCANE_ASSEMBLY_TABLE.get());
        var table=((AssemblyAccess)h.getLevel().getBlockEntity(h.absolutePos(pos))).assembly();
        table.setItem(6,new ItemStack(ModItems.CRUDE_LIGHT_CORE.get()));
        table.setItem(5,new ItemStack(ModItems.ANALYSIS_CRYSTAL.get()));
        table.setMechanicalPowered(true);
        for(int tick=1;tick<=201;tick+=10) h.runAtTickTime(tick,()->{table.setMechanicalPowered(true);table.automaticStrike();});
        h.runAtTickTime(205,()->{
            h.assertTrue(table.finished() && table.getItem(6).is(ModItems.TEST_SPELL_CORE.get()),"Core forging failed");
            for(int i=0;i<6;i++) h.assertTrue(table.getItem(i).isEmpty(),"Analysis crystal was returned instead of consumed");
            h.succeed();
        });
    }
}
