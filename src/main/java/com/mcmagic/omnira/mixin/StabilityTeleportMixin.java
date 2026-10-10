package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.anchor.StabilityField;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.Set;

@Mixin({Entity.class,ServerPlayer.class})
abstract class StabilityTeleportMixin {
    @Inject(method="changeDimension",at=@At("HEAD"),cancellable=true)
    private void omnira$dimension(DimensionTransition transition,CallbackInfoReturnable<Entity> cir){
        if(StabilityField.blocks((Entity)(Object)this,transition.newLevel(),transition.pos()))cir.setReturnValue(null);
    }
    @Inject(method="teleportTo(DDD)V",at=@At("HEAD"),cancellable=true)
    private void omnira$local(double x,double y,double z,CallbackInfo ci){
        var entity=(Entity)(Object)this;if(entity.level() instanceof ServerLevel level&&StabilityField.blocks(entity,level,new Vec3(x,y,z)))ci.cancel();
    }
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",at=@At("HEAD"),cancellable=true)
    private void omnira$target(ServerLevel level,double x,double y,double z,Set<RelativeMovement> relative,float yaw,float pitch,CallbackInfoReturnable<Boolean> cir){
        if(StabilityField.blocks((Entity)(Object)this,level,new Vec3(x,y,z)))cir.setReturnValue(false);
    }
    @Inject(method="teleportRelative",at=@At("HEAD"),cancellable=true)
    private void omnira$relative(double x,double y,double z,CallbackInfo ci){
        var entity=(Entity)(Object)this;if(entity.level() instanceof ServerLevel level&&StabilityField.blocks(entity,level,entity.position().add(x,y,z)))ci.cancel();
    }
}
