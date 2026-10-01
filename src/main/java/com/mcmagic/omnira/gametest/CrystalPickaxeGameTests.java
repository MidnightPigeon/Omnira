package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.item.CrystalPickaxeItem;
import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.event.CrystalPickaxeEvents;
import com.mcmagic.omnira.mana.ManaState;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import java.util.*;

@GameTestHolder("omnira_crystal_pickaxe")
@PrefixGameTestTemplate(false)
public final class CrystalPickaxeGameTests {
    @GameTest(template="spell_arena")
    public static void tiersRepairAndDrops(GameTestHelper h) {
        var basic=new ItemStack(ModItems.CRYSTAL_PICKAXE.get());var infused=new ItemStack(ModItems.INFUSED_CRYSTAL_PICKAXE.get());
        h.assertTrue(basic.getMaxDamage()==64 && infused.getMaxDamage()==1024,"Incorrect durability");
        h.assertTrue(basic.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()) && !basic.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),"Wrong iron tier");
        h.assertTrue(infused.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),"Wrong infused tier");
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pickaxe-test"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(100);
        player.setItemInHand(InteractionHand.OFF_HAND,infused);infused.setDamageValue(3);
        player.setData(ModAttachments.MANA,new ManaState(6,100));
        h.assertTrue(CrystalPickaxeItem.repairHeld(infused,player),"Offhand repair failed");
        h.assertTrue(infused.getDamageValue()==2 && player.getData(ModAttachments.MANA).current()==1,"Repair did not charge fixed five mana");
        h.assertTrue(!CrystalPickaxeItem.repairHeld(infused,player),"Repair ignored insufficient mana");
        var unheld=infused.copy();player.setData(ModAttachments.MANA,ManaState.initial());
        h.assertTrue(!CrystalPickaxeItem.repairHeld(unheld,player),"Unheld tool repaired");
        h.getLevel().random.setSeed(71234);
        int bonus=0;
        for(int i=0;i<200;i++) {
            var drops=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
            var event=new net.neoforged.neoforge.event.level.BlockDropsEvent(h.getLevel(),h.absolutePos(new net.minecraft.core.BlockPos(3,3,3)),Blocks.DIAMOND_ORE.defaultBlockState(),null,drops,player,infused);
            CrystalPickaxeEvents.oreDrops(event);
            h.assertTrue(drops.size()<=1,"More than one bonus crystal");bonus+=drops.size();
        }
        h.assertTrue(bonus>15 && bonus<70,"Ore bonus probability outside expected range");
        var drops=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        CrystalPickaxeEvents.oreDrops(new net.neoforged.neoforge.event.level.BlockDropsEvent(h.getLevel(),h.absolutePos(new net.minecraft.core.BlockPos(3,3,3)),Blocks.STONE.defaultBlockState(),null,drops,player,infused));
        h.assertTrue(drops.isEmpty(),"Non-ore yielded a crystal");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void outerRecipePermutations(GameTestHelper h) {
        var recipe=(AssemblyRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira","assembly/infused_crystal_pickaxe")).orElseThrow().value();
        for(int a=0;a<6;a++) for(int b=0;b<6;b++) for(int c=0;c<6;c++) {
            if(a==b || a==c || b==c) continue;
            var input=new SimpleContainer(7);input.setItem(a,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
            input.setItem(b,new ItemStack(Items.IRON_INGOT));input.setItem(c,new ItemStack(Items.DIAMOND));
            input.setItem(6,new ItemStack(ModItems.CRYSTAL_PICKAXE.get()));
            h.assertTrue(recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Outer ordering mattered");
            input.setItem(a,ItemStack.EMPTY);
            h.assertTrue(!recipe.matches(new AssemblyRecipe.Input(input),h.getLevel()),"Recipe accepted missing crystal");
        }
        h.succeed();
    }
}
