package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.client.renderer.WorldGlassBuffers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiBufferSource.BufferSource.class)
public abstract class WorldGlassBufferMixin {
    @Inject(method="getBuffer",at=@At("HEAD"),cancellable=true)
    private void omnira$collectGlass(RenderType type,CallbackInfoReturnable<VertexConsumer> callback){
        var vertices=WorldGlassBuffers.redirect((MultiBufferSource.BufferSource)(Object)this,type);
        if(vertices!=null)callback.setReturnValue(vertices);
    }
}
