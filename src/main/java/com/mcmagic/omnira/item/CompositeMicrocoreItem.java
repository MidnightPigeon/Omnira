package com.mcmagic.omnira.item;

import com.mcmagic.omnira.spell.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;

public final class CompositeMicrocoreItem extends Item {
    public CompositeMicrocoreItem(Properties properties){super(properties);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        ElementType.byMicrocore(stack).ifPresent(element->lines.add(Component.translatable("spell_composite.omnira."+
                new SpellPattern(element,null).compositeName()).withStyle(s->s.withColor(element.tooltipColor()))));
        lines.add(Component.translatable("tooltip.omnira.microcore.composite").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
