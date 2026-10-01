package com.mcmagic.omnira.mire;

import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TimeflowEel extends AbstractFish {
    public TimeflowEel(EntityType<? extends TimeflowEel> type,Level level){super(type,level);}
    // NeoForge's vanilla water check uses FluidType, not the minecraft:water tag.
    @Override public boolean isInWater(){return super.isInWater()||isInFluidType(MireContent.TYPE.get());}
    @Override public ItemStack getBucketItemStack(){return new ItemStack(MireContent.EEL_BUCKET.get());}
    @Override protected SoundEvent getFlopSound(){return SoundEvents.COD_FLOP;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.COD_DEATH;}
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return SoundEvents.COD_HURT;}
    @Override public int getMaxSpawnClusterSize(){return 2;}
}
