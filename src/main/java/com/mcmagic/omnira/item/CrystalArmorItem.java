package com.mcmagic.omnira.item;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class CrystalArmorItem extends ArmorItem {
    public CrystalArmorItem(Holder<ArmorMaterial> material,Type type,Properties properties){super(material,type,properties);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        boolean dress=getMaterial().equals(com.mcmagic.omnira.registry.ArmorContent.DRESS);
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.armor_set."+(dress?"dress":"plate"))
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        super.inventoryTick(stack,level,entity,slot,selected);
        if(entity instanceof ServerPlayer player && level.getGameTime()%10==0)HeldManaRepair.repair(stack,player);
    }
}
