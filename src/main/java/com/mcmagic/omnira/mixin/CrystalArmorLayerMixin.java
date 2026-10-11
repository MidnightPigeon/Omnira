package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import org.spongepowered.asm.mixin.Unique;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

@Mixin(HumanoidArmorLayer.class)
public abstract class CrystalArmorLayerMixin {
    @Unique private boolean omnira$phaseWearer;
    @WrapMethod(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V")
    private void omnira$wearer(PoseStack pose,MultiBufferSource buffers,int light,LivingEntity wearer,float swing,float amount,float partial,float age,float yaw,float pitch,Operation<Void> original){
        boolean previous=omnira$phaseWearer;omnira$phaseWearer=com.mcmagic.omnira.space.phase.PhaseConcealment.active(wearer);
        try{original.call(pose,buffers,light,wearer,swing,amount,partial,age,yaw,pitch);}finally{omnira$phaseWearer=previous;}
    }
    @WrapOperation(method="renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void omnira$phaseArmor(net.minecraft.client.model.Model model,PoseStack pose,VertexConsumer vertices,int light,int overlay,int color,Operation<Void> original,
                                  PoseStack renderPose,MultiBufferSource buffers,int packedLight,net.minecraft.client.model.Model renderModel,int dye,ResourceLocation texture){
        if(omnira$phaseWearer){vertices=com.mcmagic.omnira.client.renderer.DreamPlayerVisuals.buffer(buffers,texture);color=(color&0xFFFFFF)|((int)((color>>>24)*.38F)<<24);}
        original.call(model,pose,vertices,light,overlay,color);
    }
    // NeoForge's HumanoidModel overload only delegates; the Model overload draws the armor.
    @WrapOperation(method="renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType omnira$transparentPlate(ResourceLocation texture,Operation<RenderType> original){
        if(texture.getNamespace().equals("omnira")){
            if(texture.getPath().equals("textures/models/armor/crystal_armor_layer_1.png"))return com.mcmagic.omnira.client.renderer.CrystalGlassLayer.ARMOR_OUTER;
            if(texture.getPath().equals("textures/models/armor/crystal_armor_layer_2.png"))return com.mcmagic.omnira.client.renderer.CrystalGlassLayer.ARMOR_INNER;
        }
        return original.call(texture);
    }
}
