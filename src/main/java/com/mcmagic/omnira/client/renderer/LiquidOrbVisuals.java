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
            var model=mc.getModelManager().getModel(CrystalBallRenderer.model("mote"));
            var consumer=buffers.getBuffer(CrystalGlassLayer.ATLAS);
            for(int i=0;i<6;i++) {
                double a=time*.025+i*Math.PI/3;
                pose.pushPose();pose.translate(Math.cos(a)*.19,Math.sin(a*1.3+i)*.14,Math.sin(a)*.19);
                pose.translate(.5,.5,.5);pose.scale(.45F,.45F,.45F);pose.translate(-.5,-.5,-.5);
                StaticModelQuads.draw(model,pose,consumer,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
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
        for(int i=0;i<LiquidOrbMesh.size();i++) {
            var v=LiquidOrbMesh.vertex(i);
            consumer.addVertex(pose.last(),.5F+radius*v.x(),.5F+radius*v.y(),.5F+radius*v.z()).setColor(color>>16&255,color>>8&255,color&255,alpha)
                    .setUv(sprite.getU(v.u()),sprite.getV(v.v()))
                    .setOverlay(overlay).setLight(packed).setNormal(pose.last(),v.x(),v.y(),v.z());
        }
    }
}
