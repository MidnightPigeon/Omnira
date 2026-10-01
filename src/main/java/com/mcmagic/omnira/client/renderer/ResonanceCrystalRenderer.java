package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;

public final class ResonanceCrystalRenderer extends BlockEntityWithoutLevelRenderer {
    public ResonanceCrystalRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());
    }
    public static ModelResourceLocation model(String part) {
        return CrystalGridRenderer.part("resonance_crystal",part);
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,
            MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        double time=mc.level==null?net.minecraft.Util.getMillis()/50.0:
                mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        draw("body",stack,pose,buffers,light,overlay);
        for(int i=0;i<4;i++) {
            double phase=(time*.008+i*.25)%1,angle=time*.018+i*Math.PI/2;
            pose.pushPose();
            pose.translate(.5+Math.cos(angle)*.28,.15+phase*.72,.5+Math.sin(angle)*.28);
            float size=(float)(.55*Math.sin(phase*Math.PI));
            pose.scale(size,size,size);
            pose.translate(-.5,-.5,-.5);
            draw("mote",stack,pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            pose.popPose();
        }
    }
    private static void draw(String part,ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(part)),stack,light,overlay,
                pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
}
