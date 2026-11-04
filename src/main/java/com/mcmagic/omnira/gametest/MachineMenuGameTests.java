package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.menu.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_machine_menus") @PrefixGameTestTemplate(false)
public final class MachineMenuGameTests {
    private static ReactionPoolBlockEntity pool(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));
        h.assertTrue(ReactionPoolBlock.place(h.getLevel(),pos,Direction.NORTH),"Pool placement failed");
        return ReactionPoolBlock.controller(h.getLevel(),pos);
    }
    private static ItemStack sample(int count){
        var stack=new ItemStack(Items.DIAMOND,count);stack.set(DataComponents.CUSTOM_NAME,Component.literal("transfer-test"));return stack;
    }
    private static int total(AbstractContainerMenu menu,ItemStack sample){
        int total=0;for(var slot:menu.slots)if(ItemStack.isSameItemSameComponents(slot.getItem(),sample))total+=slot.getItem().getCount();
        if(ItemStack.isSameItemSameComponents(menu.getCarried(),sample))total+=menu.getCarried().getCount();return total;
    }
    @GameTest(template="spell_arena") public static void poolEveryInventorySlotPreservesComponents(GameTestHelper h){
        var pool=pool(h);var player=h.makeMockPlayer(GameType.SURVIVAL);var menu=new ReactionPoolMenu(1,player.getInventory(),pool);
        for(int source=13;source<49;source++){
            player.getInventory().clearContent();for(int i=0;i<9;i++)pool.items.setStackInSlot(i,ItemStack.EMPTY);
            var sample=sample(37);menu.getSlot(source).set(sample.copy());menu.clicked(source,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(total(menu,sample)==37&&pool.items.getStackInSlot(0).getCount()==37&&menu.getSlot(source).getItem().isEmpty(),"Pool shift insertion lost items/components at "+source);
            menu.clicked(0,0,ClickType.QUICK_MOVE,player);
            h.assertTrue(pool.items.getStackInSlot(0).isEmpty()&&total(menu,sample)==37,"Pool extraction lost or duplicated items");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void poolPartialFullAndOversizedTransfers(GameTestHelper h){
        var pool=pool(h);var player=h.makeMockPlayer(GameType.SURVIVAL);var menu=new ReactionPoolMenu(1,player.getInventory(),pool);var sample=sample(12);
        pool.items.setStackInSlot(0,sample.copyWithCount(60));menu.getSlot(13).set(sample.copy());menu.quickMoveStack(player,13);
        h.assertTrue(pool.items.getStackInSlot(0).getCount()==64&&menu.getSlot(13).getItem().getCount()==8&&total(menu,sample)==72,"Partial pool merge lost remainder");
        h.assertTrue(menu.quickMoveStack(player,13).isEmpty()&&total(menu,sample)==72,"Full item kind accepted or deleted input");
        player.getInventory().clearContent();pool.upgrades.insertItem(0,new ItemStack(ModItems.INFUSED_STACKING_UPGRADE.get()),false);
        pool.items.setStackInSlot(0,sample.copyWithCount(256));menu.clicked(0,0,ClickType.QUICK_MOVE,player);
        h.assertTrue(total(menu,sample)==256&&pool.items.getStackInSlot(0).isEmpty(),"Oversized extraction did not preserve all 256 items");
        for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        pool.items.setStackInSlot(0,sample.copyWithCount(32));h.assertTrue(menu.quickMoveStack(player,0).isEmpty()&&pool.items.getStackInSlot(0).getCount()==32,"Full player inventory consumed output");
        player.getInventory().setItem(0,sample.copyWithCount(60));menu.quickMoveStack(player,0);
        h.assertTrue(pool.items.getStackInSlot(0).getCount()==28&&player.getInventory().getItem(0).getCount()==64,"Partial output did not leave remainder");
        h.succeed();
    }
    private static void upgrades(GameTestHelper h,AbstractContainerMenu menu,Player player,int from,int to,int playerStart,Set<Item> allowed){
        for(var item:BuiltInRegistries.ITEM)if(item instanceof com.mcmagic.omnira.item.OrbUpgradeItem){
            for(int method=0;method<3;method++){
                for(int i=from;i<to;i++)menu.getSlot(i).set(ItemStack.EMPTY);
                player.getInventory().clearContent();menu.setCarried(ItemStack.EMPTY);var stack=new ItemStack(item);
                int target=from;for(int i=from;i<to;i++)if(menu.getSlot(i).mayPlace(stack)){target=i;break;}
                if(method==0){menu.getSlot(playerStart).set(stack);menu.quickMoveStack(player,playerStart);}
                else if(method==1){menu.setCarried(stack);menu.clicked(target,0,ClickType.PICKUP,player);}
                else {player.getInventory().setItem(0,stack);menu.clicked(target,0,ClickType.SWAP,player);}
                boolean installed=false;for(int i=from;i<to;i++)installed|=menu.getSlot(i).getItem().is(item);
                h.assertTrue(installed==allowed.contains(item),"Upgrade compatibility mismatch: "+item+" method "+method+" menu "+menu.getClass().getSimpleName());
                h.assertTrue(total(menu,new ItemStack(item))==1,"Upgrade insertion lost or duplicated plate");
                if(installed){
                    // Free the source/cursor before extracting the installed plate.
                    menu.setCarried(ItemStack.EMPTY);menu.quickMoveStack(player,target);
                    h.assertTrue(menu.getSlot(target).getItem().isEmpty()&&total(menu,new ItemStack(item))==1,"Upgrade shift extraction failed");
                }else h.assertTrue(UpgradeMenuFeedback.incompatible(menu,stack,player,from,to),"Rejected upgrade has no feedback");
            }
        }
    }
    @GameTest(template="spell_arena") public static void poolAndChamberUpgradeCompatibility(GameTestHelper h){
        var pool=pool(h);var player=h.makeMockPlayer(GameType.SURVIVAL);
        upgrades(h,new ReactionPoolMenu(1,player.getInventory(),pool),player,9,13,13,Set.of(ModItems.INTAKE_UPGRADE.get(),ModItems.OUTPUT_UPGRADE.get(),ModItems.BASIC_STACKING_UPGRADE.get(),ModItems.INFUSED_STACKING_UPGRADE.get(),ModItems.RECIPE_CONTROL_UPGRADE.get(),ModItems.SPEED_UPGRADE.get()));
        var chamber=new CatalysisBlockEntity(h.absolutePos(new BlockPos(10,3,10)),CatalysisContent.BLOCK.get().defaultBlockState());chamber.setLevel(h.getLevel());
        upgrades(h,new CatalysisMenu(2,player.getInventory(),chamber),player,3,6,6,Set.of(ModItems.INTAKE_UPGRADE.get(),ModItems.OUTPUT_UPGRADE.get(),ModItems.RECIPE_CONTROL_UPGRADE.get(),ModItems.SPEED_UPGRADE.get()));h.succeed();
    }
    @GameTest(template="spell_arena") public static void poolCapacityUpgradeCannotBeShiftRemoved(GameTestHelper h){
        var pool=pool(h);var player=h.makeMockPlayer(GameType.SURVIVAL);var menu=new ReactionPoolMenu(1,player.getInventory(),pool);
        pool.upgrades.insertItem(0,new ItemStack(ModItems.BASIC_STACKING_UPGRADE.get()),false);pool.items.setStackInSlot(0,sample(128));
        h.assertTrue(menu.quickMoveStack(player,9).isEmpty()&&pool.upgrades.getStackInSlot(0).is(ModItems.BASIC_STACKING_UPGRADE.get()),"Shift bypassed capacity guard");
        menu.quickMoveStack(player,0);menu.quickMoveStack(player,9);
        h.assertTrue(pool.upgrades.getStackInSlot(0).isEmpty()&&player.getInventory().countItem(ModItems.BASIC_STACKING_UPGRADE.get())==1,"Safe capacity removal failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void storageAndEnergyOrbUpgradeMenus(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);var all=new HashSet<Item>();
        for(var item:BuiltInRegistries.ITEM)if(item instanceof com.mcmagic.omnira.item.OrbUpgradeItem)all.add(item);
        all.remove(ModItems.RECIPE_CONTROL_UPGRADE.get());
        for(var id:List.of("crystal_ball","liquid_crystal_ball","resonant_crystal_ball","resonant_liquid_crystal_ball","pulse_accumulator_orb","resonant_accumulator_orb")){
            var pos=new BlockPos(5,3,5);h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("omnira",id)));
            var be=h.getLevel().getBlockEntity(h.absolutePos(pos));
            if(be instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball){
                var menu=new CrystalBallMenu(1,player.getInventory(),ball);
                var allowed=new HashSet<Item>(all);
                if(ball instanceof com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)
                    allowed.remove(ModItems.INFUSED_INTAKE_UPGRADE.get());
                upgrades(h,menu,player,menu.upgradeStart,menu.frequencyStart,menu.playerStart,allowed);
            }else{
                var orb=com.mcmagic.omnira.pulse.PulseOrbEntity.from(be);h.assertTrue(orb!=null,"Missing energy orb");
                var allowed=new HashSet<Item>();for(var item:all)if(orb.accepts(new ItemStack(item)))allowed.add(item);
                var menu=new com.mcmagic.omnira.pulse.PulseOrbMenu(1,player.getInventory(),orb);upgrades(h,menu,player,0,1,menu.playerStart,allowed);
            }
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void cruiseInsertionAndUpgradeMenus(GameTestHelper h){
        var orb=ModEntityTypes.CRUISE_ORB.get().create(h.getLevel());orb.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(8,20,8)));
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(orb.position().add(0,com.mcmagic.omnira.vehicle.CruiseOrbPanel.RIDER_Y,.18));
        h.assertTrue(player.startRiding(orb),"Cannot mount test orb");
        var menu=new com.mcmagic.omnira.vehicle.CruiseOrbMenu(1,player.getInventory(),orb);
        try{
            var sample=sample(37);menu.getSlot(18).set(sample.copy());menu.quickMoveStack(player,18);
            h.assertTrue(orb.storage.items.getStackInSlot(0).getCount()==37&&total(menu,sample)==37,"Cruise shift insertion deleted/duplicated items");
            menu.quickMoveStack(player,0);h.assertTrue(total(menu,sample)==37&&orb.storage.items.getStackInSlot(0).isEmpty(),"Cruise extraction changed count");
            var allowed=new HashSet<Item>();for(var item:BuiltInRegistries.ITEM)if(item instanceof com.mcmagic.omnira.item.OrbUpgradeItem plate
                    &&plate.kind!=com.mcmagic.omnira.item.OrbUpgradeItem.Kind.INTAKE&&plate.kind!=com.mcmagic.omnira.item.OrbUpgradeItem.Kind.OUTPUT&&plate.kind!=com.mcmagic.omnira.item.OrbUpgradeItem.Kind.RECIPE_CONTROL)allowed.add(item);
            upgrades(h,menu,player,13,16,18,allowed);
        }finally{player.stopRiding();orb.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void ordinaryMachineShiftConservation(GameTestHelper h){
        var ids=List.of("crystal_processing_table","analysis_artisan_table","arcane_assembly_table","mana_engine","spacetime_reverser","void_infuser","split_time_sundial",
                "crystal_furnace","crystal_grindstone","crystal_anvil","infused_crystal_furnace","infused_crystal_grindstone","infused_crystal_anvil");
        var player=h.makeMockPlayer(GameType.SURVIVAL);var pos=new BlockPos(5,3,5);
        for(var id:ids){
            var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("omnira",id));h.setBlock(pos,block);
            var be=h.getLevel().getBlockEntity(h.absolutePos(pos));
            MenuProvider provider=be instanceof com.mcmagic.omnira.block.entity.AssemblyAccess access?access.assembly():
                    be instanceof com.mcmagic.omnira.workstation.WorkstationAccess access?access.workstation():be instanceof MenuProvider p?p:null;
            h.assertTrue(provider!=null,"Missing machine menu: "+id);
            var menu=provider.createMenu(1,player.getInventory(),player);
            int playerStart=menu.slots.size()-36;
            for(var item:List.of(Items.STONE,Items.IRON_SWORD,Items.COAL,ModItems.EARTH_MICROCORE.get(),ModItems.SPIRITUAL_CRYSTAL.get(),ModItems.INTAKE_UPGRADE.get())){
                player.getInventory().clearContent();for(int i=0;i<playerStart;i++)menu.getSlot(i).set(ItemStack.EMPTY);
                var stack=new ItemStack(item,item.getDefaultMaxStackSize()>1?17:1);
                menu.getSlot(playerStart).set(stack.copy());menu.quickMoveStack(player,playerStart);
                h.assertTrue(total(menu,stack)==stack.getCount(),"Shift input violates conservation: "+id+" / "+item);
                for(int i=0;i<playerStart;i++)menu.quickMoveStack(player,i);
                h.assertTrue(total(menu,stack)==stack.getCount(),"Shift output violates conservation: "+id+" / "+item);
            }
        }h.succeed();
    }
    private MachineMenuGameTests(){}
}
