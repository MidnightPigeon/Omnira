package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.paradox.ParadoxContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;

public final class ParadoxMaterialRenderer extends BlockEntityWithoutLevelRenderer {
    public static ModelResourceLocation model(String part){return CrystalGridRenderer.part("paradox_material",part);}
    public ParadoxMaterialRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();double t=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        boolean mystery=stack.is(ParadoxContent.MYSTERY.get());
        pose.pushPose();pose.translate(.5,.5,.5);
        if(mystery){
            pose.mulPose(Axis.YP.rotationDegrees((float)(t*.6%360)));
            pose.mulPose(Axis.ZP.rotationDegrees((float)(Math.sin(t*.02)*8)));
        }
        draw(stack,mystery?"mystery":"flow",pose,buffers,light,overlay);
        if(mystery)for(int i=0;i<10;i++){
            double a=t*.035+i*2.39996;
            pose.pushPose();pose.translate(Math.cos(a)*.34,Math.sin(a*1.7)*.24,Math.sin(a)*.34);
            draw(stack,"grain",pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
        }
        pose.popPose();
    }
    private static void draw(ItemStack stack,String part,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(-.5,-.5,-.5);
        Minecraft.getInstance().getItemRenderer().renderModelLists(Minecraft.getInstance().getModelManager().getModel(model(part)),
                stack,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));pose.popPose();
    }
}
