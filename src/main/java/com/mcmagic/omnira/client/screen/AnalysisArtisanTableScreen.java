package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.AnalysisArtisanTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class AnalysisArtisanTableScreen extends ResponsiveContainerScreen<AnalysisArtisanTableMenu> {
    private static final ResourceLocation TEXTURE = CrystalWorkstationBackground.texture("workstation_analysis");
    private Button analyze;
    public AnalysisArtisanTableScreen(AnalysisArtisanTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176; imageHeight = 240; inventoryLabelY = 146;
    }
    @Override protected void init() {
        super.init();
        analyze = addRenderableWidget(new CrystalActionButton(leftPos+124,topPos+124,40,18,Component.translatable("button.omnira.analysis_artisan_table.analyze"), button -> {
            if (minecraft != null && minecraft.gameMode != null && menu.canAnalyze()) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }));
        analyze.active = menu.canAnalyze() && !menu.isWorking();
    }
    @Override protected void containerTick() { super.containerTick(); analyze.active = menu.canAnalyze() && !menu.isWorking(); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        CrystalWorkstationBackground.draw(graphics,TEXTURE,leftPos,topPos,"analysis",CrystalUi.time(partialTick),menu.workProgress(),menu.isWorking());
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        CrystalWorkstationBackground.labels(graphics,font,title,playerInventoryTitle);
        WorkstationManaStatus.render(graphics,font,minecraft == null ? null : minecraft.player);
    }
    @Override protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(graphics,mouseX,mouseY,partialTick);
        WorkstationManaStatus.progress(graphics,leftPos+124,topPos+124,menu.workProgress());
        renderTooltip(graphics,mouseX,mouseY);
    }
}
