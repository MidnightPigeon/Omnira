package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.menu.*;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.registry.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_advanced_condensation")
@PrefixGameTestTemplate(false)
public final class AdvancedCondensationGameTests {
    @GameTest(template="spell_arena")
    public static void crystalMaterialsAndSaplingGrowth(GameTestHelper h) {
        var recipe=h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","advanced_condensation_table")).orElseThrow().value();
        var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.List.of(
                new ItemStack(DreamContent.LIGHT_SOURCE_CRYSTAL.get()),new ItemStack(DreamContent.LIGHT_SOURCE_CRYSTAL.get()),new ItemStack(DreamContent.LIGHT_SOURCE_CRYSTAL.get()),
                new ItemStack(ModItems.RESONANCE_CRYSTAL.get()),new ItemStack(ModItems.TEST_SPELL_CORE.get()),new ItemStack(ModItems.RESONANCE_CRYSTAL.get()),
                new ItemStack(ModItems.RESONANCE_CRYSTAL.get()),ItemStack.EMPTY,new ItemStack(ModItems.RESONANCE_CRYSTAL.get())));
        h.assertTrue(((net.minecraft.world.item.crafting.CraftingRecipe)recipe).matches(input,h.getLevel()),"Advanced table materials or layout wrong");
        var pos=h.absolutePos(new BlockPos(8,2,8));var level=h.getLevel();
        level.setBlockAndUpdate(pos.below(),DreamContent.MOSSY_SHADOW_ROCK.get().defaultBlockState());
        var sapling=(com.mcmagic.omnira.block.ShadowSaplingBlock)ShadowWoodContent.SAPLING.get();
        level.setBlockAndUpdate(pos,sapling.defaultBlockState());
        h.assertTrue(level.getBlockState(pos).canSurvive(level,pos),"Sapling rejects dream soil");
        var early=level.getBlockState(pos).getShape(level,pos).bounds();
        sapling.advanceTree(level,pos,level.getBlockState(pos),net.minecraft.util.RandomSource.create(12));
        var mature=level.getBlockState(pos);
        h.assertTrue(mature.getValue(net.minecraft.world.level.block.SaplingBlock.STAGE)==1 && mature.getShape(level,pos).bounds().maxY>early.maxY,"Sapling growth stage/outline not updated");
        sapling.advanceTree(level,pos,mature,net.minecraft.util.RandomSource.create(12));
        h.assertTrue(level.getBlockState(pos).is(DreamContent.SHADOW_LOG.get()),"3D sapling no longer grows a tree");h.succeed();
    }
    private static AdvancedCondensationTableBlockEntity table(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(p,ModBlocks.ADVANCED_CONDENSATION_TABLE.get().defaultBlockState());
        return (AdvancedCondensationTableBlockEntity)h.getLevel().getBlockEntity(p);
    }
    private static FakePlayer player(GameTestHelper h) {
        var player=new FakePlayer(h.getLevel(),new GameProfile(java.util.UUID.randomUUID(),"condense-test"));
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5.5,2,6.5)));
        player.setData(ModAttachments.MANA,new ManaState(500,500));return player;
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void offhandStaffDiscountMatchesDebit(GameTestHelper h) {
        var table=table(h);var p=player(h);
        var basic=com.mcmagic.omnira.item.staff.StaffAssembly.basic();
        var staff=new com.mcmagic.omnira.item.staff.StaffAssembly(basic.shaft(),
                new ItemStack(ModItems.NETHERITE_REINFORCEMENT.get()),basic.tip(),java.util.List.of()).create();
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.STICK));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,staff);
        p.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(7);
        table.setItem(0,new ItemStack(ModItems.SPACETIME_KNOT.get()));
        var menu=new SimpleCondensationTableMenu(1,p.getInventory(),table);
        h.assertTrue(menu.manaCost()==283,"Offhand discount missing from workstation display");
        h.assertTrue(com.mcmagic.omnira.mana.ManaCosts.cost(p,5)==0,"Discount permits negative cost");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new com.mcmagic.omnira.item.staff.StaffAssembly(basic.shaft(),
                new ItemStack(ModItems.DIAMOND_REINFORCEMENT.get()),basic.tip(),java.util.List.of()).create());
        var discount=p.getAttribute(ModAttributes.COST_REDUCTION);
        discount.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                com.mcmagic.omnira.spell.CastAttributes.STAFF_DISCOUNT,5,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        h.assertTrue(menu.manaCost()==288,"Dual staves must use only the main-hand bonus");
        discount.removeModifier(com.mcmagic.omnira.spell.CastAttributes.STAFF_DISCOUNT);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.STICK));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        h.assertTrue(menu.manaCost()==293,"Unequipped staff still discounts costs");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,staff);
        h.assertTrue(menu.clickMenuButton(p,0),"Discounted conversion cannot start");
        h.runAfterDelay(200,()->{
            menu.broadcastChanges();
            h.assertTrue(p.getData(ModAttachments.MANA).current()==217 && table.getItem(0).is(ModItems.PARADOX_DUST.get()),"Displayed discount differs from actual debit");
            h.assertTrue(new ItemStack(ModItems.CRUISE_ORB.get()).getRarity()==net.minecraft.world.item.Rarity.UNCOMMON,"Cruise orb rarity must be yellow");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void timedConversionChargesOnce(GameTestHelper h) {
        var table=table(h);var p=player(h);var other=player(h);
        table.setItem(0,new ItemStack(ModItems.SPACETIME_KNOT.get()));
        var menu=new SimpleCondensationTableMenu(1,p.getInventory(),table);
        var second=new SimpleCondensationTableMenu(2,other.getInventory(),table);
        h.assertTrue(menu.manaCost()==300 && menu.clickMenuButton(p,0) && second.clickMenuButton(other,0),"Cannot start conversion");
        h.assertTrue(!menu.clickMenuButton(p,0),"Double start accepted");
        h.runAfterDelay(199,()->{
            menu.broadcastChanges();h.assertTrue(table.getItem(0).is(ModItems.SPACETIME_KNOT.get()) && p.getData(ModAttachments.MANA).current()==500,"Premature conversion or charge");
        });
        h.runAfterDelay(200,()->{
            menu.broadcastChanges();second.broadcastChanges();menu.broadcastChanges();
            h.assertTrue(table.getItem(0).is(ModItems.PARADOX_DUST.get()) && table.getItem(0).getCount()==1,"Wrong in-place output");
            h.assertTrue(p.getData(ModAttachments.MANA).current()==200 && other.getData(ModAttachments.MANA).current()==500,"Concurrent completion charged twice");
            h.assertTrue(!menu.isWorking() && !menu.canCondense(),"Output can be reconverted");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void ballConversionAndManaDiscount(GameTestHelper h) {
        var table=table(h);var p=player(h);p.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(10);
        var menu=new SimpleCondensationTableMenu(1,p.getInventory(),table);
        p.getInventory().setItem(9,new ItemStack(ModItems.CRYSTAL_BALL.get(),3));
        menu.quickMoveStack(p,1);
        h.assertTrue(table.getItem(0).getCount()==1 && p.getInventory().getItem(9).getCount()==2,"Input slot did not enforce single item");
        h.assertTrue(menu.manaCost()==290 && menu.clickMenuButton(p,0),"Discount not applied");
        h.runAfterDelay(200,()->{
            menu.broadcastChanges();h.assertTrue(table.getItem(0).is(ModItems.PURE_VESSEL.get()) && p.getData(ModAttachments.MANA).current()==210,"Ball conversion or debit failed");
            menu.quickMoveStack(p,0);h.assertTrue(table.isEmpty() && p.getInventory().countItem(ModItems.PURE_VESSEL.get())==1,"Output transfer failed");h.succeed();
        });
    }
    @GameTest(template="spell_arena",templateNamespace="omnira_condensation_recipes")
    public static void cancellationAndRecipeIsolation(GameTestHelper h) {
        var table=table(h);var p=player(h);var menu=new SimpleCondensationTableMenu(1,p.getInventory(),table);
        h.assertTrue(!menu.clickMenuButton(p,0),"Empty input accepted");
        table.setItem(0,new ItemStack(ModItems.SPACETIME_KNOT.get()));
        p.setData(ModAttachments.MANA,new ManaState(299,500));h.assertTrue(!menu.clickMenuButton(p,0),"Unaffordable work started");
        p.setData(ModAttachments.MANA,new ManaState(500,500));menu.clickMenuButton(p,0);
        table.setItem(0,new ItemStack(ModItems.CRYSTAL_BALL.get()));menu.broadcastChanges();
        h.assertTrue(!menu.isWorking() && p.getData(ModAttachments.MANA).current()==500,"Changed input was charged");
        menu.clickMenuButton(p,0);menu.removed(p);h.assertTrue(!menu.isWorking(),"Closing failed to cancel");
        h.assertTrue(!menu.slots.getFirst().mayPlace(new ItemStack(Items.STONE)),"Unrelated input accepted");
        var pos=table.getBlockPos();h.getLevel().setBlockAndUpdate(pos,ModBlocks.SIMPLE_CONDENSATION_TABLE.get().defaultBlockState());
        var simple=new SimpleCondensationTableMenu(2,p.getInventory(),(SimpleCondensationTableBlockEntity)h.getLevel().getBlockEntity(pos));
        h.assertTrue(simple.recipe()!=null && !simple.recipe().advanced() && !simple.slots.getFirst().mayPlace(new ItemStack(ModItems.CRYSTAL_BALL.get())),"Basic recipe contaminated");
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.CONDENSATION_TYPE.get());
        var conversions=java.util.Map.of(
                ModItems.SPACETIME_KNOT.get(),ModItems.PARADOX_DUST.get(),
                ModItems.CRYSTAL_BALL.get(),ModItems.PURE_VESSEL.get(),
                ModItems.SPIRITUAL_CRYSTAL.get(),ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),
                ModItems.RESONANCE_TERMINAL.get(),ModItems.ENHANCED_RESONANCE_TERMINAL.get());
        conversions.forEach((input,output)->h.assertTrue(recipes.stream().anyMatch(r->r.value().advanced()
                &&r.value().ingredient().test(new ItemStack(input))&&r.value().result().is(output)),"Required advanced conversion missing: "+input));
        h.assertTrue(recipes.stream().anyMatch(r->r.value().ingredient().test(new ItemStack(ModItems.GUIDE_BOOK.get()))
                && r.value().result().is(ModItems.INFUSED_GRIMOIRE.get())),"Guidebook conversion missing");
        h.assertTrue(recipes.stream().filter(r->r.value().advanced()).allMatch(r->r.value().manaCost()==300 && !r.value().matches(new SingleRecipeInput(ItemStack.EMPTY),h.getLevel())),"Invalid advanced recipe data");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void vesselRestrictsPersistsAndDropsWeapon(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(pos,ModBlocks.PURE_VESSEL.get().defaultBlockState());
        var vessel=(PureVesselBlockEntity)h.getLevel().getBlockEntity(pos);var p=player(h);
        var handler=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.UP);
        h.assertTrue(handler==null && !((Object)vessel instanceof net.minecraft.world.Container),"Vessel still exposes storage");
        for(var item:java.util.List.of(Items.BOW,Items.CROSSBOW,Items.DIAMOND_PICKAXE,Items.STONE,ModItems.ARCANE_ARQUEBUS.get())) {
            var stack=new ItemStack(item);h.assertTrue(!vessel.activate(p,stack) && stack.getCount()==1,"Non-melee item accepted");
        }
        for(var item:java.util.List.of(Items.DIAMOND_SWORD,Items.IRON_AXE,Items.MACE,Items.TRIDENT))h.assertTrue(PureVesselBlockEntity.accepts(new ItemStack(item)),"Melee weapon rejected");
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(55);
        h.assertTrue(!vessel.activate(p,sword) && sword.getCount()==1 && vessel.getItem(0).isEmpty(),"Invalid ritual consumed weapon");
        vessel.setItem(0,sword.copy());
        var restored=new PureVesselBlockEntity(pos,vessel.getBlockState());restored.loadWithComponents(vessel.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(ItemStack.matches(vessel.getItem(0),restored.getItem(0)),"Weapon components failed to persist");
        var item=new ItemStack(ModItems.PURE_VESSEL.get());vessel.saveToItem(item,h.getLevel().registryAccess());
        h.assertTrue(!item.has(net.minecraft.core.component.DataComponents.CONTAINER),"Vessel item carries storage");
        h.getLevel().destroyBlock(pos,true);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.DIAMOND_SWORD)).mapToInt(e->e.getItem().getCount()).sum()==1,"Weapon missing or duplicated");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.PURE_VESSEL.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Vessel drop missing");
        h.assertTrue(!vessel.stillValid(p),"Broken vessel still valid");h.succeed();
    }
}
