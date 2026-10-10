package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.anchor.StabilityField;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class StabilityPlayerTeleportMixin {
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",at=@At("HEAD"),cancellable=true)
    private void omnira$target(ServerLevel level,double x,double y,double z,float yaw,float pitch,CallbackInfo ci){
        if(StabilityField.blocks((ServerPlayer)(Object)this,level,new Vec3(x,y,z)))ci.cancel();
    }
}
