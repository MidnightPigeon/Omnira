package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.paradox.ParadoxContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

/** Stable frames stay still; their suspended contents and bounded motes remain alive. */
public final class ParadoxSourceRenderer extends BlockEntityWithoutLevelRenderer {
    public ParadoxSourceRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();pose.translate(.5,.5,.5);
        boolean stable=stack.is(ParadoxContent.STABLE_SOURCE.get())||stack.is(ParadoxContent.STABLE_AGGREGATE.get());
        if(stable)draw(ParadoxMaterialRenderer.model("frame"),pose,buffers,light,overlay,false);
        if(stack.is(ParadoxContent.GENESIS.get()))grains(true,true,time,pose,buffers,overlay,1);
        else {
            pose.pushPose();if(stable)pose.scale(.63F,.63F,.63F);
            core(stack,time,pose,buffers,light,overlay);pose.popPose();
            grains(stack.is(ParadoxContent.STABLE_AGGREGATE.get()),stable,time,pose,buffers,overlay,1);
        }
        pose.popPose();
    }
    /** Origin is the chamber planting site's center; deliberately excludes the packaging. */
    public static void core(ItemStack stack,double time,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(0,Math.sin(time*.045)*.025,0);pose.mulPose(Axis.YP.rotationDegrees((float)(time*.4%360)));
        if(stack.is(ParadoxContent.STABLE_AGGREGATE.get())){
            draw(UnstableAggregateRenderer.model("body"),pose,buffers,LightTexture.FULL_BRIGHT,overlay,true);
            draw(UnstableAggregateRenderer.model("halo"),pose,buffers,LightTexture.FULL_BRIGHT,overlay,false);
        }else{
            float pulse=(float)Math.sin(time*.055)*.055F;pose.scale(1+pulse,1-pulse,1+pulse);
            draw(ParadoxMaterialRenderer.model("source"),pose,buffers,light,overlay,true);
        }
        pose.popPose();
    }
    public static void grains(boolean genesis,boolean orbit,double time,PoseStack pose,MultiBufferSource buffers,int overlay,float amount){
        int count=genesis?5:8;
        for(int i=0;i<count;i++){
            double phase=(time/36+i*.137)%1,angle=time*.027+i*2.39996;
            double radius=(genesis?.22:.30)+(orbit?Math.sin(angle*1.3)*.025:phase*.22);
            pose.pushPose();pose.translate(Math.cos(angle)*radius,Math.sin(angle*1.7)*radius*.6,Math.sin(angle)*radius);
            float scale=amount*(orbit?1:(float)Math.sin(phase*Math.PI));pose.scale(scale,scale,scale);
            draw(ParadoxMaterialRenderer.model(genesis?"genesis_grain":"grain"),pose,buffers,LightTexture.FULL_BRIGHT,overlay,true);pose.popPose();
        }
    }
    private static void draw(net.minecraft.client.resources.model.ModelResourceLocation model,PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean opaque){
        pose.pushPose();pose.translate(-.5,-.5,-.5);var mc=Minecraft.getInstance();
        var layer=opaque?Sheets.cutoutBlockSheet():CrystalGlassLayer.ATLAS;
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model),ItemStack.EMPTY,light,overlay,pose,CrystalGlassLayer.contents(buffers).getBuffer(layer));pose.popPose();
    }
}
