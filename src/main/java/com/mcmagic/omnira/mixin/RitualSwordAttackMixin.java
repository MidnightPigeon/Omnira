package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.item.RitualSwordItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class RitualSwordAttackMixin {
    @Inject(method="entityInteractionRange",at=@At("RETURN"),cancellable=true)
    private void omnira$wideSwordReach(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValue()*com.mcmagic.omnira.item.SwordActions.reachMultiplier((Player)(Object)this));
    }
    @Inject(method="attack",at=@At("HEAD"),cancellable=true)
    private void omnira$missingNeedle(Entity target,CallbackInfo ci) {
        if(com.mcmagic.omnira.item.SwordActions.weaponUnavailable((Player)(Object)this))ci.cancel();
    }
    @Inject(method="getAttackStrengthScale",at=@At("HEAD"),cancellable=true)
    private void omnira$fullSlashDamage(float partial,CallbackInfoReturnable<Float> cir) {
        if(com.mcmagic.omnira.item.SwordActions.isCharged((Player)(Object)this))cir.setReturnValue(1F);
    }
    @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean omnira$weaponContact(Entity target,DamageSource source,float amount,Operation<Boolean> original) {
        boolean hurt=original.call(target,source,com.mcmagic.omnira.item.SwordActions.damage((Player)(Object)this,amount));
        RitualSwordItem.contact((Player)(Object)this,target,hurt);
        return hurt;
    }
}
