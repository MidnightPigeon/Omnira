package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.mana.ManaState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import java.util.*;

@GameTestHolder("omnira_hotbar_repair")
@PrefixGameTestTemplate(false)
public final class HotbarManaRepairGameTests {
    private static List<Item> items(){return BuiltInRegistries.ITEM.stream().filter(item->item==ModItems.INFUSED_CRYSTAL_PICKAXE.get()
            ||item instanceof RitualSwordItem||item instanceof CrystalArmorItem||item instanceof com.mcmagic.omnira.space.sea.DeflectionFieldItem).toList();}
    private static FakePlayer player(GameTestHelper h){return new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"hotbar-repair"));}
    @GameTest(template="spell_arena") public static void allRepairFamiliesSupportHotbarAndRejectBackpack(GameTestHelper h){
        var p=player(h);p.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(100);
        h.assertTrue(items().size()>10,"Repair item variants were not covered");
        for(var item:items()){
            p.getInventory().clearContent();p.getInventory().selected=0;
            var stack=new ItemStack(item);stack.setDamageValue(3);p.getInventory().setItem(7,stack);p.setData(ModAttachments.MANA,new ManaState(20,100));
            h.assertTrue(HeldManaRepair.repair(stack,p)&&stack.getDamageValue()==2&&p.getData(ModAttachments.MANA).current()==15,"Unselected hotbar repair or fixed cost failed: "+item);
            p.getInventory().setItem(7,ItemStack.EMPTY);p.getInventory().setItem(9,stack);
            h.assertTrue(!HeldManaRepair.repair(stack,p)&&stack.getDamageValue()==2&&p.getData(ModAttachments.MANA).current()==15,"Backpack item repaired: "+item);
            p.getInventory().setItem(9,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,stack);
            h.assertTrue(HeldManaRepair.repair(stack,p)&&stack.getDamageValue()==1,"Offhand repair regressed: "+item);
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            if(item instanceof CrystalArmorItem armor)p.setItemSlot(armor.getEquipmentSlot(),stack);else p.getInventory().setItem(0,stack);
            h.assertTrue(HeldManaRepair.repair(stack,p)&&stack.getDamageValue()==0&&!HeldManaRepair.repair(stack,p)&&p.getData(ModAttachments.MANA).current()==5,"Equipped or full-durability repair regressed: "+item);
        }h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=40) public static void unselectedInventoryTicksRepairEveryFamily(GameTestHelper h){
        var p=player(h);GameType.CREATIVE.updatePlayerAbilities(p.getAbilities());p.getInventory().selected=0;
        long now=h.getLevel().getGameTime();
        h.runAfterDelay((int)Math.floorMod(-now,10)+10,()->{
            h.assertTrue(h.getLevel().getGameTime()%10==0,"Repair tick not aligned");
            for(var item:items()){
                p.getInventory().clearContent();var stack=new ItemStack(item);stack.setDamageValue(3);p.getInventory().setItem(8,stack);
                p.setData(ModAttachments.MANA,new ManaState(5,100));item.inventoryTick(stack,h.getLevel(),p,8,false);
                h.assertTrue(stack.getDamageValue()==2&&p.getData(ModAttachments.MANA).current()==0,"Actual inventory tick did not repair or waived cost: "+item);
                item.inventoryTick(stack,h.getLevel(),p,8,false);h.assertTrue(stack.getDamageValue()==2,"Insufficient mana repaired twice: "+item);
            }h.succeed();
        });
    }
}
