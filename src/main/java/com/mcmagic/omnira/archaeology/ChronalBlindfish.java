package com.mcmagic.omnira.archaeology;

import com.mcmagic.omnira.mire.MireContent;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ChronalBlindfish extends AbstractFish {
    public ChronalBlindfish(EntityType<? extends ChronalBlindfish> type,Level level){super(type,level);}
    @Override public boolean isInWater(){return super.isInWater()||isInFluidType(MireContent.TYPE.get());}
    @Override public ItemStack getBucketItemStack(){return new ItemStack(ArchaeologyContent.BLINDFISH_BUCKET.get());}
    @Override protected SoundEvent getFlopSound(){return SoundEvents.COD_FLOP;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.COD_DEATH;}
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return SoundEvents.COD_HURT;}
    @Override public int getMaxSpawnClusterSize(){return 2;}
}
