package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

final class WorkstationManaStatus {
    private static final net.minecraft.resources.ResourceLocation TITLE_TEXTURE=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "omnira","textures/gui/container/analysis_artisan_table.png");
    private WorkstationManaStatus() {}
    static void title(GuiGraphics graphics,Font font,Component title,int y,int color) {
        // Reuse the approved scroll and its surrounding background to replace
        // older baked-in banners without changing workstation layouts.
        graphics.blit(TITLE_TEXTURE,8,y-8,8,5,160,27,176,240);
        titleText(graphics,font,title,y,color);
    }
    static void titleText(GuiGraphics graphics,Font font,Component title,int y,int color) {
        float scale=Math.min(1,100F/Math.max(1,font.width(title)));
        graphics.pose().pushPose();graphics.pose().translate(88,y,0);graphics.pose().scale(scale,scale,1);
        graphics.drawString(font,title,-font.width(title)/2,0,color,false);graphics.pose().popPose();
    }
    static void render(GuiGraphics graphics, Font font, Player player) {
        render(graphics,font,player,128);
    }
    static void render(GuiGraphics graphics,Font font,Player player,int y) {
        if (player == null) return;
        var mana = player.getData(ModAttachments.MANA);
        com.mcmagic.omnira.client.hud.ManaBar.render(graphics,font,mana,8,y,44);
    }
    static void progress(GuiGraphics graphics,int x,int y,float progress) {
        if(progress<=0) return;
        graphics.fill(x+2,y+14,x+38,y+17,0xFF28252F);
        int width=Math.min(36,(int)(36*progress));
        for(int i=0;i<width;i+=3)
            graphics.fill(x+2+i,y+14,x+2+Math.min(width,i+2),y+16,0xFFF1E5C9);
    }
}
