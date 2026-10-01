package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.recipe.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_shadow_crafting")
@PrefixGameTestTemplate(false)
public final class ShadowCraftingGameTests {
    private static Recipe<?> recipe(GameTestHelper h,String name) {
        return h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira",name)).orElseThrow().value();
    }
    private static Item item(String name) {return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("omnira",name));}
    @GameTest(template="spell_arena")
    public static void additionalAssemblyAndDrops(GameTestHelper h) {
        for(String name:new String[]{"diamond_reinforcement","light_core_reinforcement","spiritual_crystal_tip","elemental_fusion_tip_elements","elemental_fusion_tip_analysis","ritual_energy_core","dark_microcore","spacetime_knot"}) {
            var recipe=(AssemblyRecipe)recipe(h,"assembly/"+name);var container=new SimpleContainer(7);
            for(int i=0;i<7;i++) if(!recipe.slots().get(i).isEmpty()) container.setItem(i,recipe.slots().get(i).getItems()[0].copy());
            h.assertTrue(recipe.matches(new AssemblyRecipe.Input(container),h.getLevel()),"Assembly mismatch: "+name);
            h.assertTrue(!recipe.assemble(new AssemblyRecipe.Input(container),h.getLevel().registryAccess()).isEmpty(),"Empty output: "+name);
            for(int i=0;i<7;i++) if(!container.getItem(i).isEmpty()) {container.setItem(i,ItemStack.EMPTY);break;}
            h.assertTrue(!recipe.matches(new AssemblyRecipe.Input(container),h.getLevel()),"Incomplete recipe accepted: "+name);
        }
        var ghost=ModEntityTypes.SHADOW_GHOST.get().create(h.getLevel());
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,ghost)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,ghost.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,h.getLevel().damageSources().generic())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
        var drops=h.getLevel().getServer().reloadableRegistries().getLootTable(ghost.getLootTable()).getRandomItems(params);
        h.assertTrue(drops.size()==1 && drops.getFirst().is(ModItems.SHADOW_MANUSCRIPT.get()),"Shadow ghost manuscript drop missing");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void shaftsAndParts(GameTestHelper h) {
        Item[] materials={Items.OAK_LOG,item("shadow_wood"),Items.BLAZE_ROD};
        String[] names={"wooden_staff_shaft","shadow_staff_shaft","blaze_staff_shaft"};
        for(int n=0;n<3;n++) {
            var slots=NonNullList.withSize(9,ItemStack.EMPTY);
            for(int i:new int[]{2,4,6}) slots.set(i,new ItemStack(materials[n]));
            var crafting=(CraftingRecipe)recipe(h,names[n]);
            h.assertTrue(crafting.matches(CraftingInput.of(3,3,slots),h.getLevel()),"Diagonal shaft mismatch");
            slots.set(2,ItemStack.EMPTY);
            h.assertTrue(!crafting.matches(CraftingInput.of(3,3,slots),h.getLevel()),"Two materials accepted");
            var input=new SimpleContainer(7);input.setItem(6,new ItemStack(materials[n]));
            h.assertTrue(((AssemblyRecipe)recipe(h,"assembly/"+names[n])).matches(new AssemblyRecipe.Input(input),h.getLevel()),"Single material shaft mismatch");
        }
        var shadow=new SimpleContainer(7);shadow.setItem(6,new ItemStack(item("shadow_log")));
        h.assertTrue(!((AssemblyRecipe)recipe(h,"assembly/wooden_staff_shaft")).matches(new AssemblyRecipe.Input(shadow),h.getLevel()),"Shadow log ambiguously makes ordinary shaft");
        var smithing=(SmithingRecipe)recipe(h,"netherite_reinforcement");
        var smithInput=new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),new ItemStack(ModItems.DIAMOND_REINFORCEMENT.get()),new ItemStack(Items.NETHERITE_INGOT));
        h.assertTrue(smithing.matches(smithInput,h.getLevel()) && smithing.assemble(smithInput,h.getLevel().registryAccess()).is(ModItems.NETHERITE_REINFORCEMENT.get()),"Smithing upgrade mismatch");
        h.assertTrue(com.mcmagic.omnira.item.staff.StaffPart.of(smithing.assemble(smithInput,h.getLevel().registryAccess())).reduction()==10,"Smithing retained diamond stats");
        h.assertTrue(!smithing.matches(new SmithingRecipeInput(ItemStack.EMPTY,smithInput.base(),smithInput.addition()),h.getLevel()),"Smithing accepted missing template");
        var ink=NonNullList.withSize(9,ItemStack.EMPTY);ink.set(0,new ItemStack(Items.GLASS_BOTTLE));
        for(int i=1;i<5;i++) ink.set(i,new ItemStack(Items.INK_SAC));
        for(int i=5;i<9;i++) ink.set(i,new ItemStack(ModItems.ARCANE_DUST.get()));
        h.assertTrue(((CraftingRecipe)recipe(h,"spell_ink")).matches(CraftingInput.of(3,3,ink),h.getLevel()),"Ink recipe mismatch");
        h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("omnira","spell_manuscript"))==Items.AIR,"Old manuscript ID retained");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void shadowAnalysisAndGrid(GameTestHelper h) {
        var analysis=(AnalysisRecipe)recipe(h,"analysis/shadow_mist");
        h.assertTrue(analysis.matches(new SingleRecipeInput(new ItemStack(ModItems.SHADOW_MANUSCRIPT.get(),4)),h.getLevel()) && analysis.dustMin()==8 && analysis.dustMax()==10,"Manuscript analysis incorrect");
        h.assertTrue(!analysis.matches(new SingleRecipeInput(new ItemStack(ModItems.SHADOW_MANUSCRIPT.get(),3)),h.getLevel()),"Too few manuscripts accepted");
        var fast=(CraftingRecipe)recipe(h,"crystal_analysis/shadow_mist");
        var input=CraftingInput.of(2,1,java.util.List.of(new ItemStack(ModItems.ANALYSIS_CRYSTAL.get()),new ItemStack(ModItems.SHADOW_MANUSCRIPT.get())));
        h.assertTrue(fast.matches(input,h.getLevel()) && fast.assemble(input,h.getLevel().registryAccess()).is(ModItems.SHADOW_MIST.get()),"Fast mist recipe mismatch");
        h.assertTrue(fast.getRemainingItems(input).get(0).getDamageValue()==1,"Analysis crystal durability not consumed");
        var grid=new ItemStack(ModItems.ELEMENTAL_CRYSTAL_GRID.get());
        grid.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Saved chain"));
        var spells=NonNullList.withSize(5,ItemStack.EMPTY);spells.set(4,new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get()));
        grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(spells));grid.set(ModDataComponents.GRID_CURSOR,4);
        var slots=new SimpleContainer(7);slots.setItem(6,grid);
        for(int i=0;i<6;i++) slots.setItem(i,new ItemStack(i==1?ModItems.LIGHT_MICROCORE.get():i==4?ModItems.DARK_MICROCORE.get():ModItems.DREAM_CRYSTAL_SHARD.get()));
        var upgrade=(AssemblyRecipe)recipe(h,"assembly/arcane_crystal_grid");var in=new AssemblyRecipe.Input(slots);
        h.assertTrue(upgrade.matches(in,h.getLevel()),"Arcane grid layout mismatch");
        var result=upgrade.assemble(in,h.getLevel().registryAccess());
        h.assertTrue(result.is(ModItems.ARCANE_CRYSTAL_GRID.get()) && result.get(DataComponents.CONTAINER).equals(grid.get(DataComponents.CONTAINER))
                && result.get(ModDataComponents.GRID_CURSOR)==4 && result.get(DataComponents.CUSTOM_NAME).equals(grid.get(DataComponents.CUSTOM_NAME)),"Grid contents lost");
        var expanded=NonNullList.withSize(8,ItemStack.EMPTY);result.get(DataComponents.CONTAINER).copyInto(expanded);
        h.assertTrue(expanded.get(5).isEmpty()&&expanded.get(7).isEmpty(),"New grid positions not empty");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void mistExpandsAppliesDarknessAndExpires(GameTestHelper h) {
        var mist=ModEntityTypes.SHADOW_MIST.get().create(h.getLevel());var pos=h.absoluteVec(new net.minecraft.world.phys.Vec3(5,3,5));
        mist.setPos(pos);h.getLevel().addFreshEntity(mist);
        var mob=net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());mob.setNoAi(true);mob.setPos(pos.add(1.8,0,0));h.getLevel().addFreshEntity(mob);
        h.assertTrue(Math.abs(mist.diameter(0)-1)<.001 && !mist.isPickable(),"Mist starts wrong size or can be recovered");
        for(int i=0;i<10;i++) mist.tick();
        h.assertTrue(!mob.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS),"Mist affects outside expansion");
        for(int i=0;i<10;i++) mist.tick();
        h.assertTrue(Math.abs(mist.diameter(0)-3)<.001 && mob.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS),"Expanded mist does not apply darkness");
        h.assertTrue(mob.getEffect(net.minecraft.world.effect.MobEffects.DARKNESS).getDuration()==200,"Darkness duration incorrect");
        var tag=new net.minecraft.nbt.CompoundTag();mist.saveWithoutId(tag);var restored=ModEntityTypes.SHADOW_MIST.get().create(h.getLevel());restored.load(tag);
        h.assertTrue(restored.age()==20,"Mist age lost on save");
        h.assertTrue(mist.opacity(0)<1,"Mist not fading");
        for(int i=20;i<400;i++) mist.tick();
        h.assertTrue(mist.isRemoved(),"Mist persists beyond lifetime");mob.discard();h.succeed();
    }
}
