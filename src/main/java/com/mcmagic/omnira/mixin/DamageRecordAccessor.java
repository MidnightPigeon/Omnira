package com.mcmagic.omnira.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface DamageRecordAccessor {
    @Invoker("checkTotemDeathProtection") boolean omnira$tryTotem(net.minecraft.world.damagesource.DamageSource source);
    @Accessor("lastHurtByPlayerTime") void omnira$playerHitWindow(int ticks);
    @Accessor("lastDamageSource") void omnira$lastDamageSource(net.minecraft.world.damagesource.DamageSource source);
    @Accessor("lastDamageStamp") void omnira$lastDamageStamp(long tick);
}
