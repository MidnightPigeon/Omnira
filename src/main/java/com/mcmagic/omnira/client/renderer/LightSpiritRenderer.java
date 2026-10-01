package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.entity.LightSpirit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class LightSpiritRenderer extends EntityRenderer<LightSpirit> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("omnira", "textures/entity/light_spirit.png");
    private final ModelPart shell=SpiritVoxelParts.sphere(5);
    private final ModelPart core=SpiritVoxelParts.sphere(2.6F);
    public LightSpiritRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = .2F; }

    @Override public void render(LightSpirit spirit, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, .325 + Math.sin((spirit.tickCount + partial) * .09) * .02, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
        float pulse = 1 + (float)Math.sin((spirit.tickCount + partial) * .065) * .05F;
        if (spirit.deathTime > 0) {
            float fade=Math.max(0,1-(spirit.deathTime+partial)/20F);pose.scale(fade,fade,fade);
        }
        var mesh = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        int overlay = OverlayTexture.pack(0, spirit.hurtTime > 0 || spirit.deathTime > 0);
        int glow = LightTexture.pack(Math.max(10, LightTexture.block(light)), LightTexture.sky(light));
        core.render(pose,mesh,glow,overlay,0xFFF3D677);
        pose.scale(pulse,pulse,pulse);
        shell.render(pose,mesh,glow,overlay,0x78FFFFFF);
        pose.popPose();
        super.render(spirit, yaw, partial, pose, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(LightSpirit spirit) { return TEXTURE; }
}
