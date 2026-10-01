package com.mcmagic.omnira.dream;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DreamRabbitItem extends Item {
    private final boolean sigil;
    public DreamRabbitItem(Properties properties,boolean sigil){super(properties);this.sigil=sigil;}
    public boolean sigil(){return sigil;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        return player instanceof ServerPlayer server && DreamRabbitTravel.start(server,hand,this)
                ?InteractionResultHolder.success(stack):InteractionResultHolder.fail(stack);
    }
}
