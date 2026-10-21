package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class WeaverRunes {
    private static final Component[] GLYPHS=new Component[26];
    static {for(int i=0;i<26;i++)GLYPHS[i]=Component.literal(String.valueOf((char)('a'+i))).withStyle(s->s.withFont(ResourceLocation.withDefaultNamespace("alt")));}
    public static void draw(int glyph,PoseStack pose,MultiBufferSource buffers,float scale){
        draw(glyph,pose,buffers,scale,1);
    }
    public static void draw(int glyph,PoseStack pose,MultiBufferSource buffers,float scale,float alpha){
        if(alpha<=4/255F)return;
        var mc=Minecraft.getInstance();pose.pushPose();pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(scale,-scale,scale);
        var text=GLYPHS[Math.floorMod(glyph,26)];mc.font.drawInBatch(text,-mc.font.width(text)/2F,-4,((int)(Math.clamp(alpha,0,1)*255)<<24)|0xEBFAFF,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);pose.popPose();
    }
    private WeaverRunes(){}
}
