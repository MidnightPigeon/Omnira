package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
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
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void rebornLilyProducesOnePoint(GameTestHelper h){
        var table=table(h);var p=player(h);var menu=table.work;
        table.setItem(0,new ItemStack(com.mcmagic.omnira.mire.MireContent.REBORN_ITEM.get()));
        h.assertTrue(menu.start(p),"Lily condensation failed to start");
        h.runAfterDelay(200,()->{
            menu.tick();h.assertTrue(table.getItem(0).is(ModItems.TIME_WARP_POINT.get())&&table.getItem(0).getCount()==1&&p.getData(ModAttachments.MANA).current()==200,"Lily output or mana incorrect");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void temporalMoteConsumesEightAndPlaces(GameTestHelper h){
        var table=table(h);var p=player(h);var menu=table.work;
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.TIME_WARP_POINT.get(),10));com.mcmagic.omnira.workstation.CondensationChannel.shortClick(p,table);
        h.assertTrue(table.getItem(0).getCount()==8&&p.getMainHandItem().getCount()==2,"Eight-item input limit incorrect");
        table.setItem(0,new ItemStack(ModItems.TIME_WARP_POINT.get(),7));h.assertTrue(!menu.canStart(p),"Seven points accepted");
        table.setItem(0,new ItemStack(ModItems.TIME_WARP_POINT.get(),8));h.assertTrue(menu.start(p),"Eight-point condensation failed");
        h.runAfterDelay(200,()->{
            menu.tick();h.assertTrue(table.getItem(0).is(ModItems.TEMPORAL_MOTE.get())&&table.getItem(0).getCount()==1,"Wrong mote result");
            h.assertTrue(p.getData(ModAttachments.MANA).current()==200,"Wrong condensation mana debit");
            var ground=table.getBlockPos().east(2);h.getLevel().setBlockAndUpdate(ground,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,table.removeItem(0,1));
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(ground),Direction.UP,ground,false);
            var result=ModItems.TEMPORAL_MOTE.get().useOn(new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            h.assertTrue(result.consumesAction()&&h.getLevel().getBlockState(ground.above()).is(ModBlocks.TEMPORAL_MOTE.get()),"Mote item failed placement");h.succeed();
        });
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
        var menu=table.work;
        h.assertTrue(com.mcmagic.omnira.mana.ManaCosts.cost(p,menu.recipe().manaCost())==283,"Offhand discount missing from workstation display");
        h.assertTrue(com.mcmagic.omnira.mana.ManaCosts.cost(p,5)==0,"Discount permits negative cost");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new com.mcmagic.omnira.item.staff.StaffAssembly(basic.shaft(),
                new ItemStack(ModItems.DIAMOND_REINFORCEMENT.get()),basic.tip(),java.util.List.of()).create());
        var discount=p.getAttribute(ModAttributes.COST_REDUCTION);
        discount.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                com.mcmagic.omnira.spell.CastAttributes.STAFF_DISCOUNT,5,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        h.assertTrue(com.mcmagic.omnira.mana.ManaCosts.cost(p,menu.recipe().manaCost())==288,"Dual staves must use only the main-hand bonus");
        discount.removeModifier(com.mcmagic.omnira.spell.CastAttributes.STAFF_DISCOUNT);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.STICK));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        h.assertTrue(com.mcmagic.omnira.mana.ManaCosts.cost(p,menu.recipe().manaCost())==293,"Unequipped staff still discounts costs");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,staff);
        h.assertTrue(menu.start(p),"Discounted conversion cannot start");
        h.runAfterDelay(200,()->{
            menu.tick();
            h.assertTrue(p.getData(ModAttachments.MANA).current()==217 && table.getItem(0).is(ModItems.PARADOX_DUST.get()),"Displayed discount differs from actual debit");
            h.assertTrue(new ItemStack(ModItems.CRUISE_ORB.get()).getRarity()==net.minecraft.world.item.Rarity.UNCOMMON,"Cruise orb rarity must be yellow");
            h.succeed();
        });
    }
}
