package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Recipe-colored core and outward white waves; independent from resonance item particles. */
public final class AdvancedCondensationVisuals {
    public static ModelResourceLocation model(String part) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/advanced_condensation_"+part));
    }
    public static void render(double time,PoseStack pose,MultiBufferSource buffers,int overlay) {
        pose.pushPose();
        pose.translate(.5,.405+Math.sin(time*.04)*.018,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(time%240)*1.5F));
        pose.translate(-.5,-.5,-.5);
        draw("core",pose,buffers,overlay,1);
        pose.popPose();
        for(int wave=0;wave<3;wave++) {
            double phase=(time/90+wave/3D)%1;
            float radius=(float)(.2+phase*.7),alpha=(float)Math.sin(phase*Math.PI);
            pose.pushPose();
            pose.translate(.5,.34+wave*.06,.5);
            pose.mulPose(Axis.YP.rotationDegrees((float)(time%360)*.35F+wave*30));
            pose.scale(radius,.18F,radius);
            pose.translate(-.5,-.5,-.5);
            draw("ripple",pose,buffers,overlay,alpha);
            pose.popPose();
        }
    }
    private static void draw(String part,PoseStack pose,MultiBufferSource buffers,int overlay,float alpha) {
        var baked=Minecraft.getInstance().getModelManager().getModel(model(part));
        var buffer=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        // These authored models have no culled quads; texture alpha remains in the shader.
        for(var quad:baked.getQuads(null,null,RandomSource.create(42)))
            buffer.putBulkData(pose.last(),quad,1,1,1,alpha,LightTexture.FULL_BRIGHT,overlay);
    }
    private AdvancedCondensationVisuals() {}
}
