package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The placed block uses world particles; an item needs its own animated grains. */
public final class TimeWarpPointItemRenderer extends BlockEntityWithoutLevelRenderer {
    public TimeWarpPointItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model(String part){
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/time_warp_point/"+part));
    }
    private static void draw(String part,PoseStack pose,MultiBufferSource buffers,int overlay,int color){
        var baked=Minecraft.getInstance().getModelManager().getModel(model(part));
        var buffer=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        float red=((color>>16)&255)/255F,green=((color>>8)&255)/255F,blue=(color&255)/255F;
        for(var quad:baked.getQuads(null,null,RandomSource.create(42)))
            buffer.putBulkData(pose.last(),quad,red,green,blue,1,LightTexture.FULL_BRIGHT,overlay);
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,
                                       MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        double time=mc.level==null?net.minecraft.Util.getMillis()/50.0
                :mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();pose.translate(.5,.5,.5);pose.scale(1.25F,1.25F,1.25F);pose.translate(-.5,-.5,-.5);
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees((float)(time*.8)));
        pose.translate(-.5,-.5,-.5);draw("body",pose,buffers,overlay,0xFFFFFF);pose.popPose();
        for(int i=0;i<10;i++){
            double phase=(time/30+i*.137)%1;
            double y=1-2*(i+.5)/10,angle=i*2.3999632297+time*.045;
            double radius=.29+.16*phase,horizontal=Math.sqrt(1-y*y);
            pose.pushPose();pose.translate(.5+Math.cos(angle)*horizontal*radius,.5+y*radius,
                    .5+Math.sin(angle)*horizontal*radius);
            float scale=(float)(.65+.4*Math.sin(Math.PI*phase));pose.scale(scale,scale,scale);
            draw("grain",pose,buffers,overlay,i%4==0?0xE8F8ED:i%3==0?0x9BDFC0:0x65C785);
            pose.popPose();
        }
        pose.popPose();
    }
}
