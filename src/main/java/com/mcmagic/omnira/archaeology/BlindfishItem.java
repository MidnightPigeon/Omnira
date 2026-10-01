package com.mcmagic.omnira.archaeology;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class BlindfishItem extends Item {
    public BlindfishItem(Properties properties){super(properties);}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity living){
        var result=super.finishUsingItem(stack,level,living);
        // Explicit health loss bypasses armor, absorption and damage multipliers.
        if(!level.isClientSide&&living.isAlive())living.setHealth(living.getHealth()/2);
        return result;
    }
}
