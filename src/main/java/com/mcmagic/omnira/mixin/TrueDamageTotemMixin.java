package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mcmagic.omnira.combat.OmniraDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
abstract class TrueDamageTotemMixin {
    @ModifyExpressionValue(method="checkTotemDeathProtection",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean omnira$allowTrueDamageRevival(boolean bypass,DamageSource source){
        return bypass&&!OmniraDamage.isTrue(source);
    }
}
