package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.music.*;
import com.mcmagic.omnira.client.guide.GuideProjection;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ViolinHandLayerMixin extends RenderLayer<LivingEntity,HumanoidModel<LivingEntity>> {
    protected ViolinHandLayerMixin(RenderLayerParent<LivingEntity,HumanoidModel<LivingEntity>> parent){super(parent);}
    @Inject(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",at=@At("HEAD"),cancellable=true)
    private void omnira$instrument(PoseStack pose,MultiBufferSource buffers,int light,LivingEntity e,
            float limb,float amount,float partial,float age,float yaw,float pitch,CallbackInfo ci){
        if(GuideProjection.holding(e)){
            GuideProjection.thirdPerson(pose,buffers,getParentModel(),partial);
        }else if(e instanceof Player p&&ViolinClient.active(p)){
            ViolinRender.thirdPerson(p,getParentModel(),pose,buffers,light);ci.cancel();
        }
    }
    @Inject(method="renderArmWithItem",at=@At("HEAD"),cancellable=true)
    private void omnira$projectionHand(LivingEntity entity,net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.item.ItemDisplayContext context,net.minecraft.world.entity.HumanoidArm arm,
            PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){
        if(GuideProjection.holding(entity) && arm==entity.getMainArm())ci.cancel();
    }
}
