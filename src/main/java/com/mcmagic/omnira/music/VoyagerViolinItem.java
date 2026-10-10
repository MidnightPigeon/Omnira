package com.mcmagic.omnira.music;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class VoyagerViolinItem extends Item {
    public VoyagerViolinItem(Properties properties) { super(properties); }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){
        var text=net.minecraft.network.chat.Component.translatable("tooltip.omnira.voyager_violin").getString();
        var line=net.minecraft.network.chat.Component.empty();int i=0;
        for(int code:text.codePoints().toArray())line.append(net.minecraft.network.chat.Component.literal(new String(Character.toChars(code)))
                .withStyle(i++%2==0?net.minecraft.ChatFormatting.AQUA:net.minecraft.ChatFormatting.GOLD,net.minecraft.ChatFormatting.ITALIC));
        lines.add(line);
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.voyager_violin_use").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(hand!=InteractionHand.OFF_HAND||!player.getMainHandItem().isEmpty())return InteractionResultHolder.fail(stack);
        if(!level.isClientSide)VoyagerPerformance.toggle(player);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
