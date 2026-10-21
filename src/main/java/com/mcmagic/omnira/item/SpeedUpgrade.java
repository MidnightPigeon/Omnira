package com.mcmagic.omnira.item;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Two real, recoverable cores stored in the vanilla container component. */
public final class SpeedUpgrade {
    public static NonNullList<ItemStack> cores(ItemStack plate){
        var result=NonNullList.withSize(2,ItemStack.EMPTY);
        plate.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(result);
        return result;
    }
    public static int bonus(ItemStack core){
        if(!(core.getItem() instanceof SpellCoreItem spell))return 0;
        return switch(spell.kind()){case TEST->1;case DREAM,LIGHT_DARK->2;case SPACETIME->3;};
    }
    public static void cores(ItemStack plate,java.util.List<ItemStack> cores){
        plate.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(cores));
    }
    public static int multiplier(ItemStack plate){
        int value=2;for(var core:cores(plate))value+=bonus(core);return value;
    }
    private SpeedUpgrade(){}
}
