package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.abyss.AbyssGravity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Camera.class)
abstract class AbyssCameraMixin {
    @Shadow private Entity entity;
    @Unique private float omnira$partial;
    @Inject(method="setup",at=@At("HEAD"))
    private void omnira$frame(net.minecraft.world.level.BlockGetter level,Entity cameraEntity,boolean third,boolean reverse,float partial,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){omnira$partial=partial;}
    @ModifyArg(method="setup",at=@At(value="INVOKE",target="Lnet/minecraft/client/Camera;setRotation(FFF)V",ordinal=0),index=2)
    private float omnira$roll(float roll){return roll+com.mcmagic.omnira.space.abyss.GravityTransition.roll(entity,omnira$partial);}
    @ModifyArg(method="setup",at=@At(value="INVOKE",target="Lnet/minecraft/client/Camera;setPosition(DDD)V"),index=1)
    private double omnira$eyes(double y){return y+com.mcmagic.omnira.space.abyss.GravityTransition.eyeOffset(entity,omnira$partial);}
}
