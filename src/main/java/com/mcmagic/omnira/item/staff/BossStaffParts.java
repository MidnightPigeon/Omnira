package com.mcmagic.omnira.item.staff;

import com.mcmagic.omnira.item.StaffItem;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import java.util.List;

/** Conditional held benefits share native modifier IDs across hands; held life steal joins the existing additive attribute. */
public final class BossStaffParts {
    public static final ResourceLocation DAMAGE=ResourceLocation.parse("omnira:monstrous_staff_damage"),
            GUARD=ResourceLocation.parse("omnira:guardian_staff_protection"),LEECH=ResourceLocation.parse("omnira:cursium_staff_leech");
    public static boolean monstrous(ItemStack stack){return stack.getItem() instanceof StaffItem&&StaffAssembly.of(stack).shaft().is(ModItems.MONSTROUS_STAFF_SHAFT.get());}
    public static boolean guardian(ItemStack stack){return stack.getItem() instanceof StaffItem&&StaffAssembly.of(stack).reinforcement().is(ModItems.GUARD_REINFORCEMENT.get());}
    public static boolean cursium(ItemStack stack){return stack.getItem() instanceof StaffItem&&StaffAssembly.of(stack).reinforcement().is(ModItems.CURSIUM_REINFORCEMENT.get());}
    public static void attributes(ItemAttributeModifierEvent event){
        var stack=event.getItemStack();
        if(monstrous(stack))event.replaceModifier(ModAttributes.DAMAGE_DEALT,new AttributeModifier(DAMAGE,.1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.HAND);
        if(cursium(stack))event.replaceModifier(ModAttributes.LIFESTEAL,new AttributeModifier(LEECH,.1,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.HAND);
        if(guardian(stack))event.replaceModifier(ModAttributes.DAMAGE_TAKEN,new AttributeModifier(GUARD,-.1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.HAND);
    }
    public static void refresh(Player player){
        set(player,ModAttributes.DAMAGE_DEALT,DAMAGE,monstrous(player.getMainHandItem())||monstrous(player.getOffhandItem())?.1:0);
        set(player,ModAttributes.LIFESTEAL,LEECH,cursium(player.getMainHandItem())||cursium(player.getOffhandItem())?.1:0);
        set(player,ModAttributes.DAMAGE_TAKEN,GUARD,guardian(player.getMainHandItem())||guardian(player.getOffhandItem())?-.1:0);
    }
    private static void set(Player player,Holder<Attribute> type,ResourceLocation id,double amount){
        var attribute=player.getAttribute(type);if(attribute==null)return;
        var operation=type.equals(ModAttributes.LIFESTEAL)?AttributeModifier.Operation.ADD_VALUE:AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        var old=attribute.getModifier(id);
        if(old!=null&&old.amount()==amount&&old.operation()==operation)return;
        if(old!=null)attribute.removeModifier(id);
        if(amount!=0)attribute.addTransientModifier(new AttributeModifier(id,amount,operation));
    }
    public static void partTooltip(ItemStack stack,List<Component> lines){
        String key=stack.is(ModItems.MONSTROUS_STAFF_SHAFT.get())?"monstrous":stack.is(ModItems.GUARD_REINFORCEMENT.get())?"guard":stack.is(ModItems.CURSIUM_REINFORCEMENT.get())?"cursium":null;
        if(key!=null)lines.add(Component.translatable("tooltip.omnira.staff."+key).withStyle(net.minecraft.ChatFormatting.AQUA));
    }
    private BossStaffParts(){}
}
