package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.spacetime.TemporalAmber;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class TemporalAmberRenderer extends EntityRenderer<TemporalAmber> {
    public static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/solid_white.png");
    private final net.minecraft.client.model.geom.ModelPart shell=SpiritVoxelParts.sphere(6);
    private final net.minecraft.client.model.geom.ModelPart hourglass=SpiritVoxelParts.boxes(
            new float[]{-3,-4,-1,6,1,2},new float[]{-2,-3,-1,4,1,2},new float[]{-1,-2,-1,2,4,2},
            new float[]{-2,2,-1,4,1,2},new float[]{-3,3,-1,6,1,2});
    public TemporalAmberRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public void render(TemporalAmber entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        float time=entity.tickCount+partial;
        pose.pushPose();pose.translate(0,.5+Math.sin(time*.04)*.025,0);pose.mulPose(Axis.YP.rotationDegrees(time*.45F));
        hourglass.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0xFFFFEDB0);
        pose.scale(1,1.25F,1);
        shell.render(pose,buffers.getBuffer(CrystalGlassLayer.AMBER),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0xA6EBA837);
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(TemporalAmber entity){return TEXTURE;}
}
