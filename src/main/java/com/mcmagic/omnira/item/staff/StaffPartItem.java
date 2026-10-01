package com.mcmagic.omnira.item.staff;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import com.mcmagic.omnira.registry.ModDataComponents;
import java.util.List;

public final class StaffPartItem extends Item {
    public StaffPartItem(Properties properties,StaffPart part) {super(properties.component(ModDataComponents.STAFF_PART.get(),part));}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        var part=StaffPart.of(stack);if(part==null) return;
        if(part.role()==StaffPart.Role.SHAFT) lines.add(Component.translatable("tooltip.omnira.staff.enhancement_capacity",part.enhancementSlots()).withStyle(ChatFormatting.GRAY));
        if(part.role()==StaffPart.Role.TIP) lines.add(Component.translatable("tooltip.omnira.staff.rune_capacity",part.runeSlots()).withStyle(ChatFormatting.GRAY));
        if(part.role()==StaffPart.Role.UPGRADE) lines.add(Component.translatable("tooltip.omnira.staff.slot_type."+part.upgradeSlot().getSerializedName()).withStyle(ChatFormatting.GRAY));
        if(part.power()!=0) lines.add(Component.translatable("tooltip.omnira.staff.power",Math.round(part.power()*100)).withStyle(ChatFormatting.AQUA));
        if(part.reduction()!=0) lines.add(Component.translatable("tooltip.omnira.staff.reduction",format(part.reduction())).withStyle(ChatFormatting.AQUA));
        if(part.speed()!=0) lines.add(Component.translatable("tooltip.omnira.staff.speed",Math.round(part.speed()*100)).withStyle(ChatFormatting.AQUA));
        if(part.range()!=0) lines.add(Component.translatable("tooltip.omnira.staff.range").withStyle(ChatFormatting.AQUA));
        if(part.cooldownReduction()!=0) lines.add(Component.translatable("tooltip.omnira.staff.cooldown_reduction",Math.round(part.cooldownReduction()*100)).withStyle(ChatFormatting.AQUA));
        if(part.role()==StaffPart.Role.TIP) {
            lines.add(Component.translatable("tooltip.omnira.staff.cycle",part.shots(),format(part.cooldownTicks()/20D)).withStyle(ChatFormatting.GRAY));
            if(part.shots()>1) lines.add(Component.translatable("tooltip.omnira.staff.interval","0.3").withStyle(ChatFormatting.GRAY));
        }
    }
    public static String format(double value) {return value==(long)value?Long.toString((long)value):String.format(java.util.Locale.ROOT,"%.2f",value).replaceAll("0+$","");}
}
