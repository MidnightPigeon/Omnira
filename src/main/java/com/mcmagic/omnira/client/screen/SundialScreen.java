package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.time.SundialBlockEntity;
import com.mcmagic.omnira.time.SundialMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import static com.mcmagic.omnira.time.SundialLayout.*;

public final class SundialScreen extends AbstractContainerScreen<SundialMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/sundial.png");
    public SundialScreen(SundialMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);imageWidth=WIDTH;imageHeight=HEIGHT;
        inventoryLabelX=INVENTORY_X;inventoryLabelY=151;
    }
    private boolean over(double mx,double my,int x,int y,int w,int h){
        return mx>=leftPos+x&&mx<leftPos+x+w&&my>=topPos+y&&my<topPos+y+h;
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0&&over(mx,my,CHOICE_X-1,CHOICE_Y-1,18,18)){
            if(Minecraft.getInstance().gameMode!=null)Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId,0);
            return true;
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.blit(TEXTURE,leftPos,topPos,0,0,WIDTH,HEIGHT,WIDTH,HEIGHT);
        int energyHeight=ENERGY_HEIGHT*menu.energy()/SundialBlockEntity.CAPACITY;
        g.fill(leftPos+ENERGY_X,topPos+ENERGY_Y+ENERGY_HEIGHT-energyHeight,
                leftPos+ENERGY_X+ENERGY_WIDTH,topPos+ENERGY_Y+ENERGY_HEIGHT,0xFFB8FFD0);
        int width=PROGRESS_WIDTH*menu.progress()/SundialBlockEntity.DURATION;
        g.fill(leftPos+PROGRESS_X,topPos+PROGRESS_Y,leftPos+PROGRESS_X+width,
                topPos+PROGRESS_Y+PROGRESS_HEIGHT,0xFFFFDB88);
        var chosen=menu.selectedOutput();
        if(!chosen.isEmpty())g.renderItem(chosen,leftPos+CHOICE_X,topPos+CHOICE_Y);
        if(over(mouseX,mouseY,CHOICE_X-1,CHOICE_Y-1,18,18))
            g.fill(leftPos+CHOICE_X,topPos+CHOICE_Y,leftPos+CHOICE_X+16,topPos+CHOICE_Y+16,0x40FFFFFF);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xE4ECE8,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
        if(over(mouseX,mouseY,CHOICE_X-1,CHOICE_Y-1,18,18)){
            var chosen=menu.selectedOutput();
            Component text=Component.translatable("gui.omnira.sundial.choose");
            if(!chosen.isEmpty())text=chosen.getHoverName().copy().append(" - ").append(text);
            g.renderTooltip(font,text,mouseX,mouseY);
        }else if(over(mouseX,mouseY,PROGRESS_X-1,PROGRESS_Y-1,PROGRESS_WIDTH+2,PROGRESS_HEIGHT+2)){
            g.renderTooltip(font,Component.translatable("gui.omnira.sundial.progress",menu.progress()/20,SundialBlockEntity.DURATION/20),mouseX,mouseY);
        }else if(over(mouseX,mouseY,ENERGY_X-2,ENERGY_Y-2,ENERGY_WIDTH+4,ENERGY_HEIGHT+4)){
            g.renderTooltip(font,Component.translatable("gui.omnira.sundial.energy",menu.energy(),SundialBlockEntity.CAPACITY),mouseX,mouseY);
        }
    }
}
