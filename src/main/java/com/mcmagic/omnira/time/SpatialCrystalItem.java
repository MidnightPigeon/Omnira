package com.mcmagic.omnira.time;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class SpatialCrystalItem extends Item {
    public SpatialCrystalItem(Properties p){super(p);}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
        if(level.isClientSide&&selected&&level.random.nextInt(5)==0)NatureParticles.space(level,holder.blockPosition().above(),level.random);
    }
    @Override public boolean onEntityItemUpdate(ItemStack stack,ItemEntity entity){
        if(entity.level().isClientSide&&entity.tickCount%5==0)NatureParticles.space(entity.level(),entity.blockPosition(),entity.level().random);
        return false;
    }
}
