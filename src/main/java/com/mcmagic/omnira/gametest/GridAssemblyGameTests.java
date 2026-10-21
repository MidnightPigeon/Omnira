package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_grid_assembly")
@PrefixGameTestTemplate(false)
public final class GridAssemblyGameTests {
    private static ItemStack arcaneGrid() {
        var grid=populatedGrid().transmuteCopy(ModItems.ARCANE_CRYSTAL_GRID.get(),1);
        var contents=NonNullList.withSize(8,ItemStack.EMPTY);
        grid.get(DataComponents.CONTAINER).copyInto(contents);
        contents.set(7,contents.get(2).copy());
        grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));return grid;
    }
    private static void assertOmniUpgrade(GameTestHelper h,ItemStack before,ItemStack after) {
        h.assertTrue(after.is(ModItems.OMNI_CRYSTAL_GRID.get())&&after.getCount()==1,"Wrong omni upgrade result");
        h.assertTrue(before.getComponents().equals(after.getComponents()),"Upgrade changed grid data or spell ordering");
        var slots=NonNullList.withSize(12,ItemStack.EMPTY);after.get(DataComponents.CONTAINER).copyInto(slots);
        for(int i=8;i<12;i++)h.assertTrue(slots.get(i).isEmpty(),"New slots must be empty");
    }
    @GameTest(template="spell_arena")
    public static void omniGridRecipePreservesDataAndRequiresNodes(GameTestHelper h) {
        var recipe=(com.mcmagic.omnira.forging.AdvancedForgeRecipe)h.getLevel().getRecipeManager()
                .byKey(id("advanced_assembly_table/omni_crystal_grid")).orElseThrow().value();
        var inventory=new SimpleContainer(9);var grid=arcaneGrid();var before=grid.copy();
        inventory.setItem(6,new ItemStack(ModItems.OMNI_MICROCORE.get()));inventory.setItem(7,grid);
        inventory.setItem(8,new ItemStack(ModItems.OMNI_MICROCORE.get()));
        var input=new com.mcmagic.omnira.forging.AdvancedForgeRecipe.Input(inventory);
        for(int rotation=0;rotation<6;rotation++){
            for(int i=0;i<6;i++)inventory.setItem(i,new ItemStack((i+rotation)%6<3?
                    com.mcmagic.omnira.paradox.ParadoxContent.GENESIS.get():ModItems.GRID_FRAME.get()));
            h.assertTrue(recipe.matches(input,h.getLevel()),"Shuffled tabletop materials rejected");
            assertOmniUpgrade(h,before,recipe.assemble(input,h.getLevel().registryAccess()));
            h.assertTrue(ItemStack.matches(before,grid),"Upgrade preview changed source");
        }
        inventory.setItem(0,ItemStack.EMPTY);h.assertTrue(!recipe.matches(input,h.getLevel()),"Missing material accepted");
        inventory.setItem(0,new ItemStack(ModItems.GRID_FRAME.get()));
        inventory.setItem(7,inventory.getItem(6));inventory.setItem(6,grid);
        h.assertTrue(!recipe.matches(input,h.getLevel()),"Wrong node order accepted");
        inventory.setItem(6,new ItemStack(ModItems.OMNI_MICROCORE.get()));inventory.setItem(7,grid);
        var invalid=NonNullList.withSize(9,new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get()));
        grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(invalid));
        h.assertTrue(!recipe.matches(input,h.getLevel()),"Oversized source inventory accepted");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void realAdvancedForgeUpgradesOmniGrid(GameTestHelper h) {
        var center=h.absolutePos(new net.minecraft.core.BlockPos(5,2,5));
        var player=AdvancedForgeGameTests.player(h,center);AdvancedForgeGameTests.build(h,center,net.minecraft.core.Direction.SOUTH);
        h.assertTrue(com.mcmagic.omnira.forging.ForgeLayout.form(h.getLevel(),center,player,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())),"Cannot form advanced forge");
        var forge=(com.mcmagic.omnira.forging.AdvancedForgeBlockEntity)h.getLevel().getBlockEntity(center);
        var recipe=(com.mcmagic.omnira.forging.AdvancedForgeRecipe)h.getLevel().getRecipeManager()
                .byKey(id("advanced_assembly_table/omni_crystal_grid")).orElseThrow().value();
        for(int i=0;i<9;i++)forge.setItem(i,recipe.slots().get(i).getItems()[0].copy());
        var source=arcaneGrid();forge.setItem(7,source);forge.setItem(9,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        h.assertTrue(forge.start(player),"Populated grid cannot start forging");
        for(int i=0;i<com.mcmagic.omnira.forging.AdvancedForgeBlockEntity.duration(forge.getItem(9));i++)forge.tick();
        h.assertTrue(!forge.working(),"Advanced forge did not finish");assertOmniUpgrade(h,source,forge.output());
        for(int i=0;i<9;i++)h.assertTrue(forge.getItem(i).isEmpty(),"Upgrade ingredients not consumed");
        h.assertTrue(forge.getItem(9).is(ModItems.TEST_SPELL_CORE.get()),"Power core consumed");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void advancedGridsProvideEquippedCooldownAttribute(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"grid-cooldown"));
        for(var holder:java.util.List.of(ModItems.BASIC_CRYSTAL_GRID,ModItems.ELEMENTAL_CRYSTAL_GRID,ModItems.ARCANE_CRYSTAL_GRID,ModItems.OMNI_CRYSTAL_GRID)) {
            var stack=new ItemStack(holder.get());var item=(com.mcmagic.omnira.item.CrystalGridItem)stack.getItem();
            double expected=holder==ModItems.ARCANE_CRYSTAL_GRID || holder==ModItems.OMNI_CRYSTAL_GRID?.1:0;
            var context=new top.theillusivec4.curios.api.SlotContext("crystal_grid",player,0,false,true);
            var mods=item.getAttributeModifiers(context,id("test_grid"),stack).get(ModAttributes.COOLDOWN_REDUCTION);
            h.assertTrue(Math.abs(mods.stream().mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::amount).sum()-expected)<1e-6,"Wrong grid cooldown bonus");
            var attr=player.getAttribute(ModAttributes.COOLDOWN_REDUCTION);attr.removeModifier(id("test_grid"));
            for(var modifier:mods)attr.addTransientModifier(modifier);
            h.assertTrue(SpellCooldowns.ticks(player,80,1)==(expected==0?80:72),"Equipped bonus not applied to 4-second loop");
            var wrong=new top.theillusivec4.curios.api.SlotContext("spell_core",player,0,false,true);
            h.assertTrue(item.getAttributeModifiers(wrong,id("wrong"),stack).isEmpty(),"Unequipped/wrong-slot bonus leaked");
            for(var modifier:mods)attr.removeModifier(modifier.id());
        }
        player.getAttribute(ModAttributes.COOLDOWN_REDUCTION).setBaseValue(1);
        h.assertTrue(SpellCooldowns.ticks(player,80,1)==40,"50 percent cap bypassed");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void chainCooldownSkipsGapsAndIncludesBurstBoundary(GameTestHelper h) {
        var grid=populatedGrid();
        grid.set(ModDataComponents.GRID_CURSOR,0);
        h.assertTrue(com.mcmagic.omnira.item.StaffItem.sequenceCooldown(grid,1,20)==20,"Chain interior lost staff interval");
        h.assertTrue(com.mcmagic.omnira.item.StaffItem.sequenceCooldown(grid,2,20)==80,"Burst ending at boundary missed cycle cooldown");
        grid.set(ModDataComponents.GRID_CURSOR,2);
        h.assertTrue(com.mcmagic.omnira.item.StaffItem.sequenceCooldown(grid,2,100)==100,"Burst boundary must keep the greater cooldown");
        var slots=NonNullList.withSize(3,ItemStack.EMPTY);
        slots.set(2,grid.get(DataComponents.CONTAINER).getStackInSlot(2));
        grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(slots));
        for(int base:new int[]{20,60,100}) for(int shots:new int[]{1,2})
            h.assertTrue(com.mcmagic.omnira.item.StaffItem.sequenceCooldown(grid,shots,base)==Math.max(80,base),"Single spell must keep the greater cooldown");
        h.assertTrue(SpellCooldowns.ticks(80,.2,1)==64 && SpellCooldowns.ticks(80,1,1)==40,"Cycle reduction/cap changed");
        h.assertTrue(grid.get(ModDataComponents.GRID_CURSOR)==2,"Cooldown preview advanced live cursor");
        h.assertTrue(com.mcmagic.omnira.item.ArquebusPlugin.NONE.cooldownTicks==40
                && com.mcmagic.omnira.item.ArquebusPlugin.KINGS_NEW_CLOTHES.cooldownTicks==40
                && com.mcmagic.omnira.item.ArquebusPlugin.ANCESTOR_LAUNCHER.cooldownTicks==60,"Gun cooldown profiles incorrect");
        h.succeed();
    }
    private static ResourceLocation id(String path) {return ResourceLocation.fromNamespaceAndPath("omnira",path);}
    private static AssemblyRecipe recipe(GameTestHelper h,String name) {
        return (AssemblyRecipe)h.getLevel().getRecipeManager().byKey(id("assembly/"+name)).orElseThrow().value();
    }
    @GameTest(template="spell_arena")
    public static void frameCraftingAndEquipmentRestrictions(GameTestHelper h) {
        var frame=new ItemStack(ModItems.GRID_FRAME.get());
        h.assertTrue(!(frame.getItem() instanceof top.theillusivec4.curios.api.type.capability.ICurioItem),"Frame implements equipment behavior");
        var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.fromNamespaceAndPath("curios","crystal_grid"));
        h.assertTrue(!frame.is(tag),"Frame belongs to grid equipment tag");
        var crafting=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(id("grid_frame")).orElseThrow().value();
        var items=NonNullList.withSize(9,ItemStack.EMPTY);
        for(int i=0;i<9;i++) if(i!=4) items.set(i,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        h.assertTrue(crafting.matches(CraftingInput.of(3,3,items),h.getLevel()),"Eight-crystal crafting recipe failed");
        items.set(4,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        h.assertTrue(!crafting.matches(CraftingInput.of(3,3,items),h.getLevel()),"Filled center accepted");
        var assembly=new SimpleContainer(7);
        for(int i=0;i<6;i++) assembly.setItem(i,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        h.assertTrue(recipe(h,"grid_frame").matches(new AssemblyRecipe.Input(assembly),h.getLevel()),"Six-crystal forging recipe failed");
        assembly.setItem(0,ItemStack.EMPTY);
        h.assertTrue(!recipe(h,"grid_frame").matches(new AssemblyRecipe.Input(assembly),h.getLevel()),"Incomplete frame accepted");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void basicGridRequiresAlternatingCrystals(GameTestHelper h) {
        var recipe=recipe(h,"basic_crystal_grid");
        for(int parity=0;parity<2;parity++) {
            var input=new SimpleContainer(7);input.setItem(6,new ItemStack(ModItems.GRID_FRAME.get()));
            for(int i=parity;i<6;i+=2) input.setItem(i,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
            h.assertTrue(recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Alternating frame recipe failed");
            input.setItem(1-parity,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
            h.assertTrue(!recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Extra crystal accepted");
        }
        h.succeed();
    }
    private static ItemStack populatedGrid() {
        var grid=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());
        var slots=NonNullList.withSize(3,ItemStack.EMPTY);
        for(int i:new int[]{0,2}) {
            var crystal=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
            crystal.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(i==0?ElementType.AIR:ElementType.WATER,ElementType.AIR));
            crystal.set(ModDataComponents.SPELL_PAYLOAD,new SpellPayload(30+i,0));
            crystal.set(DataComponents.CUSTOM_NAME,Component.literal("Spell "+i));slots.set(i,crystal);
        }
        grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(slots));
        grid.set(DataComponents.CUSTOM_NAME,Component.literal("My spell chain"));
        grid.set(ModDataComponents.GRID_CURSOR,2);return grid;
    }
    private static void assertUpgrade(GameTestHelper h,ItemStack before,ItemStack after) {
        h.assertTrue(after.is(ModItems.ELEMENTAL_CRYSTAL_GRID.get()),"Wrong upgrade result");
        h.assertTrue(before.get(DataComponents.CUSTOM_NAME).equals(after.get(DataComponents.CUSTOM_NAME))
                && after.getOrDefault(ModDataComponents.GRID_CURSOR,-1)==2,"Name or cursor lost");
        var old=NonNullList.withSize(3,ItemStack.EMPTY);var enlarged=NonNullList.withSize(5,ItemStack.EMPTY);
        before.get(DataComponents.CONTAINER).copyInto(old);after.get(DataComponents.CONTAINER).copyInto(enlarged);
        for(int i=0;i<3;i++) h.assertTrue(ItemStack.matches(old.get(i),enlarged.get(i)),"Spell order, gap or components changed");
        h.assertTrue(enlarged.get(3).isEmpty() && enlarged.get(4).isEmpty(),"New slots are not trailing empty slots");
    }
    @GameTest(template="spell_arena")
    public static void upgradeAcceptsMicrocoreTagAndPreservesContents(GameTestHelper h) {
        var recipe=recipe(h,"elemental_crystal_grid");var grid=populatedGrid();var original=grid.copy();
        var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,id("microcores"));
        var cores=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(item->new ItemStack(item).is(tag)).toList();
        h.assertTrue(cores.size()>=4,"Microcore tag empty");
        for(var left:cores) for(var right:cores) {
            var inventory=new SimpleContainer(7);inventory.setItem(6,grid);
            inventory.setItem(4,new ItemStack(left));inventory.setItem(1,new ItemStack(right));
            var input=new AssemblyRecipe.Input(inventory);
            h.assertTrue(recipe.matches(input,h.getLevel()),"Tagged microcore combination rejected");
            assertUpgrade(h,original,recipe.assemble(input,h.getLevel().registryAccess()));
            h.assertTrue(ItemStack.matches(grid,original),"Preview mutated the input grid");
            inventory.setItem(4,new ItemStack(Items.DIAMOND));
            h.assertTrue(!recipe.matches(input,h.getLevel()),"Non-core upgrade material accepted");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=230)
    public static void realForgingKeepsTheSpellChain(GameTestHelper h) {
        var pos=new net.minecraft.core.BlockPos(5,2,5);h.setBlock(pos,ModBlocks.ARCANE_ASSEMBLY_TABLE.get());
        var table=((com.mcmagic.omnira.block.entity.AssemblyAccess)h.getLevel().getBlockEntity(h.absolutePos(pos))).assembly();
        var grid=populatedGrid();var original=grid.copy();
        table.setItem(6,grid);table.setItem(4,new ItemStack(ModItems.FIRE_MICROCORE.get()));table.setItem(1,new ItemStack(ModItems.WATER_MICROCORE.get()));
        for(int i=0;i<20;i++) h.runAtTickTime(1+i*10,()->{table.setMechanicalPowered(true);table.automaticStrike();});
        h.runAtTickTime(205,()->{
            h.assertTrue(table.finished(),"Forge did not complete");assertUpgrade(h,original,table.getItem(6));
            for(int i=0;i<6;i++) h.assertTrue(table.getItem(i).isEmpty(),"Ingredients not consumed");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena")
    public static void engineHasOnlyOneArrangement(GameTestHelper h) {GuideRevisionGameTests.engineRequiresFixedMaterials(h);}
}
