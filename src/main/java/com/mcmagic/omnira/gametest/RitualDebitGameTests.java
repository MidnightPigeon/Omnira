package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.world.dimension.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_ritual_debits")
@PrefixGameTestTemplate(false)
public final class RitualDebitGameTests {
    private static BlockPos setup(GameTestHelper h,Item item) {
        var core=h.absolutePos(new BlockPos(6,3,6));h.getLevel().setBlockAndUpdate(core,ModBlocks.RITUAL_ENERGY_CORE.get().defaultBlockState());
        for(var offset:DreamRitual.ANCHORS) {
            var pos=core.offset(offset[0],0,offset[1]);h.getLevel().setBlockAndUpdate(pos,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
            ((CrystalPedestalBlockEntity)h.getLevel().getBlockEntity(pos)).setItem(0,new ItemStack(item));
        }
        return core;
    }
    private static CrystalPedestalBlockEntity pedestal(GameTestHelper h,BlockPos core,int i) {
        var a=DreamRitual.ANCHORS[i];return (CrystalPedestalBlockEntity)h.getLevel().getBlockEntity(core.offset(a[0],0,a[1]));
    }
    @GameTest(template="spell_arena") public static void resonanceConsumesExactlyOnePerPedestal(GameTestHelper h) {
        var core=setup(h,ModItems.RESONANCE_CORE.get());var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.SPACETIME_KNOT.get(),2));
        h.assertTrue(ResonanceRitual.activate(h.getLevel(),core,player,InteractionHand.MAIN_HAND),"Resonance did not activate");
        for(int i=0;i<8;i++)h.assertTrue(pedestal(h,core,i).getItem(0).isEmpty(),"Wrong per-pedestal debit");
        h.assertTrue(player.getMainHandItem().getCount()==1&&h.getLevel().getBlockState(core).is(ModBlocks.RITUAL_ENERGY_CORE.get()),"Activator or core debit wrong");h.succeed();
    }
    @GameTest(template="spell_arena") public static void invalidResonanceDoesNotPartiallyConsume(GameTestHelper h) {
        var core=setup(h,ModItems.RESONANCE_CORE.get());var player=h.makeMockPlayer(GameType.SURVIVAL);
        pedestal(h,core,7).setItem(0,ItemStack.EMPTY);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.SPACETIME_KNOT.get(),2));
        h.assertTrue(!ResonanceRitual.activate(h.getLevel(),core,player,InteractionHand.MAIN_HAND),"Invalid resonance activated");
        for(int i=0;i<7;i++)h.assertTrue(pedestal(h,core,i).getItem(0).getCount()==1,"Partially consumed invalid resonance");
        h.assertTrue(player.getMainHandItem().getCount()==2,"Invalid resonance consumed activator");h.succeed();
    }
    @GameTest(template="spell_arena") public static void cleansingWithoutTargetsKeepsEveryMaterial(GameTestHelper h) {
        var core=setup(h,ModItems.INFUSED_SPIRITUAL_CRYSTAL.get());var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.PARADOX_DUST.get(),2));
        h.assertTrue(!CleansingRitual.activate(h.getLevel(),core,player,InteractionHand.MAIN_HAND),"No-target cleansing activated");
        for(int i=0;i<8;i++)h.assertTrue(pedestal(h,core,i).getItem(0).getCount()==1,"No-target cleansing consumed a crystal");
        h.assertTrue(player.getMainHandItem().getCount()==2,"No-target cleansing consumed dust");h.succeed();
    }
    private RitualDebitGameTests() {}
}
