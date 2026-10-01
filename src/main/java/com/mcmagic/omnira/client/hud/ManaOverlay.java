package com.mcmagic.omnira.client.hud;

import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class ManaOverlay {
    private static final int WIDTH = 81;

    private ManaOverlay() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.player.isSpectator()
                || !minecraft.player.isAlive()) {
            return;
        }
        ManaState mana = minecraft.player.getData(ModAttachments.MANA);
        int hotbarLeft=graphics.guiWidth()/2-91;
        int width=Math.min(WIDTH,hotbarLeft-38);
        int x=hotbarLeft-30-width; // Leave room for the offhand slot.
        int y=graphics.guiHeight()-18;
        if(width<58) {
            width=WIDTH;
            x=Math.max(2,hotbarLeft);
            y=Math.max(2,graphics.guiHeight()-minecraft.gui.leftHeight-20);
            minecraft.gui.leftHeight+=18;
        }
        ManaBar.render(graphics,minecraft.font,mana,x,y,width);
    }
}
