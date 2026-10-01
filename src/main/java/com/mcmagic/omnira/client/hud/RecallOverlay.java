package com.mcmagic.omnira.client.hud;

import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.travel.RecallTravel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class RecallOverlay {
    public static void render(GuiGraphics g,DeltaTracker tracker) {
        var mc=Minecraft.getInstance();if(mc.player==null || mc.options.hideGui) return;
        int rabbit=mc.player.getData(ModAttachments.RABBIT_TICKS);
        int remaining=rabbit>0?rabbit:mc.player.getData(ModAttachments.RECALL_TICKS);if(remaining<=0) return;
        int x=g.guiWidth()/2-60,y=g.guiHeight()-83;
        g.fill(x,y,x+120,y+7,0xFF343139);g.fill(x+1,y+1,x+119,y+6,0xFFCED7DC);
        g.fill(x+2,y+2,x+118,y+5,0xFF434951);
        g.fill(x+2,y+2,x+2+(116*(RecallTravel.DURATION-remaining)/RecallTravel.DURATION),y+5,0xFFDDF6FA);
        g.drawCenteredString(mc.font,Component.translatable(rabbit>0?"gui.omnira.rabbit.channel":"gui.omnira.recall.channel",String.format(java.util.Locale.ROOT,"%.1f",remaining/20F)),x+60,y-12,0xFFE9F8FF);
    }
}
