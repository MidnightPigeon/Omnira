package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.item.SpellCoreItem;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_placed_core")
@PrefixGameTestTemplate(false)
public final class PlacedSpellCoreGameTests {
    @GameTest(template="spell_arena") public static void sneakPickup(GameTestHelper h) {
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);
        var p=h.absolutePos(new BlockPos(4,2,4));
        level.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
        for(var item:java.util.List.of(ModItems.TEST_SPELL_CORE.get(),ModItems.DREAM_SPELL_CORE.get(),ModItems.LIGHT_DARK_SPELL_CORE.get(),ModItems.SPACETIME_SPELL_CORE.get())) {
            var state=((BlockItem)item).getBlock().defaultBlockState();
            level.setBlockAndUpdate(p,state);
            var hit=new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false);
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            state.useWithoutItem(level,player,hit);
            h.assertTrue(level.getBlockState(p).is(state.getBlock()),"Non-sneaking interaction picked up core");
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
            state.useWithoutItem(level,player,hit);
            h.assertTrue(level.getBlockState(p).is(state.getBlock()),"Occupied main hand picked up core");
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STICK));
            var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,p,hit);
            com.mcmagic.omnira.block.SpellCoreBlock.allowEmptyMainHandPickup(event);
            h.assertTrue(event.getUseBlock()==net.neoforged.neoforge.common.util.TriState.TRUE,"Offhand blocks pickup");
            event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
            com.mcmagic.omnira.block.SpellCoreBlock.allowEmptyMainHandPickup(event);
            h.assertTrue(event.getUseBlock()==net.neoforged.neoforge.common.util.TriState.FALSE,"Pickup bypasses protection");
            h.assertTrue(state.useWithoutItem(level,player,hit).consumesAction(),"Pickup did not consume interaction");
            h.assertTrue(level.getBlockState(p).isAir(),"Picked-up core remains placed");
            h.assertTrue(player.getMainHandItem().is(item) && player.getMainHandItem().getCount()==1,"Wrong pickup item");
            h.assertTrue(player.getOffhandItem().is(Items.STICK),"Pickup changed offhand");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void placeDropAndCurioAttributes(GameTestHelper h) {
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);
        int index=0;
        for(var item:java.util.List.of(ModItems.TEST_SPELL_CORE.get(),ModItems.DREAM_SPELL_CORE.get(),ModItems.LIGHT_DARK_SPELL_CORE.get(),ModItems.SPACETIME_SPELL_CORE.get())) {
            var p=h.absolutePos(new BlockPos(4+index*3,2,4));level.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
            var stack=new ItemStack(item);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var hit=new BlockHitResult(Vec3.atBottomCenterOf(p),Direction.UP,p.below(),false);
            h.assertTrue(item.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Right click did not place core");
            h.assertTrue(stack.isEmpty(),"Survival placement did not consume item");
            var state=level.getBlockState(p);
            h.assertTrue(state.getBlock().asItem()==item,"Placed wrong core");
            h.assertTrue(level.getBlockEntity(p) instanceof com.mcmagic.omnira.block.entity.SpellCoreBlockEntity,"Missing core renderer entity");
            h.assertTrue(state.getShape(level,p).bounds().getXsize()<1,"Core has full-cube shape");
            var drops=Block.getDrops(state,level,p,level.getBlockEntity(p));
            h.assertTrue(drops.size()==1 && drops.getFirst().is(item) && drops.getFirst().getCount()==1,"Wrong core drop");
            var mods=((SpellCoreItem)item).getAttributeModifiers(new top.theillusivec4.curios.api.SlotContext("spell_core",player,0,false,true),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","placed_core_test"),new ItemStack(item));
            h.assertTrue(mods.get(ModAttributes.MAX_MANA).iterator().next().amount()>0,"Placed core lost mana capacity");
            var power=mods.get(ModAttributes.SPELL_POWER).iterator().next();
            h.assertTrue(power.amount()>0 && power.operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Placed core lost additive spell power");
            level.destroyBlock(p.below(),false);
            h.assertTrue(level.getBlockState(p).isAir(),"Core remains after support removed");
            index++;
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void lightDarkAttributes(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var context=new top.theillusivec4.curios.api.SlotContext("spell_core",player,0,false,true);
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","light_dark_core_test");
        var core=(SpellCoreItem)ModItems.LIGHT_DARK_SPELL_CORE.get();
        var stack=new ItemStack(core);
        var actual=core.getAttributeModifiers(context,id,stack);
        for(var attribute:java.util.List.of(ModAttributes.MAX_MANA,ModAttributes.SPELL_POWER,ModAttributes.MANA_REGEN)) {
            h.assertTrue(actual.get(attribute).size()==1,"Missing or duplicated light/dark attribute");
            var modifier=actual.get(attribute).iterator().next();
            var operation=attribute.equals(ModAttributes.MAX_MANA)
                    ?net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    :net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            h.assertTrue(modifier.amount()>0 && modifier.operation()==operation,"Light/dark attribute has wrong operation");
        }
        h.assertTrue(actual.size()==3 && actual.get(ModAttributes.COST_REDUCTION).isEmpty(),"Light/dark reduces spell cost");
        h.assertTrue(stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("curios","spell_core"))),"Missing spell-core Curios tag");
        var other=new top.theillusivec4.curios.api.SlotContext("ring",player,0,false,true);
        h.assertTrue(core.canEquip(context,stack) && !core.canEquip(other,stack)
                && core.getAttributeModifiers(other,id,stack).isEmpty(),"Core grants bonuses in wrong slot");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void spacetimeAttributes(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var context=new top.theillusivec4.curios.api.SlotContext("spell_core",player,0,false,true);
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","spacetime_core_test");
        var core=(SpellCoreItem)ModItems.SPACETIME_SPELL_CORE.get();
        var modifiers=core.getAttributeModifiers(context,id,new ItemStack(core));
        for(var attribute:java.util.List.of(ModAttributes.MAX_MANA,ModAttributes.SPELL_POWER,ModAttributes.MANA_REGEN,ModAttributes.COST_REDUCTION))
            h.assertTrue(modifiers.get(attribute).size()==1 && modifiers.get(attribute).iterator().next().amount()>0,
                    "Spacetime core is missing an equipped bonus");
        h.assertTrue(modifiers.get(ModAttributes.SPELL_POWER).iterator().next().operation()
                ==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "Spacetime spell power must add against the base value");
        h.assertTrue(core.engineOutput(new ItemStack(core)).stress()>0
                && core.engineOutput(new ItemStack(core)).fePerSecond()>0,"Spacetime core cannot power an engine");
        h.succeed();
    }
}
