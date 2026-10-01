package com.mcmagic.omnira.item;

import com.mcmagic.omnira.spell.ElementType;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class PrimaryMicrocoreItem extends Item {
    public static final String TARGET_TOOLTIP = "tooltip.omnira.microcore.target_keyword";
    public static final String SHAPE_TOOLTIP = "tooltip.omnira.microcore.shape_keyword";

    public PrimaryMicrocoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        ElementType.byMicrocore(stack).filter(ElementType::isPrimary).ifPresent(element -> {
            lines.add(Component.translatable(TARGET_TOOLTIP,
                    Component.translatable("spell_target_keyword.omnira." + element.targetKeyword().orElseThrow().getSerializedName())
                            .withStyle(style -> style.withColor(element.tooltipColor())))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(SHAPE_TOOLTIP,
                    Component.translatable("spell_shape_keyword.omnira." + element.shapeKeyword().orElseThrow().getSerializedName())
                            .withStyle(style -> style.withColor(element.tooltipColor())))
                    .withStyle(ChatFormatting.GRAY));
        });
    }
}
