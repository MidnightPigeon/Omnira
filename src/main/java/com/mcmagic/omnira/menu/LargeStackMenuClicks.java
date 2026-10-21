package com.mcmagic.omnira.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Only intercept oversized extraction; ordinary insertion and gestures remain vanilla. */
public final class LargeStackMenuClicks {
    public static boolean handle(AbstractContainerMenu menu,int index,int button,ClickType type,Player player,int from,int to){
        if(index<from||index>=to)return false;
        var slot=menu.getSlot(index);var stored=slot.getItem();int limit=stored.getMaxStackSize();
        if(stored.getCount()<=limit)return false;
        if(type==ClickType.PICKUP&&(button==0||button==1)){
            var carried=menu.getCarried();
            if(carried.isEmpty()){
                menu.setCarried(slot.safeTake(button==0?limit:(limit+1)/2,limit,player));return true;
            }
            return !ItemStack.isSameItemSameComponents(stored,carried);
        }
        if(type==ClickType.SWAP&&(button>=0&&button<9||button==40)){
            if(player.getInventory().getItem(button).isEmpty())player.getInventory().setItem(button,slot.safeTake(limit,limit,player));
            return true;
        }
        if(type==ClickType.THROW&&menu.getCarried().isEmpty()){
            var taken=slot.safeTake(button==0?1:limit,limit,player);if(!taken.isEmpty())player.drop(taken,true);return true;
        }
        return false;
    }
    private LargeStackMenuClicks(){}
}
