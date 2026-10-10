package com.mcmagic.omnira.paradox;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;

public final class GenesisParticlesItem extends Item {
    public GenesisParticlesItem(Properties properties){super(properties);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.omnira.genesis_particles").withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC));
    }
}
