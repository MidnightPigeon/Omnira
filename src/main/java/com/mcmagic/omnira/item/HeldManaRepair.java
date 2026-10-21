package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class HeldManaRepair {
    private HeldManaRepair() {}
    public static boolean repair(ItemStack stack,ServerPlayer player) {
        boolean worn=stack.getItem() instanceof CrystalArmorItem armor && player.getItemBySlot(armor.getEquipmentSlot())==stack;
        if(!stack.isDamaged())return false;
        boolean available=worn||player.getMainHandItem()==stack||player.getOffhandItem()==stack;
        for(int i=0;!available&&i<9;i++)available=player.getInventory().getItem(i)==stack;
        if(!available)return false;
        var mana=player.getData(ModAttachments.MANA);if(!mana.canSpend(5))return false;
        player.setData(ModAttachments.MANA,mana.spend(5));stack.setDamageValue(stack.getDamageValue()-1);return true;
    }
}
