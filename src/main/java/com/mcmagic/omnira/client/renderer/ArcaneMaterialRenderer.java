package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;

public final class ArcaneMaterialRenderer extends BlockEntityWithoutLevelRenderer {
    public ArcaneMaterialRenderer() {super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model(boolean knot) {return CrystalGridRenderer.part(knot?"spacetime_knot":"shadow_mist",knot?"band":"cloud");}
    public static ModelResourceLocation spark() {return CrystalGridRenderer.part("spacetime_knot","spark");}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();boolean knot=stack.is(ModItems.SPACETIME_KNOT.get());
        double t=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();pose.translate(.5,.5,.5);
        if(knot) {
            pose.mulPose(Axis.YP.rotationDegrees(context==ItemDisplayContext.GUI?(float)(Math.sin(t*.025)*18):(float)(t*1.2%360)));
            pose.mulPose(Axis.ZP.rotationDegrees((float)(Math.sin(t*.011)*4)));
        } else pose.mulPose(Axis.YP.rotationDegrees((float)(t*.3%360)));
        draw(stack,pose,buffers,knot?LightTexture.FULL_BRIGHT:light,overlay,knot);
        if(knot) for(int i=0;i<8;i++) {
            double u=t*.06+i*Math.PI/4;
            pose.pushPose();pose.translate(5.3*Math.sin(u)/16-.5,2.25*Math.sin(2*u)/16-.5,.95*Math.cos(u)/16-.5);
            mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(spark()),stack,LightTexture.FULL_BRIGHT,overlay,
                    pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));pose.popPose();
        }
        if(!knot) for(int i=0;i<5;i++) {
            double phase=(t*.01+i*.2)%1,angle=i*2.4;
            pose.pushPose();pose.translate(Math.cos(angle)*(.23+phase*.1),phase*.6-.25,Math.sin(angle)*(.23+phase*.1));
            float s=(float)((1-phase)*.1);pose.scale(s,s,s);draw(stack,pose,buffers,light,overlay,false);pose.popPose();
        }
        pose.popPose();
    }
    private void draw(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean knot) {
        var mc=Minecraft.getInstance();pose.pushPose();pose.translate(-.5,-.5,-.5);
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(knot)),stack,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
        pose.popPose();
    }
}
