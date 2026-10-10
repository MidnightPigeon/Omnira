package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class InfusedStaffTipRenderer extends BlockEntityWithoutLevelRenderer {
    private final boolean spacetime;
    public InfusedStaffTipRenderer() {
        this(false);
    }
    public InfusedStaffTipRenderer(boolean spacetime) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());
        this.spacetime=spacetime;
    }
    public static void motes(PoseStack pose,MultiBufferSource buffers,int overlay,double y,double time) {
        var mc=Minecraft.getInstance();
        var model=mc.getModelManager().getModel(CrystalGridRenderer.part("infused_spiritual_crystal","mote"));
        for(int i=0;i<4;i++) {
            double angle=time*.04+i*Math.PI/2;
            pose.pushPose();
            pose.translate(7.5/16+Math.cos(angle)*.23,y+Math.sin(angle*1.7)*.25,7.5/16+Math.sin(angle)*.23);
            pose.scale(.65F,.65F,.65F);
            mc.getItemRenderer().renderModelLists(model,ItemStack.EMPTY,LightTexture.FULL_BRIGHT,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
            pose.popPose();
        }
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        var model=mc.getModelManager().getModel(ModularStaffRenderer.part(ResourceLocation.fromNamespaceAndPath("omnira","item/staff_parts/"+(spacetime?"spacetime_tip_body":"infused_tip_body"))));
        mc.getItemRenderer().renderModelLists(model,stack,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
        if(!spacetime)motes(pose,buffers,overlay,.5,mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false));
    }
}
