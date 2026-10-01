package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.entity.ShadowGhost;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ShadowGhostRenderer extends EntityRenderer<ShadowGhost> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/shadow_ghost.png");
    private final ModelPart shell=SpiritVoxelParts.boxes(new float[]{-4,5,-4,8,8,8},new float[]{-4,-5,-2,8,10,4});
    private final ModelPart core=SpiritVoxelParts.boxes(new float[]{-2,7,-2,4,4,4},new float[]{-2,-2,-1,4,5,2});
    private final ModelPart eyes=SpiritVoxelParts.boxes(new float[]{-3,8,-4.1F,2,1,1},new float[]{1,8,-4.1F,2,1,1});
    private final ModelPart arms=SpiritVoxelParts.boxes(new float[]{-6,-4,-1,2,9,2},new float[]{4,-4,-1,2,9,2});
    private final ModelPart fringe=SpiritVoxelParts.boxes(new float[]{-4,-9,-2,2,4,4},new float[]{-1,-11,-2,2,6,4},new float[]{2,-8,-2,2,3,4});
    public ShadowGhostRenderer(EntityRendererProvider.Context context) {super(context);shadowRadius=.25F;}
    @Override public void render(ShadowGhost entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose();pose.translate(0,.75+Math.sin((entity.tickCount+partial)*.09)*.025,0);
        pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
        if(entity.deathTime>0) {float s=Math.max(0,1-(entity.deathTime+partial)/20F);pose.scale(s,s,s);}
        var mesh=buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        int overlay=OverlayTexture.pack(0,entity.hurtTime>0 || entity.deathTime>0);
        core.render(pose,mesh,light,overlay,0xCC9982BE);
        shell.render(pose,mesh,light,overlay,0x99FFFFFF);
        eyes.render(pose,mesh,LightTexture.FULL_BRIGHT,overlay,0xFFF3E8FF);
        arms.zRot=(float)Math.sin((entity.tickCount+partial)*.08)*.06F;
        arms.render(pose,mesh,light,overlay,0x88FFFFFF);
        fringe.xRot=(float)Math.sin((entity.tickCount+partial)*.07)*.1F;
        fringe.render(pose,mesh,light,overlay,0x66FFFFFF);
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(ShadowGhost entity) {return TEXTURE;}
}
