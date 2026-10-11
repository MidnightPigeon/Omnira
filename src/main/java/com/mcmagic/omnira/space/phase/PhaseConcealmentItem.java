package com.mcmagic.omnira.space.phase;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class PhaseConcealmentItem extends Item implements ICurioItem {
    public PhaseConcealmentItem(Properties properties){super(properties.stacksTo(1));}
    @Override public boolean canEquip(SlotContext context,ItemStack stack){return context.identifier().equals("charm");}
    public static boolean equipped(LivingEntity entity){
        return CuriosApi.getCuriosInventory(entity).map(inv->inv.findCurios(s->s.is(ModItems.PHASE_CONCEALMENT_CHARM.get()))
                .stream().anyMatch(s->!s.slotContext().cosmetic()&&s.slotContext().identifier().equals("charm"))).orElse(false);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.omnira.phase_concealment_charm").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
