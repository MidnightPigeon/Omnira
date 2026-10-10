package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.abyss.AbyssGravity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
abstract class AbyssLivingRenderMixin {
    @Inject(method="setupRotations",at=@At("TAIL"))
    private void omnira$inverted(LivingEntity e,PoseStack pose,float bob,float yaw,float partial,float scale,CallbackInfo ci){
        float roll=com.mcmagic.omnira.space.abyss.GravityTransition.roll(e,partial);
        if(roll!=0&&!LivingEntityRenderer.isEntityUpsideDown(e)){
            pose.translate(0,e.getBbHeight()/scale/2,0);pose.mulPose(Axis.ZP.rotationDegrees(roll));pose.translate(0,-e.getBbHeight()/scale/2,0);
        }
    }
}
