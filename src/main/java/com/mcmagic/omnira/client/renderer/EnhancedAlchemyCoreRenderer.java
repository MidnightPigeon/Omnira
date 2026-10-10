package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class EnhancedAlchemyCoreRenderer {
    public static final ModelResourceLocation FRAME=model("enhanced_alchemical_energy_core");
    public static final ModelResourceLocation HEART=model("enhanced_alchemical_energy_core_heart");
    public static final ModelResourceLocation TIME=model("enhanced_alchemical_energy_core_time_mote");
    public static final ModelResourceLocation SPACE=model("enhanced_alchemical_energy_core_space_mote");
    private static ModelResourceLocation model(String id){return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/"+id));}
    private static void draw(ModelResourceLocation model,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model),ItemStack.EMPTY,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    public static void render(PoseStack pose,MultiBufferSource buffers,int light,int overlay,double time){
        draw(FRAME,pose,buffers,light,overlay);
        pose.pushPose();pose.translate(.5,.5+Math.sin(time*.045)*.012,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(time*1.2%360)));pose.translate(-.5,-.5,-.5);
        draw(HEART,pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
        for(int i=0;i<6;i++){
            double angle=time*.028+i*Math.PI/3;
            pose.pushPose();pose.translate(.5+.25*Math.cos(angle),.5+.15*Math.sin(angle*2),.5+.25*Math.sin(angle));
            pose.mulPose(Axis.YP.rotationDegrees((float)(time*2+i*60)));pose.translate(-.5,-.5,-.5);
            draw(i%2==0?TIME:SPACE,pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
        }
    }
    private EnhancedAlchemyCoreRenderer(){}
}
