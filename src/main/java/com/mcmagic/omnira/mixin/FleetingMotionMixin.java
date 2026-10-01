package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.time.FleetingTime;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class FleetingMotionMixin {
    @ModifyVariable(method="move",at=@At("HEAD"),argsOnly=true)
    private Vec3 omnira$gliding(Vec3 motion){
        var entity=(Entity)(Object)this;
        if(entity instanceof net.minecraft.world.entity.player.Player player&&player.isFallFlying())
            return motion.scale(player.getAttributeValue(com.mcmagic.omnira.registry.ModAttributes.GLIDING_SPEED));
        return motion;
    }
    @Inject(method="move",at=@At("HEAD"),cancellable=true)
    private void omnira$oneMovement(MoverType type,Vec3 delta,CallbackInfo ci){
        var entity=(Entity)(Object)this;
        if(FleetingTime.bonus(entity)||com.mcmagic.omnira.time.StilledTime.stopped(entity)
                ||com.mcmagic.omnira.archaeology.BrushStasis.held(entity))ci.cancel();
    }
}
