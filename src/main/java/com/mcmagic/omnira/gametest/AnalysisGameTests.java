package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.CrystalProcessingTableBlock;
import com.mcmagic.omnira.block.entity.AnalysisArtisanTableBlockEntity;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.menu.AnalysisArtisanTableMenu;
import com.mcmagic.omnira.registry.*;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira")
@PrefixGameTestTemplate(false)
public final class AnalysisGameTests {
    @GameTest(template="spell_arena")
    public static void crystalWritingManaIsAtomic(GameTestHelper h) {
        var s = station(h);
        h.setBlock(4,2,4,ModBlocks.CRYSTAL_PROCESSING_TABLE.get());
        var table = (com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity)
                h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4,2,4)));
        var menu = new com.mcmagic.omnira.menu.CrystalProcessingTableMenu(2,s.player.getInventory(),table,table);
        table.setItem(0,new ItemStack(ModItems.EARTH_MICROCORE.get(),2));
        table.setItem(1,new ItemStack(ModItems.WATER_MICROCORE.get(),2));
        table.setItem(6,new ItemStack(Items.INK_SAC,2));
        table.setItem(7,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get(),2));
        s.player.setData(ModAttachments.MANA,new ManaState(19,100));
        h.assertTrue(!menu.clickMenuButton(s.player,0),"Insufficient writing mana accepted");
        h.assertTrue(s.player.getData(ModAttachments.MANA).current()==19 && table.getItem(0).getCount()==2
                && table.getItem(6).getCount()==2 && table.getItem(7).getCount()==2,"Failed writing consumed resources");
        s.player.setData(ModAttachments.MANA,new ManaState(20,100));
        h.assertTrue(menu.clickMenuButton(s.player,0),"Writing with exactly 20 mana failed");
        h.assertTrue(s.player.getData(ModAttachments.MANA).current()==20,"Writing charged before completion");
        h.runAtTickTime(65,() -> {
        menu.broadcastChanges();
        h.assertTrue(s.player.getData(ModAttachments.MANA).current()==0,"Writing must cost exactly 20 mana");
        h.assertTrue(table.getItem(8).is(ModItems.LOW_TIER_MAGIC_CRYSTAL.get())
                && table.getItem(8).has(ModDataComponents.SPELL_PATTERN.get()),"Written crystal has no pattern");
        h.assertTrue(table.getItem(0).getCount()==1 && table.getItem(1).getCount()==1
                && table.getItem(6).getCount()==1 && table.getItem(7).getCount()==1,"Wrong writing ingredient consumption");
        s.player.setData(ModAttachments.MANA,ManaState.initial());
        h.assertTrue(!menu.clickMenuButton(s.player,0) && s.player.getData(ModAttachments.MANA).current()==100,
                "Occupied output consumed mana");
        h.assertTrue(ManaState.initial().color()==0xFFFFFF,"Neutral mana must be white");
        h.succeed();
        });
    }
    private record Station(FakePlayer player, AnalysisArtisanTableBlockEntity table, AnalysisArtisanTableMenu menu) {}
    private static Station station(GameTestHelper h) {
        h.setBlock(4,2,4,ModBlocks.ANALYSIS_ARTISAN_TABLE.get());
        var table = (AnalysisArtisanTableBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4,2,4)));
        var player = new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"analysis-test"));
        player.setPos(h.absoluteVec(new Vec3(4.5,2,6.5)));
        player.setData(ModAttachments.MANA,ManaState.initial());
        return new Station(player,table,new AnalysisArtisanTableMenu(1,player.getInventory(),table));
    }
    @GameTest(template="spell_arena", timeoutTicks=240)
    public static void fourRecipesAndMana(GameTestHelper h) {
        var s = station(h);
        Item[] inputs = {Items.DIRT,Items.LAVA_BUCKET,Items.COD,Items.FEATHER};
        Item[] outputs = {ModItems.EARTH_MICROCORE.get(),ModItems.FIRE_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.AIR_MICROCORE.get()};
        for(int index=0;index<4;index++) {
            final int i=index;
            h.runAtTickTime(1+i*50,() -> {
            s.table.clearContent();
            s.player.setData(ModAttachments.MANA,ManaState.initial());
            s.table.setItem(0,new ItemStack(inputs[i],i==1?1:64));
            h.assertTrue(s.menu.clickMenuButton(s.player,0),"Analysis failed for "+inputs[i]);
            });
            h.runAtTickTime(43+i*50,() -> {
            s.menu.broadcastChanges();
            h.assertTrue(s.table.getItem(1).is(outputs[i]) && s.table.getItem(1).getCount()==1,"Wrong microcore");
            h.assertTrue(s.player.getData(ModAttachments.MANA).current()==50,"Analysis must cost exactly 50 mana");
            if(i==0 || i==2) h.assertTrue(s.table.getItem(2).getCount()>=3 && s.table.getItem(2).getCount()<=5,"Dust out of range");
            else h.assertTrue(s.table.getItem(2).isEmpty(),"Unexpected dust");
            h.assertTrue(i==1?s.table.getItem(0).is(Items.BUCKET):s.table.getItem(0).isEmpty(),"Wrong input consumption or bucket remainder");
            });
        }
        h.runAtTickTime(200,h::succeed);
    }
    @GameTest(template="spell_arena")
    public static void failedAnalysisIsAtomic(GameTestHelper h) {
        var s = station(h);
        s.table.setItem(0,new ItemStack(Items.DIRT,63));
        h.assertTrue(!s.menu.clickMenuButton(s.player,0),"Partial stack accepted");
        h.assertTrue(s.table.getItem(0).getCount()==63 && s.player.getData(ModAttachments.MANA).current()==100,"Failed craft changed inputs/mana");
        s.table.setItem(0,new ItemStack(Items.DIRT,64));
        s.player.setData(ModAttachments.MANA,new ManaState(49,100));
        h.assertTrue(!s.menu.clickMenuButton(s.player,0) && s.table.getItem(0).getCount()==64,"Insufficient mana consumed material");
        s.player.setData(ModAttachments.MANA,ManaState.initial());
        s.table.setItem(2,new ItemStack(ModItems.ARCANE_DUST.get(),62));
        h.assertTrue(!s.menu.clickMenuButton(s.player,0),"Overflow was accepted");
        h.assertTrue(s.player.getData(ModAttachments.MANA).current()==100 && s.table.getItem(0).getCount()==64,"Overflow debited resources");
        s.table.setItem(2,ItemStack.EMPTY);
        s.player.setPos(s.player.position().add(20,0,0));
        h.assertTrue(!s.menu.clickMenuButton(s.player,0),"Out-of-range menu action accepted");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void tagsPersistenceAndFacing(GameTestHelper h) {
        var s = station(h);
        for(Item input : new Item[]{Items.COARSE_DIRT,Items.ROOTED_DIRT,Items.SALMON,Items.PUFFERFISH}) {
            s.table.setItem(0,new ItemStack(input,64));
            h.assertTrue(s.menu.recipe()!=null,"Tag member missing: "+input);
        }
        s.table.setItem(1,new ItemStack(ModItems.EARTH_MICROCORE.get(),2));
        var restored = new AnalysisArtisanTableBlockEntity(s.table.getBlockPos(),s.table.getBlockState());
        restored.loadWithComponents(s.table.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.getContainerSize()==3 && restored.getItem(1).getCount()==2,"Inventory persistence failed");
        for(var block : new net.minecraft.world.level.block.Block[]{ModBlocks.CRYSTAL_PROCESSING_TABLE.get(),ModBlocks.ANALYSIS_ARTISAN_TABLE.get()}) {
            for(int yaw=0;yaw<360;yaw+=90) {
                s.player.setYRot(yaw);
                var context = new net.minecraft.world.item.context.BlockPlaceContext(s.player,net.minecraft.world.InteractionHand.MAIN_HAND,
                        new ItemStack(block),new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(s.table.getBlockPos()),
                        net.minecraft.core.Direction.UP,s.table.getBlockPos(),false));
                // These models expose their output on the side opposite their FACING property.
                h.assertTrue(block.getStateForPlacement(context).getValue(CrystalProcessingTableBlock.FACING)==s.player.getDirection(),"Table output does not face placer");
            }
        }
        h.succeed();
    }
}
