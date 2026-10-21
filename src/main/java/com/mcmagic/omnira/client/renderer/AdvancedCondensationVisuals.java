package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

/** Recipe-colored core and outward white waves; independent from resonance item particles. */
public final class AdvancedCondensationVisuals {
    public static ModelResourceLocation model(String part) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/advanced_condensation_"+part));
    }
    public static void render(double time,PoseStack pose,MultiBufferSource buffers,int overlay) {
        var mc=Minecraft.getInstance();var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        pose.pushPose();
        pose.translate(.5,.405+Math.sin(time*.04)*.018,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(time%240)*1.5F));
        pose.translate(-.5,-.5,-.5);
        ParticleModelQuads.tinted(ParticleModelQuads.collect(mc.getModelManager().getModel(model("core"))),pose,out,
                LightTexture.FULL_BRIGHT,overlay,0xFFFFFF,1);
        pose.popPose();
        var ripple=ParticleModelQuads.collect(mc.getModelManager().getModel(model("ripple")));
        for(int wave=0;wave<3;wave++) {
            double phase=(time/90+wave/3D)%1;
            float radius=(float)(.2+phase*.7),alpha=(float)Math.sin(phase*Math.PI);
            pose.pushPose();
            pose.translate(.5,.34+wave*.06,.5);
            pose.mulPose(Axis.YP.rotationDegrees((float)(time%360)*.35F+wave*30));
            pose.scale(radius,.18F,radius);
            pose.translate(-.5,-.5,-.5);
            ParticleModelQuads.tinted(ripple,pose,out,LightTexture.FULL_BRIGHT,overlay,0xFFFFFF,alpha);
            pose.popPose();
        }
    }
    private AdvancedCondensationVisuals() {}
}
