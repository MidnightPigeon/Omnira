package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.combat.HeldCombatAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LivingEntity.class)
public abstract class AllDamageBonusMixin {
    // Seed the damage container with the full amount, including paths that reuse its original damage.
    @ModifyVariable(method="hurt",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float omnira$flatDamage(float amount,DamageSource source,float original){
        if(((LivingEntity)(Object)this).level().isClientSide||amount<=0||source.getEntity()==(Object)this)return amount;
        return Math.max(0,amount+HeldCombatAttributes.flatBonus(source.getEntity()));
    }
}
