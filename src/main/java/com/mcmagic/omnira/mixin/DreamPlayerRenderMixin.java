package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.client.renderer.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class DreamPlayerRenderMixin {
    @WrapOperation(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void omnira$dreamBody(EntityModel<?> model,PoseStack pose,VertexConsumer vertices,int light,int overlay,int color,Operation<Void> original,
                                  LivingEntity entity,float yaw,float partial,PoseStack renderPose,MultiBufferSource buffers,int packedLight) {
        if(entity instanceof AbstractClientPlayer player && DreamPlayerVisuals.active(player)) {
            vertices=DreamPlayerVisuals.buffer(buffers,player.getSkin().texture());color=DreamPlayerVisuals.color(color,player);
        }
        original.call(model,pose,vertices,light,overlay,color);
    }
}
