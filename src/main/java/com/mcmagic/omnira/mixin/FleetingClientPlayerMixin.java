package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.time.FleetingTime;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class FleetingClientPlayerMixin extends LivingEntity {
    protected FleetingClientPlayerMixin(EntityType<? extends LivingEntity> type,Level level){super(type,level);}
    @Inject(method="tick",at=@At("TAIL"))
    private void omnira$displayTimers(CallbackInfo ci){
        if(level().isClientSide&&FleetingTime.accelerated(level(),blockPosition())){
            attackStrengthTicker++;((Player)(Object)this).getCooldowns().tick();tickEffects();
        }
    }
}
