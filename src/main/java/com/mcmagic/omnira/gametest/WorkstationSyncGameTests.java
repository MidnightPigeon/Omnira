package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.workstation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_workstation_sync") @PrefixGameTestTemplate(false)
public final class WorkstationSyncGameTests {
    @GameTest(template="spell_arena") public static void allSixIdleWorkstationsLeaveTheirChunkClean(GameTestHelper h){
        for(int index=0;index<6;index++){
            var pos=h.absolutePos(new BlockPos(4+index*2,4,4));
            h.getLevel().setBlockAndUpdate(pos,CrystalWorkstations.BLOCKS.get(index).get().defaultBlockState());
            var host=h.getLevel().getBlockEntity(pos);var work=CrystalWorkstations.work(host);
            var chunk=h.getLevel().getChunkAt(pos);chunk.setUnsaved(false);
            for(int tick=0;tick<250;tick++)work.tickDirect();
            h.assertTrue(!chunk.isUnsaved(),"Idle workstation dirtied/synchronized its chunk: "+index);
            h.assertTrue(work.progress==0&&work.energy==0&&work.burn==0,"Idle workstation changed working state");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void activeProgressAndFuelSaveBetweenWorldSyncs(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,4,5));h.getLevel().setBlockAndUpdate(pos,CrystalWorkstations.BLOCKS.get(0).get().defaultBlockState());
        var host=h.getLevel().getBlockEntity(pos);var work=CrystalWorkstations.work(host);
        work.setItem(0,new ItemStack(Items.COBBLESTONE,2));work.setItem(1,new ItemStack(Items.COAL));
        for(int tick=0;tick<37;tick++)work.tickDirect();
        var chunk=h.getLevel().getChunkAt(pos);chunk.setUnsaved(false);work.tickDirect();
        h.assertTrue(chunk.isUnsaved(),"Unbroadcast active progress was not persisted");
        var saved=host.saveWithoutMetadata(h.getLevel().registryAccess());
        var copy=new CrystalWorkstationEntity(pos,work.getBlockState());copy.setLevel(h.getLevel());copy.readWork(saved,h.getLevel().registryAccess());
        h.assertTrue(copy.progress==38&&copy.burn==work.burn&&copy.burnTotal==work.burnTotal,"Progress/fuel lost between broadcasts");
        for(int tick=38;tick<100;tick++)work.tickDirect();
        h.assertTrue(work.getItem(2).is(Items.STONE)&&work.getItem(0).getCount()==1,"Sync reduction changed smelting completion");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void damagedAnvilWithoutManaDoesNotBroadcastRepairs(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,4,5));h.getLevel().setBlockAndUpdate(pos,CrystalWorkstations.BLOCKS.get(2).get().defaultBlockState());
        var work=CrystalWorkstations.work(h.getLevel().getBlockEntity(pos));work.durability=15;work.repairMana=195;
        var chunk=h.getLevel().getChunkAt(pos);chunk.setUnsaved(false);
        for(int tick=0;tick<100;tick++)work.tickDirect();
        h.assertTrue(!chunk.isUnsaved()&&work.durability==15&&work.repairMana==195,"Failed mana absorption changed/synchronized anvil");h.succeed();
    }
}
