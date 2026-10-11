package com.mcmagic.omnira.item;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class GuideBookItem extends Item {
    public GuideBookItem(Properties properties) {super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(player instanceof net.minecraft.server.level.ServerPlayer server){
            var book=player.getItemInHand(hand);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server,new com.mcmagic.omnira.guidance.GuideReadingPayload(
                    com.mcmagic.omnira.guidance.GuideBookmark.identity(book),hand,com.mcmagic.omnira.guidance.GuideBookmark.read(book)));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
