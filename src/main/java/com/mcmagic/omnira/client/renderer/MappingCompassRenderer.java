package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

public final class MappingCompassRenderer extends BlockEntityWithoutLevelRenderer {
    public MappingCompassRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    private static void draw(java.util.List<MappingCompassGeometry.Quad> mesh,PoseStack pose,VertexConsumer out){
        var matrix=pose.last().pose();
        for(var q:mesh)for(int i=0;i<12;i+=3)out.addVertex(matrix,q.xyz()[i],q.xyz()[i+1],q.xyz()[i+2])
                .setColor((q.color()>>16&255)/255F,(q.color()>>8&255)/255F,(q.color()&255)/255F,q.alpha());
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var out=buffers.getBuffer(CrystalGlassLayer.COLOR);draw(MappingCompassGeometry.PLATE,pose,out);
        var mc=Minecraft.getInstance();
        boolean held=context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND||context==ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                ||context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        boolean channel=held&&mc.player!=null&&mc.player.isUsingItem()&&ItemStack.isSameItemSameComponents(stack,mc.player.getUseItem());
        double time=mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        double elapsed=channel?mc.player.getTicksUsingItem()+mc.getTimer().getGameTimeDeltaPartialTick(false):0;
        pose.pushPose();pose.translate(.5,.5,0);pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(channel?(float)(elapsed*32):MappingCompassGeometry.idleAngle(time)));pose.translate(-.5,-.5,0);
        draw(MappingCompassGeometry.NEEDLE,pose,out);pose.popPose();
        if(channel)for(int i=0;i<6;i++){
            double angle=elapsed*.3+i*Math.PI/3;
            StellarVisuals.cube(pose,out,.5F+(float)Math.cos(angle)*.3F,.5F+(float)Math.sin(angle)*.3F,.65F,.015F,0xE4FBFF,.8F);
        }
        else for(int i=0;i<2;i++){
            double angle=time*.02+i*Math.PI;
            StellarVisuals.cube(pose,out,.5F+(float)Math.cos(angle)*.29F,.5F+(float)Math.sin(angle)*.29F,.62F,1F/64,i==0?0xDBF8FF:0x5568CA,.65F);
        }
    }
}
