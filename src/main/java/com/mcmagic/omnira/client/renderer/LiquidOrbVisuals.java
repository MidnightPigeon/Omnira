package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public final class LiquidOrbVisuals {
    public static void render(LiquidCrystalBallBlockEntity ball,PoseStack pose,MultiBufferSource buffers,double time,int light,int overlay) {
        render(ball.tank.getFluid(),ball.tank.getCapacity(),pose,buffers,time,light,overlay);
    }
    public static void render(net.neoforged.neoforge.fluids.FluidStack fluid,int capacity,PoseStack pose,MultiBufferSource buffers,double time,int light,int overlay) {
        var mc=Minecraft.getInstance();
        if(fluid.isEmpty()) {
            for(int i=0;i<6;i++) {
                double a=time*.025+i*Math.PI/3;
                pose.pushPose();pose.translate(Math.cos(a)*.19,Math.sin(a*1.3+i)*.14,Math.sin(a)*.19);
                pose.translate(.5,.5,.5);pose.scale(.45F,.45F,.45F);pose.translate(-.5,-.5,-.5);
                mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(CrystalBallRenderer.model("mote")),ItemStack.EMPTY,LightTexture.FULL_BRIGHT,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));pose.popPose();
            }
            return;
        }
        var extension=IClientFluidTypeExtensions.of(fluid.getFluid());
        var sprite=mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extension.getStillTexture(fluid));
        int color=extension.getTintColor(fluid);
        int alpha=color>>>24;if(alpha==0)alpha=255;
        float radius=(float)(.275*Math.cbrt(Math.min(1,(double)fluid.getAmount()/capacity)));
        var consumer=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        int glow=fluid.getFluidType().getLightLevel(fluid);int packed=LightTexture.pack(Math.max(glow,LightTexture.block(light)),LightTexture.sky(light));
        // Eight meridians and six latitude bands keep the liquid faceted rather than high-poly.
        for(int ring=0;ring<6;ring++)for(int segment=0;segment<8;segment++) {
            double lo=-Math.PI/2+ring*Math.PI/6,hi=lo+Math.PI/6;
            double a=segment*Math.PI/4,b=a+Math.PI/4;
            double[][] corners={{lo,a},{hi,a},{hi,b},{lo,b}};
            for(int i=0;i<4;i++) {
                double latitude=corners[i][0],longitude=corners[i][1];
                float nx=(float)(Math.cos(latitude)*Math.cos(longitude)),ny=(float)Math.sin(latitude),nz=(float)(Math.cos(latitude)*Math.sin(longitude));
                consumer.addVertex(pose.last(),.5F+radius*nx,.5F+radius*ny,.5F+radius*nz).setColor(color>>16&255,color>>8&255,color&255,alpha)
                        .setUv(sprite.getU((float)(longitude/(Math.PI*2))),sprite.getV((float)(.5-latitude/Math.PI)))
                        .setOverlay(overlay).setLight(packed).setNormal(pose.last(),nx,ny,nz);
            }
        }
    }
}
