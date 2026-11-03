package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spacetime.AggregateStorage;
import com.mcmagic.omnira.reversal.WetlandRepair;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

@GameTestHolder("omnira_passive_loot") @PrefixGameTestTemplate(false)
public final class PassiveStorageLootGameTests {
    private static final class CountingInventory extends net.minecraft.world.SimpleContainer {
        int reads;
        CountingInventory(){super(27);}
        @Override public net.minecraft.world.item.ItemStack getItem(int slot){reads++;return super.getItem(slot);}
    }
    @GameTest(template="spell_arena") public static void repeatedViewsScanOnce(GameTestHelper h){
        var inventory=new CountingInventory();
        var pass=new com.mcmagic.omnira.storage.PassiveStorageInspection.Pass();
        if(pass.first(inventory))AggregateStorage.container(inventory,10);
        for(int side=0;side<7;side++){
            var view=com.mcmagic.omnira.item.bottle.BottleStorageHandler.wrap(new InvWrapper(inventory));
            if(pass.first(view))AggregateStorage.handler(view,10);
        }
        h.assertTrue(inventory.reads==54,"Repeated capability views rescanned the same 27-slot inventory: "+inventory.reads);
        inventory.reads=0;pass=new com.mcmagic.omnira.storage.PassiveStorageInspection.Pass();
        for(int view=0;view<8;view++){
            var wrapper=new InvWrapper(inventory);
            if(pass.first(wrapper))WetlandRepair.repair(wrapper,false);
        }
        h.assertTrue(inventory.reads==27,"Wetland maintenance rescanned aliases: "+inventory.reads);h.succeed();
    }
    @GameTest(template="spell_arena") public static void distinctViewsAndNextPassRemainVisible(GameTestHelper h){
        var pass=new com.mcmagic.omnira.storage.PassiveStorageInspection.Pass();
        var inventory=new CountingInventory();
        var first=new InvWrapper(inventory){};var second=new InvWrapper(inventory){};
        h.assertTrue(pass.first(first)&&pass.first(second),"Potentially distinct slot mappings were merged");
        h.assertTrue(!pass.first(first),"Identical handler scanned twice");
        h.assertTrue(pass.first(new net.neoforged.neoforge.items.ItemStackHandler(1)),"Independent storage skipped");
        h.assertTrue(new com.mcmagic.omnira.storage.PassiveStorageInspection.Pass().first(first),"Capabilities cached across maintenance passes");h.succeed();
    }
    private static ChestBlockEntity pending(GameTestHelper h){
        // A new container before its surrounding chunks are fully loaded: Lootr cannot convert it yet.
        var chest=new ChestBlockEntity(h.absolutePos(new BlockPos(10000,2,10000)),Blocks.CHEST.defaultBlockState());
        chest.setLevel(h.getLevel());chest.setLootTable(BuiltInLootTables.VILLAGE_PLAINS_HOUSE,42);
        return chest;
    }
    @GameTest(template="spell_arena") public static void aggregateDoesNotRollPendingLoot(GameTestHelper h){
        var chest=pending(h);AggregateStorage.container(chest,10);
        h.assertTrue(chest.getLootTable()!=null,"Passive aggregate scan generated unopened village loot");h.succeed();
    }
    @GameTest(template="spell_arena") public static void wetlandDoesNotRollPendingLoot(GameTestHelper h){
        var chest=pending(h);WetlandRepair.repair(new InvWrapper(chest),false);
        h.assertTrue(chest.getLootTable()!=null,"Passive wetland scan generated unopened village loot");h.succeed();
    }
    @GameTest(template="spell_arena") public static void doubleChestKeepsUnopenedHalf(GameTestHelper h){
        var chest=pending(h);
        var both=new net.minecraft.world.CompoundContainer(new net.minecraft.world.SimpleContainer(27),chest);
        var wrapper=new InvWrapper(both);
        AggregateStorage.handler(wrapper,10);
        WetlandRepair.repair(wrapper,true);
        com.mcmagic.omnira.spacetime.TimeWarpStorage.handler(wrapper,false);
        h.assertTrue(chest.getLootTable()!=null,"Double chest inspection rolled its unopened half");h.succeed();
    }
    @GameTest(template="spell_arena") public static void lootMinecartIsNotInspected(GameTestHelper h){
        var cart=new net.minecraft.world.entity.vehicle.MinecartChest(net.minecraft.world.entity.EntityType.CHEST_MINECART,h.getLevel());
        cart.setLootTable(BuiltInLootTables.ABANDONED_MINESHAFT);
        AggregateStorage.container(cart,10);
        WetlandRepair.repair(new InvWrapper(cart),true);
        com.mcmagic.omnira.spacetime.TimeWarpStorage.container(cart,false);
        h.assertTrue(cart.getLootTable()!=null,"Passive maintenance generated minecart loot");h.succeed();
    }
    @GameTest(template="spell_arena") public static void openedStorageStillMaintained(GameTestHelper h){
        var inventory=new net.minecraft.world.SimpleContainer(3);
        var aggregate=new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.UNSTABLE_SPACETIME_AGGREGATE.get());
        aggregate.setDamageValue(5);inventory.setItem(0,aggregate);
        inventory.setItem(1,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.SPACE_WARP_POINT.get(),16));
        inventory.setItem(2,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.archaeology.ArchaeologyContent.DAMAGED_FACILITY.get(),3));
        AggregateStorage.container(inventory,10);
        WetlandRepair.repair(new InvWrapper(inventory),true);
        h.assertTrue(inventory.getItem(0).getDamageValue()==4,"Aggregate repair was disabled");
        h.assertTrue(inventory.getItem(1).is(com.mcmagic.omnira.registry.ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()),"Warp collapse was disabled");
        h.assertTrue(inventory.getItem(2).is(com.mcmagic.omnira.reversal.ReversalContent.PART.get())&&inventory.getItem(2).getCount()==3,"Wetland repair lost materials");h.succeed();
    }
}
