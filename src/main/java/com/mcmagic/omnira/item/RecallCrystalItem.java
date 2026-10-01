package com.mcmagic.omnira.item;

import com.mcmagic.omnira.menu.WaymarkMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class RecallCrystalItem extends Item {
    public final boolean enhanced;
    public RecallCrystalItem(Properties properties,boolean enhanced) {super(properties.stacksTo(1));this.enhanced=enhanced;}
    public int baseCost(boolean crossDimension) {return enhanced?(crossDimension?100:0):(crossDimension?300:100);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer server && !player.isSpectator() && player.isAlive()) WaymarkMenu.openRecall(server,hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
