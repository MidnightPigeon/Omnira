package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.OrbUpgrades;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Report rejected player actions, never passive slot queries or automation probes. */
public final class UpgradeMenuFeedback {
    public static boolean rejectClick(AbstractContainerMenu menu,int index,int button,ClickType type,Player player,int from,int to){
        if(index<from||index>=to)return false;
        ItemStack incoming=type==ClickType.PICKUP?menu.getCarried():
                type==ClickType.SWAP&&(button>=0&&button<9||button==40)?player.getInventory().getItem(button):ItemStack.EMPTY;
        return incompatible(menu,incoming,player,index,index+1);
    }
    public static boolean incompatible(AbstractContainerMenu menu,ItemStack stack,Player player,int from,int to){
        if(OrbUpgrades.kind(stack)==null)return false;
        for(int i=from;i<to;i++)if(menu.getSlot(i).mayPlace(stack))return false;
        if(!player.level().isClientSide)player.displayClientMessage(Component.translatable("gui.omnira.upgrade.incompatible").withStyle(ChatFormatting.RED),true);
        return true;
    }
    private UpgradeMenuFeedback(){}
}
