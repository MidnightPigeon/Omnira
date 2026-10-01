package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.entity.ShadowMist;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ShadowMistRenderer extends EntityRenderer<ShadowMist> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/shadow_mist.png");
    private final net.minecraft.client.model.geom.ModelPart cloud=SpiritVoxelParts.sphere(7);
    public ShadowMistRenderer(EntityRendererProvider.Context context) {super(context);}
    @Override public void render(ShadowMist entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        float d=entity.diameter(partial),alpha=entity.opacity(partial);
        pose.pushPose();pose.translate(0,d*.325,0);pose.scale(d*16/14,d*.65F*16/14,d*16/14);
        cloud.render(pose,buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),light,OverlayTexture.NO_OVERLAY,
                ((int)(alpha*150)<<24)|0xFFFFFF);
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(ShadowMist entity) {return TEXTURE;}
}
