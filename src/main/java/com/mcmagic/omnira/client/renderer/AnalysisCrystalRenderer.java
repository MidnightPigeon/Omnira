package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;

public final class AnalysisCrystalRenderer extends BlockEntityWithoutLevelRenderer {
    public static final String[] PARTS={"shell","earth","water","fire","air"};
    public AnalysisCrystalRenderer() {super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model(String part) {return CrystalGridRenderer.part("analysis_crystal",part);}
    private void draw(String part,ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(part)),stack,light,overlay,
                pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        double t=(mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false))*.035;
        for(int i=0;i<4;i++) {
            double phase=i*1.57;
            pose.pushPose();
            pose.translate(.068*Math.sin(t*.83+phase)+.016*Math.sin(t*1.91+phase*2),
                    .125*Math.sin(t*.67+phase*1.7)+.02*Math.cos(t*1.37+phase),
                    .064*Math.cos(t*.93+phase)+.016*Math.sin(t*1.63-phase));
            draw(PARTS[i+1],stack,pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            pose.popPose();
        }
        draw("shell",stack,pose,buffers,light,overlay);
    }
}
