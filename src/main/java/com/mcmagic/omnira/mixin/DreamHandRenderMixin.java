package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.client.renderer.DreamPlayerVisuals;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerRenderer.class)
public abstract class DreamHandRenderMixin {
    @WrapOperation(method="renderHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void omnira$dreamHand(ModelPart model,PoseStack pose,VertexConsumer vertices,int light,int overlay,Operation<Void> original,
                                  PoseStack renderPose,MultiBufferSource buffers,int packedLight,AbstractClientPlayer player,ModelPart arm,ModelPart sleeve) {
        if(DreamPlayerVisuals.active(player))model.render(pose,DreamPlayerVisuals.buffer(buffers,player.getSkin().texture()),light,overlay,DreamPlayerVisuals.color(-1,player));
        else original.call(model,pose,vertices,light,overlay);
    }
}
