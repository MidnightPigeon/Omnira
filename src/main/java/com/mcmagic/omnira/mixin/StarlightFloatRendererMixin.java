package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mcmagic.omnira.space.sea.StarlightFloat;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHookRenderer.class)
abstract class StarlightFloatRendererMixin {
    @Unique private static final String omnira$renderHook="render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    @Unique private static final ModelResourceLocation omnira$model=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/starlight_float"));

    @WrapWithCondition(method=omnira$renderHook,at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/FishingHookRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;IFIII)V"),require=4)
    private boolean omnira$drawOriginalFloat(VertexConsumer out, PoseStack.Pose pose, int light, float x, int y, int u, int v, FishingHook hook) {
        return !((StarlightFloat)hook).omnira$isStarlight();
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method=omnira$renderHook,at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/FishingHookRenderer;getPlayerHandPos(Lnet/minecraft/world/entity/player/Player;FF)Lnet/minecraft/world/phys/Vec3;"))
    private net.minecraft.world.phys.Vec3 omnira$handFallback(net.minecraft.world.phys.Vec3 hand,FishingHook hook){
        com.mcmagic.omnira.client.StarlightRodLine.hook(hook,hand);
        return hand;
    }

    @WrapWithCondition(method=omnira$renderHook,at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/FishingHookRenderer;stringVertex(FFFLcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;FF)V"))
    private boolean omnira$vanillaLine(float x,float y,float z,VertexConsumer out,PoseStack.Pose pose,float t,float next,FishingHook hook){
        return !((StarlightFloat)hook).omnira$isStarlight();
    }

    // After vanilla pops its billboard rotation, before it draws the fishing line.
    @Inject(method=omnira$renderHook,at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",ordinal=0,shift=At.Shift.AFTER))
    private void omnira$drawStarFloat(FishingHook hook,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci) {
        if (!((StarlightFloat)hook).omnira$isStarlight()) return;
        var mc=Minecraft.getInstance();
        pose.pushPose();
        // Place the clasp at the vanilla line endpoint (one quarter block above the hook).
        pose.translate(0,.25 - 1.13 * 1.2 / 16,0);
        pose.scale(1.2F,1.2F,1.2F);
        pose.translate(-.5,-.5,-.5);
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(omnira$model),ItemStack.EMPTY,
                LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers.getBuffer(Sheets.cutoutBlockSheet()));
        pose.popPose();
    }
}
