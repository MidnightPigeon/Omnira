package com.mcmagic.omnira.client.guide;
import com.mcmagic.omnira.client.renderer.AdvancedForgeRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** The construction diagram reads the same layout used by formation validation. */
public final class AdvancedForgeScene {
    public static void render(GuiGraphics g,int x,int y,int ticks){
        render(g,x,y,ticks,0,0,true);
    }
    public static void render(GuiGraphics g,int x,int y,int ticks,float yaw,float pitch,boolean construction){
        if(construction){
            ConstructionPreview.draw(g,x,y,ticks,"advanced_assembly_table",yaw,pitch,-1,-1);
            return;
        }
        var mc=Minecraft.getInstance();g.flush();RenderSystem.enableDepthTest();var p=g.pose();var buffers=mc.renderBuffers().bufferSource();
        p.pushPose();p.translate(x+62,y+83,180);p.scale(25,-25,25);p.mulPose(Axis.XP.rotationDegrees(25+pitch));p.mulPose(Axis.YP.rotationDegrees(-35+yaw+(float)Math.sin(ticks*.007)*20));
        AdvancedForgeRenderer.renderLocal(null,0,ticks,p,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        buffers.endBatch();p.popPose();RenderSystem.disableDepthTest();
    }
}
