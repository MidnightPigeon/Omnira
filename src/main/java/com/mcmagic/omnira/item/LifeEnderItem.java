package com.mcmagic.omnira.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import java.util.List;

public final class LifeEnderItem extends Item {
    public LifeEnderItem(Properties properties) {
        super(properties.stacksTo(1).attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath("omnira","life_ender"),-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),EquipmentSlotGroup.HAND).build()));
    }
    public static boolean held(LivingEntity entity) {return entity.getMainHandItem().getItem() instanceof LifeEnderItem || entity.getOffhandItem().getItem() instanceof LifeEnderItem;}
    @Override public boolean onEntitySwing(ItemStack stack,LivingEntity entity) {
        if(!entity.level().isClientSide)entity.level().playSound(null,entity.blockPosition(),net.minecraft.sounds.SoundEvents.BEE_LOOP,net.minecraft.sounds.SoundSource.PLAYERS,.6F,1.5F);
        return false;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.omnira.life_ender").withStyle(style->style.withColor(0xB7D354).withItalic(true)));
    }
}
