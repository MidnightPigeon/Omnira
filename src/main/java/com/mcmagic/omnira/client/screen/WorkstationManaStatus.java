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
        CrystalUi.title(graphics,font,title,88,y,100,color);
    }
    static void render(GuiGraphics graphics, Font font, Player player) {
        render(graphics,font,player,128);
    }
    static void render(GuiGraphics graphics,Font font,Player player,int y) {
        if (player == null) return;
        var mana = player.getData(ModAttachments.MANA);
        com.mcmagic.omnira.client.hud.ManaBar.render(graphics,font,mana,8,y,44,false);
    }
    static void progress(GuiGraphics graphics,int x,int y,float progress) {
        if(progress<=0) return;
        graphics.fill(x+3,y+12,x+37,y+15,0xFF28252F);
        int width=Math.min(34,(int)(34*progress));
        for(int i=0;i<width;i+=3)
            graphics.fill(x+3+i,y+12,x+3+Math.min(width,i+2),y+14,0xFFF1E5C9);
    }
}
