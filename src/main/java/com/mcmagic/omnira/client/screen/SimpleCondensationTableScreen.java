package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.SimpleCondensationTableMenu;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class SimpleCondensationTableScreen extends AbstractContainerScreen<SimpleCondensationTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("omnira", "textures/gui/container/simple_condensation_table.png");
    private static final ResourceLocation VANILLA = ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private Button condense;
    public SimpleCondensationTableScreen(SimpleCondensationTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176; imageHeight = 240; inventoryLabelY = 146;
    }
    @Override protected void init() {
        super.init();
        condense = addRenderableWidget(Button.builder(Component.translatable("button.omnira.simple_condensation_table.condense"), button -> {
            if (minecraft != null && minecraft.gameMode != null && menu.canCondense()) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 124, topPos + 126, 40, 18).build());
        condense.active = menu.canCondense() && !menu.isWorking();
    }
    @Override protected void containerTick() { super.containerTick(); condense.active = menu.canCondense() && !menu.isWorking(); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE,leftPos,topPos,0,0,176,240,176,240);
        graphics.blit(VANILLA,leftPos+79,topPos+69,7,83,18,18,256,256);
        graphics.blit(VANILLA,leftPos+7,topPos+157,7,83,162,76,256,256);
        CrystalProcessingTableScreen.renderVanillaFrame(graphics,leftPos,topPos,imageWidth,imageHeight);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        WorkstationManaStatus.title(graphics,font,title,13,0xFF3A2416);
        graphics.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFF404040,false);
        WorkstationManaStatus.render(graphics,font,minecraft == null ? null : minecraft.player,menu.manaCost());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics,mouseX,mouseY,partialTick);
        WorkstationManaStatus.progress(graphics,leftPos+124,topPos+126,menu.workProgress());
        renderTooltip(graphics,mouseX,mouseY);
    }
}
