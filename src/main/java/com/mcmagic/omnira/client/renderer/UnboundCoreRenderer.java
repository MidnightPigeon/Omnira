package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class UnboundCoreRenderer {
    private static ModelResourceLocation part(String name){return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/unbound_core_"+name));}
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional e){e.register(part("body"));for(int i=0;i<3;i++)e.register(part("strand"+i));}
    public static void render(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int overlay,double time){
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees((float)(time*.3)));pose.translate(-.5,-.5,-.5);
        draw("body",stack,pose,buffers,overlay);pose.popPose();
        for(int i=0;i<3;i++){
            pose.pushPose();pose.translate(.5,.5,.5);
            pose.mulPose(Axis.ZP.rotationDegrees(i*57-42));
            pose.mulPose(Axis.XP.rotationDegrees((float)(i*39+Math.sin(time*.018+i)*12)));
            pose.mulPose(Axis.YP.rotationDegrees((float)(time*(i==1?-.65:.45)+i*73)));
            pose.translate(-.5,-.5,-.5);draw("strand"+i,stack,pose,buffers,overlay);pose.popPose();
        }
    }
    private static void draw(String name,ItemStack stack,PoseStack pose,MultiBufferSource buffers,int overlay){
        var mc=Minecraft.getInstance();mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(part(name)),stack,
                LightTexture.FULL_BRIGHT,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    private UnboundCoreRenderer(){}
}
