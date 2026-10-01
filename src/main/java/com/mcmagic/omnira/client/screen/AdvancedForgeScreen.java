package com.mcmagic.omnira.client.screen;
import com.mcmagic.omnira.forging.AdvancedForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Six interior material positions and a core; the start button remains in the world. */
public final class AdvancedForgeScreen extends AbstractContainerScreen<AdvancedForgeMenu> {
    private static final ResourceLocation BACKGROUND=CrystalWorkstationBackground.texture("workstation_forge");
    public AdvancedForgeScreen(AdvancedForgeMenu m,Inventory i,Component t){super(m,i,t);imageWidth=176;imageHeight=240;inventoryLabelY=146;}
    protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        CrystalWorkstationBackground.draw(g,BACKGROUND,leftPos,topPos);
    }
    protected void renderLabels(GuiGraphics g,int x,int y){CrystalWorkstationBackground.labels(g,font,title,playerInventoryTitle);}
    public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
