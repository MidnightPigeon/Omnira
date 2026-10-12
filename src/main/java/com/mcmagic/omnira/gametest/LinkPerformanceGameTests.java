package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.storage.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_link_performance")
@PrefixGameTestTemplate(false)
public final class LinkPerformanceGameTests {
    private static CrystalBallBlockEntity orb(GameTestHelper h,int x){
        var pos=new BlockPos(x,3,4);h.setBlock(pos,CrystalStorageContent.ITEM_ORB.get());
        return (CrystalBallBlockEntity)h.getBlockEntity(pos);
    }
    @GameTest(template="spell_arena") public static void stableRegistrationDoesNotRebuild(GameTestHelper h){
        var a=orb(h,3);var b=orb(h,6);var marker=new ItemStack(Items.PAPER);
        marker.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
        a.setFrequency(0,marker);b.setFrequency(0,marker);
        h.runAfterDelay(2,()->{
            // Other save/reload tests replace the world's ledger during their initial tick.
            var networks=CrystalStorageNetworks.get(h.getLevel());networks.register(a);networks.register(b);
            var storage=a.storage();
            h.runAfterDelay(4,()->{
                networks.setDirty(false);
                for(int i=0;i<100;i++){networks.register(a);networks.register(b);}
                h.assertTrue(!networks.isDirty()&&a.storage()==storage&&b.storage()==storage,"Unchanged registrations rebuilt the network");
                h.assertTrue(StorageContainerIdentity.terminalKey(a.getLevel(),a.getBlockPos()).equals(StorageContainerIdentity.terminalKey(b.getLevel(),b.getBlockPos())),"Indexed locations lost shared identity");
                a.setItem(0,new ItemStack(Items.DIAMOND,3));
                h.assertTrue(b.getItem(0).getCount()==3&&networks.isDirty(),"Content change was not persisted or shared");
                a.detachNetwork();b.detachNetwork();h.succeed();
            });
        });
    }
    @GameTest(template="spell_arena") public static void bridgeChangesInvalidateTopology(GameTestHelper h){
        var a=orb(h,3);var other=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        var pos=h.absolutePos(new BlockPos(6,3,4)).atY(80);other.getChunkAt(pos);
        var previous=other.getBlockState(pos);other.setBlockAndUpdate(pos,CrystalStorageContent.ITEM_ORB.get().defaultBlockState());
        var b=(CrystalBallBlockEntity)other.getBlockEntity(pos);
        try{
            var marker=new ItemStack(Items.PAPER);marker.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
            a.setFrequency(0,marker);b.setFrequency(0,marker);
            a.setItem(0,new ItemStack(Items.DIAMOND,3));b.setItem(0,new ItemStack(Items.DIAMOND,2));
            a.upgrades.setStackInSlot(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
            var networks=CrystalStorageNetworks.get(h.getLevel());networks.register(a);
            h.assertTrue(a.storage()==b.storage()&&b.getItem(0).getCount()==5,"Installing bridge failed to invalidate topology");
            b.upgrades.extractItem(0,1,false);networks.register(b);
            h.assertTrue(a.storage()!=b.storage()&&a.getItem(0).getCount()+b.getItem(0).getCount()==5,"Removing bridge failed to split without duplication");
        }finally{a.detachNetwork();b.detachNetwork();other.setBlockAndUpdate(pos,previous);}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void contentsNotifyOnlyTheirChannel(GameTestHelper h) throws Exception{
        var a=orb(h,3);var b=orb(h,6);var c=orb(h,9);
        var marker=new ItemStack(Items.PAPER);marker.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
        a.setFrequency(0,marker);b.setFrequency(0,marker);
        var independent=marker.copy();independent.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(java.util.UUID.randomUUID().toString()));
        c.setFrequency(0,independent);
        var networks=CrystalStorageNetworks.get(h.getLevel());
        var field=CrystalStorageNetworks.class.getDeclaredField("views");field.setAccessible(true);
        var views=(java.util.Map<?,?>)field.get(networks);
        try{
            a.setItem(0,new ItemStack(Items.DIAMOND));
            h.assertTrue(a.storage()==b.storage()&&a.storage()!=c.storage(),"Independent channels merged");
            h.assertTrue(((java.util.List<?>)views.get(a.storage())).size()==2
                &&((java.util.List<?>)views.get(c.storage())).size()==1,"Notification index includes unrelated channels");
            var saved=networks.save(new CompoundTag(),h.getLevel().registryAccess());
            h.getLevel().getServer().overworld().getDataStorage().set("omnira_crystal_storage",CrystalStorageNetworks.load(saved,h.getLevel().registryAccess()));
            var reloaded=CrystalStorageNetworks.get(h.getLevel());
            a.setItem(0,new ItemStack(Items.DIAMOND,2));
            var loadedViews=(java.util.Map<?,?>)field.get(reloaded);
            h.assertTrue(((java.util.List<?>)loadedViews.get(a.storage())).size()==2&&b.getItem(0).getCount()==2,"Early access after reload lost channel notifications");
        }finally{a.detachNetwork();b.detachNetwork();c.detachNetwork();}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void filterCacheTracksEditsAndReload(GameTestHelper h) throws Exception{
        var a=orb(h,3);a.upgrades.setStackInSlot(0,new ItemStack(ModItems.INTAKE_UPGRADE.get()));
        a.upgrades.filter(0,0,new ItemStack(Items.DIAMOND));
        var tag=OrbUpgrades.settings(a.upgrades.getStackInSlot(0));tag.putBoolean("Whitelist",true);a.upgrades.update(0,tag);
        var field=OrbUpgrades.class.getDeclaredField("filters");field.setAccessible(true);
        h.assertTrue(a.upgrades.matches(0,new ItemStack(Items.DIAMOND),null),"Initial whitelist failed");
        var cache=((Object[])field.get(a.upgrades))[0];
        for(int i=0;i<100;i++)a.upgrades.matches(0,new ItemStack(Items.STONE),null);
        h.assertTrue(((Object[])field.get(a.upgrades))[0]==cache,"Unchanged filters were decoded again");
        a.upgrades.filter(0,0,new ItemStack(Items.STONE));
        h.assertTrue(!a.upgrades.matches(0,new ItemStack(Items.DIAMOND),null)&&a.upgrades.matches(0,new ItemStack(Items.STONE),null),"Edited filter retained old cache");
        a.upgrades.deserializeNBT(h.getLevel().registryAccess(),new CompoundTag());
        h.assertTrue(a.upgrades.matches(0,new ItemStack(Items.DIAMOND),null),"Reload retained stale whitelist");h.succeed();
    }
}
