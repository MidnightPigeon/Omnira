package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HumanoidArmorLayer.class)
public abstract class CrystalArmorLayerMixin {
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
