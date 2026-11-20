package com.mcmagic.omnira.client.hud;

import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class ManaOverlay {

    private ManaOverlay() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof com.mcmagic.omnira.client.screen.AnalysisArtisanTableScreen
                || minecraft.screen instanceof com.mcmagic.omnira.client.screen.ArcaneAssemblyTableScreen
                || minecraft.screen instanceof com.mcmagic.omnira.client.screen.CrystalProcessingTableScreen
                || minecraft.screen instanceof com.mcmagic.omnira.client.screen.WaymarkScreen || minecraft.screen instanceof com.mcmagic.omnira.client.screen.WaymarkDeleteScreen || minecraft.player == null || minecraft.options.hideGui || minecraft.player.isSpectator()
                || !minecraft.player.isAlive()) {
            return;
        }
        ManaState mana = minecraft.player.getData(ModAttachments.MANA);
        if(minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)return;
        var pos=ManaHudPosition.position(graphics.guiWidth(),graphics.guiHeight());
        ManaBar.render(graphics,minecraft.font,mana,pos.x(),pos.y(),ManaHudPosition.WIDTH);
    }
}
