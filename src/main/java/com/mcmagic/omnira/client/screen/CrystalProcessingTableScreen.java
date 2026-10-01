package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.item.PrimaryMicrocoreItem;
import com.mcmagic.omnira.menu.CrystalProcessingTableMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CrystalProcessingTableScreen extends AbstractContainerScreen<CrystalProcessingTableMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Omnira.MOD_ID,
            "textures/gui/container/workstation_processing.png"
    );
    private static final ResourceLocation CRAFTING_TABLE_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/crafting_table.png"
    );
    private static final int WRITE_BUTTON_X = 124;
    private static final int WRITE_BUTTON_Y = 124;
    private static final int WRITE_BUTTON_WIDTH = 40;
    private static final int WRITE_BUTTON_HEIGHT = 18;
    private Button writeButton;

    public CrystalProcessingTableScreen(CrystalProcessingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 240;
        this.inventoryLabelY = 146;
    }

    @Override
    protected void init() {
        super.init();
        this.writeButton = addRenderableWidget(Button.builder(
                        Component.translatable("button.omnira.crystal_processing_table.write"),
                        button -> {
                            if (this.minecraft != null && this.minecraft.player != null && this.minecraft.gameMode != null
                                    && this.menu.canProcessCrystal()) {
                                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, CrystalProcessingTableMenu.WRITE_BUTTON);
                            }
                        })
                .bounds(this.leftPos + WRITE_BUTTON_X, this.topPos + WRITE_BUTTON_Y, WRITE_BUTTON_WIDTH, WRITE_BUTTON_HEIGHT)
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.writeButton != null) {
            this.writeButton.active = this.menu.canProcessCrystal() && !this.menu.isWorking();
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        CrystalWorkstationBackground.draw(guiGraphics,GUI_TEXTURE,leftPos,topPos);
    }

    static void renderVanillaFrame(GuiGraphics graphics, int leftPos, int topPos, int imageWidth, int imageHeight) {
        // Sample the actual vanilla container border without stretching its bevel.
        graphics.blit(CRAFTING_TABLE_TEXTURE, leftPos, topPos, 0, 0, 176, 4, 256, 256);
        graphics.blit(CRAFTING_TABLE_TEXTURE, leftPos, topPos + imageHeight - 4, 0, 162, 176, 4, 256, 256);
        graphics.blit(CRAFTING_TABLE_TEXTURE, leftPos, topPos + 4, 4, imageHeight - 8,
                0.0F, 50.0F, 4, 1, 256, 256);
        graphics.blit(CRAFTING_TABLE_TEXTURE, leftPos + imageWidth - 4, topPos + 4, 4, imageHeight - 8,
                172.0F, 50.0F, 4, 1, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        CompositeSlotGhost.render(guiGraphics,menu.getSlot(0),menu.getSlot(1),leftPos,topPos);
        WorkstationManaStatus.progress(guiGraphics,leftPos+WRITE_BUTTON_X,topPos+WRITE_BUTTON_Y,menu.workProgress());
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (stack.getItem() instanceof PrimaryMicrocoreItem && this.hoveredSlot != null) {
            String hiddenKey = this.hoveredSlot == this.menu.getSlot(CrystalProcessingTableMenu.TARGET_CORE_SLOT)
                    ? PrimaryMicrocoreItem.SHAPE_TOOLTIP
                    : this.hoveredSlot == this.menu.getSlot(CrystalProcessingTableMenu.SHAPE_CORE_SLOT)
                    ? PrimaryMicrocoreItem.TARGET_TOOLTIP : null;
            if (hiddenKey != null) {
                lines.removeIf(line -> line.getContents() instanceof TranslatableContents text
                        && text.getKey().equals(hiddenKey));
            }
        }
        return lines;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        CrystalWorkstationBackground.labels(guiGraphics,font,title,playerInventoryTitle);
        WorkstationManaStatus.render(guiGraphics,font,minecraft == null ? null : minecraft.player,menu.manaCost());
    }
}
