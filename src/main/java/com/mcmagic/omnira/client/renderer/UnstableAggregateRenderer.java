package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;

public final class UnstableAggregateRenderer extends BlockEntityWithoutLevelRenderer {
    private static int storageDepth;
    public static net.minecraft.client.resources.model.ModelResourceLocation model(String part){
        return net.minecraft.client.resources.model.ModelResourceLocation.standalone(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","item/unstable_spacetime_aggregate/"+part));
    }
    public UnstableAggregateRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static void inStorage(Runnable draw){storageDepth++;try{draw.run();}finally{storageDepth--;}}
    private static int flow(ItemStack stack,ItemDisplayContext context){
        if(storageDepth>0)return -1;
        var player=Minecraft.getInstance().player;
        if(context==ItemDisplayContext.GUI && player!=null){
            if(player.containerMenu.getCarried()==stack)return 0;
            for(var slot:player.containerMenu.slots)if(slot.getItem()==stack)return slot.container instanceof Inventory?1:-1;
        }
        return context==ItemDisplayContext.FIXED || context==ItemDisplayContext.NONE?-1:1;
    }
    private static void draw(String part,PoseStack pose,MultiBufferSource buffers,int overlay){
        var mc=Minecraft.getInstance();
        var layer=part.equals("body")?Sheets.cutoutBlockSheet():CrystalGlassLayer.ATLAS;
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(part)),
                ItemStack.EMPTY,LightTexture.FULL_BRIGHT,overlay,pose,buffers.getBuffer(layer));
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        int flow=flow(stack,context);
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees((float)(time*.65)));pose.translate(-.5,-.5,-.5);
        draw("body",pose,buffers,overlay);
        pose.pushPose();pose.translate(.5,.5,.5);float pulse=1+(float)Math.sin(time*.07)*.025F;pose.scale(pulse,pulse,pulse);pose.translate(-.5,-.5,-.5);
        draw("halo",pose,buffers,overlay);pose.popPose();pose.popPose();
        for(int i=0;i<8;i++){
            double phase=flow==0?.45:((time/32+i*.137)%1),progress=flow<0?1-phase:phase;
            double y=1-2*(i+.5)/8,angle=i*2.3999632297+time*.012,radius=.26+.22*progress,r=Math.sqrt(1-y*y);
            pose.pushPose();pose.translate(.5+Math.cos(angle)*r*radius,.5+y*radius,.5+Math.sin(angle)*r*radius);
            float scale=(float)(.25+.75*Math.sin(Math.PI*phase));pose.scale(scale,scale,scale);draw("grain",pose,buffers,overlay);pose.popPose();
        }
    }
}
