package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_tool_repair") @PrefixGameTestTemplate(false)
public final class ToolRepairGameTests {
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h) {
        return new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RepairTest"));
    }
    private static GrindstoneMenu menu(GameTestHelper h,net.minecraft.world.entity.player.Player player) {
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));
        h.getLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.GRINDSTONE.defaultBlockState());
        return new GrindstoneMenu(1,player.getInventory(),ContainerLevelAccess.create(h.getLevel(),pos));
    }
    private static java.util.List<Item> tools() {
        return BuiltInRegistries.ITEM.stream().filter(i->BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("omnira") && new ItemStack(i).isDamageableItem()).toList();
    }
    @GameTest(template="spell_arena") public static void allDurableItemsGrindstoneMerge(GameTestHelper h) {
        var player=player(h);
        h.assertTrue(tools().size()>=6,"Durable item audit is incomplete");
        for(var item:tools()) {
            var top=new ItemStack(item);var bottom=new ItemStack(item);int max=top.getMaxDamage();
            top.setDamageValue(max*3/4);bottom.setDamageValue(max*2/3);
            var menu=menu(h,player);menu.slots.get(0).set(top);menu.slots.get(1).set(bottom);
            var result=menu.slots.get(2).getItem();int damage=Math.max(0,top.getDamageValue()+bottom.getDamageValue()-max-max*5/100);
            h.assertTrue(result.is(item) && result.getCount()==1 && result.getDamageValue()==damage,"Grindstone durability mismatch: "+item);
            menu.clicked(2,0,ClickType.PICKUP,player);
            h.assertTrue(menu.slots.get(0).getItem().isEmpty() && menu.slots.get(1).getItem().isEmpty(),"Grindstone failed to consume tools: "+item);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void allDurableItemsDisenchantAndKeepCurse(GameTestHelper h) {
        var player=player(h);var ench=h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var ordinary=ench.getHolderOrThrow(Enchantments.UNBREAKING);var curse=ench.getHolderOrThrow(Enchantments.VANISHING_CURSE);
        for(var item:tools())for(int slot=0;slot<2;slot++) {
            var stack=new ItemStack(item);stack.setDamageValue(stack.getMaxDamage()/2);stack.enchant(ordinary,3);stack.enchant(curse,1);
            var menu=menu(h,player);menu.slots.get(slot).set(stack);
            var result=menu.slots.get(2).getItem();
            h.assertTrue(result.is(item) && result.getDamageValue()==stack.getDamageValue(),"Disenchant changed durability: "+item);
            h.assertTrue(result.getEnchantmentLevel(ordinary)==0 && result.getEnchantmentLevel(curse)==1,"Wrong enchantment removal: "+item);
            menu.clicked(2,0,ClickType.PICKUP,player);
            h.assertTrue(menu.slots.get(slot).getItem().isEmpty(),"Disenchant did not consume input: "+item);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void analysisRepairConsumesBothCrystals(GameTestHelper h) {
        for (int[] damages : new int[][] {{110,70,46},{0,0,0},{0,70,0}}) {
        var first=new ItemStack(ModItems.ANALYSIS_CRYSTAL.get());first.setDamageValue(damages[0]);
        var second=new ItemStack(ModItems.ANALYSIS_CRYSTAL.get());second.setDamageValue(damages[1]);
        var input=CraftingInput.of(2,2,java.util.List.of(first,ItemStack.EMPTY,ItemStack.EMPTY,second));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value();
        h.assertTrue(recipe instanceof RepairItemRecipe,"Wrong repair recipe");
        var result=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.is(ModItems.ANALYSIS_CRYSTAL.get()) && result.getDamageValue()==damages[2],"Incorrect combined crystal durability");
        h.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),"Repair returned catalyst crystals");
        var player=player(h);
        var grid=new TransientCraftingContainer(player.inventoryMenu,2,2);grid.setItem(0,first);grid.setItem(3,second);
        var output=new ResultContainer();output.setItem(0,result);
        var slot=new ResultSlot(player,grid,output,0,0,0);slot.onTake(player,slot.remove(1));
        h.assertTrue(grid.isEmpty(),"Real crafting pickup left crystals in grid");
        }
        h.succeed();
    }
}
