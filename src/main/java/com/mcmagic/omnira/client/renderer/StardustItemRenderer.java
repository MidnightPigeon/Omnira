package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class StardustItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ModelResourceLocation MODEL=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/stardust_powder"));
    public static final ModelResourceLocation MOTE=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/stardust_mote"));
    public StardustItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(MODEL),stack,LightTexture.FULL_BRIGHT,overlay,
                pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
        double time=mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        for(int i=0;i<5;i++){
            double phase=time*.035+i*Math.PI*2/5;
            pose.pushPose();pose.translate(Math.cos(phase)*.26,.13+Math.sin(phase*1.7+i)*.07,Math.sin(phase)*.22);
            pose.translate(.5,.5,.5);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)(phase*50)));pose.translate(-.5,-.5,-.5);
            mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(MOTE),stack,LightTexture.FULL_BRIGHT,overlay,
                    pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));pose.popPose();
        }
    }
}
