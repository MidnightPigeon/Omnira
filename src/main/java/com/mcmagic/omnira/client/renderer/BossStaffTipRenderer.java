package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Shared geometry and animation for loose tips and assembled staffs. */
public final class BossStaffTipRenderer extends BlockEntityWithoutLevelRenderer {
    // Authored tips use (8,12,8); the shared staff socket meets the reinforcement at (7.5,13,7.5).
    public static final float MOUNT_WIDTH = .7F;
    public static final float MOUNT_HEIGHT = .8F;
    public static final double STORM_CENTER = 18.5;
    public BossStaffTipRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static boolean supports(ItemStack stack){return stack.is(com.mcmagic.omnira.registry.ModItems.STORM_TIP.get())||stack.is(com.mcmagic.omnira.registry.ModItems.ABYSS_TIP.get());}
    public static void renderTip(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay,double time){
        pose.pushPose();
        pose.translate(7.5/16,13.0/16,7.5/16);
        pose.scale(MOUNT_WIDTH,MOUNT_HEIGHT,MOUNT_WIDTH);
        pose.translate(-.5,-12.0/16,-.5);
        boolean storm=stack.is(com.mcmagic.omnira.registry.ModItems.STORM_TIP.get());
        String name=storm?"storm_tip":"abyss_tip";var mc=Minecraft.getInstance();
        var model=mc.getModelManager().getModel(ModularStaffRenderer.part(ResourceLocation.parse("omnira:item/staff_parts/"+name)));
        mc.getItemRenderer().renderModelLists(model,stack,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
        if(storm){
            // Keep the socket still; the detached wind ring revolves around the fixed crystal.
            pose.pushPose();pose.translate(.5,STORM_CENTER/16,.5);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)(time*1.5%360)));
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
            pose.translate(-.5,-20.0/16,-.5);
            var ring=mc.getModelManager().getModel(ModularStaffRenderer.part(ResourceLocation.parse("omnira:item/staff_parts/storm_tip_ring")));
            mc.getItemRenderer().renderModelLists(ring,stack,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
            pose.popPose();
        }
        pose.pushPose();pose.translate(0,storm?(STORM_CENTER-20)/16:Math.sin(time*.035)*.008,0);
        var core=mc.getModelManager().getModel(ModularStaffRenderer.part(ResourceLocation.parse("omnira:item/staff_parts/"+name+"_core")));
        mc.getItemRenderer().renderModelLists(core,stack,LightTexture.FULL_BRIGHT,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));pose.popPose();
        var out=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<(storm?6:3);i++){
            double phase=i*2.399963,angle=time*(storm?.035:.018)+i*Math.PI*2/(storm?6:3);
            float x,y,z;
            if(storm){
                // Independent waves modulate the orbit rather than rotating one rigid particle frame.
                angle+=Math.sin(time*.024+phase)*.18;
                double radius=.31+Math.sin(time*.031+phase)*.025;
                x=.5F+(float)(Math.cos(angle)*radius);
                y=(float)(STORM_CENTER/16)+(float)Math.sin(time*.047+phase)*.045F;
                z=.5F+(float)(Math.sin(angle)*radius);
            }else{
                x=.5F+(float)Math.cos(angle)*.16F;
                y=1.23F+(float)Math.sin(angle)*.10F;
                z=.5F+(float)Math.sin(angle*1.4)*.07F;
            }
            float alpha=storm?.65F+(float)Math.sin(time*.039+phase)*.12F:.7F;
            StellarVisuals.cube(pose,out,x,y,z,.018F,storm?0xB6F5FF:0xCE9BFA,alpha);
        }
        pose.popPose();
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();pose.translate(0,-.65,0);renderTip(stack,pose,buffers,light,overlay,time);pose.popPose();
    }
}
