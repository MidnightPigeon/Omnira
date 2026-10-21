package com.mcmagic.omnira.mixin;

import net.minecraft.tags.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSource.class)
public abstract class FounderDamageSourceMixin {
    @Inject(method="is(Lnet/minecraft/tags/TagKey;)Z",at=@At("HEAD"),cancellable=true)
    private void omnira$founderCooldown(TagKey<DamageType> tag,CallbackInfoReturnable<Boolean> cir){
        if(tag==DamageTypeTags.BYPASSES_COOLDOWN&&((DamageSource)(Object)this).getEntity() instanceof Player player
                &&com.mcmagic.omnira.weaver.FounderApprovalItem.held(player))cir.setReturnValue(true);
    }
}
