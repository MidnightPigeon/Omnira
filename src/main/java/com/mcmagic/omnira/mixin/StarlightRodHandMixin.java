package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.client.StarlightRodLine;
import com.mcmagic.omnira.space.sea.StarlightRodItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
abstract class StarlightRodHandMixin {
    @Unique private LivingEntity omnira$angler;

    @WrapOperation(method="renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"))
    private void omnira$withAngler(ItemRenderer renderer,ItemStack stack,ItemDisplayContext context,boolean left,
                                   PoseStack pose,MultiBufferSource buffers,int light,int overlay,BakedModel model,
                                   Operation<Void> original,LivingEntity owner){
        if(!(stack.getItem() instanceof StarlightRodItem)){
            original.call(renderer,stack,context,left,pose,buffers,light,overlay,model);
            return;
        }
        var previous=omnira$angler;
        omnira$angler=owner;
        try {original.call(renderer,stack,context,left,pose,buffers,light,overlay,model);}
        finally {omnira$angler=previous;}
    }

    @Inject(method="render",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",ordinal=0,shift=At.Shift.AFTER))
    private void omnira$eyelet(ItemStack stack,ItemDisplayContext context,boolean left,PoseStack pose,
                               MultiBufferSource buffers,int light,int overlay,BakedModel model,CallbackInfo ci){
        if(omnira$angler!=null && (context.firstPerson() || context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND))
            StarlightRodLine.held(omnira$angler,stack,context,pose,buffers);
    }
}
