package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.AnalysisArtisanTableBlockEntity;
import com.mcmagic.omnira.menu.AnalysisArtisanTableMenu;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.recipe.AnalysisRecipe;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_element_extraction")
@PrefixGameTestTemplate(false)
public final class ElementExtractionGameTests {
    private record Station(net.neoforged.neoforge.common.util.FakePlayer player,AnalysisArtisanTableBlockEntity table,AnalysisArtisanTableMenu menu) {}
    private static Station station(GameTestHelper h) {
        var pos=new BlockPos(4,2,4);h.setBlock(pos,ModBlocks.ANALYSIS_ARTISAN_TABLE.get());
        var table=(AnalysisArtisanTableBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"extraction-test"));
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(4.5,2,6.5)));player.setData(ModAttachments.MANA,ManaState.initial());
        return new Station(player,table,new AnalysisArtisanTableMenu(1,player.getInventory(),table));
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void fourElementExtractions(GameTestHelper h) {
        var s=station(h);
        Item[] inputs={Items.IRON_SWORD,Items.GLISTERING_MELON_SLICE,ModItems.ANALYSIS_CRYSTAL.get(),ModItems.CRYSTAL_CASING.get()};
        int[] counts={1,16,1,8};
        Item[] outputs={ModItems.SHARP_BREATH.get(),ModItems.HEALING_DEW.get(),ModItems.DISSOCIATION_THREAD.get(),ModItems.CONSTRUCTION_MATRIX.get()};
        ItemStack[] original=new ItemStack[4];
        for(int n=0;n<4;n++) {
            final int i=n;
            h.runAtTickTime(1+n*50,()->{
                s.table.clearContent();s.player.setData(ModAttachments.MANA,ManaState.initial());
                var stack=new ItemStack(inputs[i],counts[i]);if(i==2) stack.setDamageValue(7);
                s.table.setItem(0,stack);original[i]=s.table.getItem(0);
                h.assertTrue(s.menu.clickMenuButton(s.player,0),"Extraction did not start: "+i);
                h.assertTrue(original[i].getCount()==counts[i] && s.player.getData(ModAttachments.MANA).current()==100,"Charged before completion");
            });
            h.runAtTickTime(43+n*50,()->{
                s.menu.broadcastChanges();
                h.assertTrue(s.table.getItem(1).is(outputs[i]) && s.table.getItem(1).getCount()==1,"Wrong element: "+i);
                var bonus=s.table.getItem(2);
                if(i==0) h.assertTrue(bonus.is(Items.STICK) && bonus.getCount()==1,"Wrong sword byproduct");
                if(i==1) h.assertTrue(bonus.is(Items.MELON_SEEDS) && bonus.getCount()>=3 && bonus.getCount()<=5,"Wrong seeds");
                if(i==2) {
                    h.assertTrue(s.table.getItem(0)==original[i] && original[i].getDamageValue()==8,"Crystal was replaced instead of damaged in place");
                    h.assertTrue(java.util.Set.of(ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.FIRE_MICROCORE.get(),ModItems.AIR_MICROCORE.get()).contains(bonus.getItem()) && bonus.getCount()==1,"Invalid microcore");
                } else h.assertTrue(s.table.getItem(0).isEmpty(),"Input not consumed");
                if(i==3) h.assertTrue(bonus.is(ModItems.SPIRITUAL_CRYSTAL.get()) && bonus.getCount()==8,"Wrong casing byproduct");
                h.assertTrue(s.player.getData(ModAttachments.MANA).current()==50,"Wrong mana debit");
            });
        }
        h.runAtTickTime(200,h::succeed);
    }
    @GameTest(template="spell_arena")
    public static void blockedOutputAndLastDurability(GameTestHelper h) {
        var s=station(h);var crystal=new ItemStack(ModItems.ANALYSIS_CRYSTAL.get());crystal.setDamageValue(127);s.table.setItem(0,crystal);
        s.table.setItem(2,new ItemStack(ModItems.EARTH_MICROCORE.get()));
        h.assertTrue(!s.menu.clickMenuButton(s.player,0) && crystal.getDamageValue()==127 && s.player.getData(ModAttachments.MANA).current()==100,"Blocked random output charged resources");
        s.table.setItem(2,ItemStack.EMPTY);
        h.assertTrue(s.menu.clickMenuButton(s.player,0),"Final durability cannot be used");
        h.runAtTickTime(43,()->{
            s.menu.broadcastChanges();
            h.assertTrue(s.table.getItem(0).isEmpty() && s.table.getItem(1).is(ModItems.DISSOCIATION_THREAD.get()) && s.table.getItem(2).getCount()==1,"Last use did not break in input slot");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena")
    public static void recipeCodecsAndRandomPool(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        for(String id:new String[]{"sharp_breath","healing_dew","dissociation_thread","construction_matrix","earth","fire"}) {
            var recipe=(AnalysisRecipe)manager.byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","analysis/"+id)).orElseThrow().value();
            var ops=h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var json=AnalysisRecipe.CODEC.codec().encodeStart(ops,recipe).getOrThrow();
            var decoded=AnalysisRecipe.CODEC.codec().parse(ops,json).getOrThrow();
            h.assertTrue(decoded.inputDamage()==recipe.inputDamage() && decoded.secondaryOutput().max()==recipe.secondaryOutput().max(),"JSON roundtrip failed");
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
            try {
                AnalysisRecipe.STREAM_CODEC.encode(buffer,recipe);var network=AnalysisRecipe.STREAM_CODEC.decode(buffer);
                h.assertTrue(network.inputDamage()==recipe.inputDamage() && network.secondaryOutput().choices().size()==recipe.secondaryOutput().choices().size(),"Network roundtrip failed");
            } finally {buffer.release();}
            if(id.equals("dissociation_thread")) {
                var seen=new java.util.HashSet<Item>();var random=net.minecraft.util.RandomSource.create(5829);
                for(int i=0;i<400;i++) {var roll=recipe.secondaryOutput().roll(random);h.assertTrue(roll.getCount()==1,"Multiple random cores produced");seen.add(roll.getItem());}
                h.assertTrue(seen.equals(java.util.Set.of(ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.FIRE_MICROCORE.get(),ModItems.AIR_MICROCORE.get())),"Wrong random pool");
            }
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=240)
    public static void legacyBucketsAndDust(GameTestHelper h) {AnalysisGameTests.fourRecipesAndMana(h);}
}
