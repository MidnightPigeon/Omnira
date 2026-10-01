package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.ManaEngineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ManaEngineScreen extends AbstractContainerScreen<ManaEngineMenu> {
    private static final ResourceLocation BACKGROUND=CrystalWorkstationBackground.texture("workstation_engine");
    private static final ResourceLocation ICONS=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/engine_controls.png");
    private final Button[] controls=new Button[3];
    public ManaEngineScreen(ManaEngineMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=176;imageHeight=240;inventoryLabelY=146;
    }
    @Override protected void init() {
        super.init();
        for(int i=0;i<3;i++) {
            final int index=i;
            controls[i]=addRenderableWidget(new Button(leftPos+101+i*22,topPos+122,20,20,Component.empty(),button->{
                if(minecraft!=null && minecraft.gameMode!=null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId,index);
            },supplier->supplier.get()) {
                @Override protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
                    super.renderWidget(graphics,mouseX,mouseY,partialTick);
                    int icon=index==2?(menu.setting(2)?2:3):index;
                    com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    graphics.blit(ICONS,getX()+2,getY()+2,icon*16,0,16,16,64,16);
                    if(index<2) graphics.fill(getX()+14,getY()+14,getX()+17,getY()+17,menu.setting(index)?0xFF9CD8A6:0xFF68696E);
                }
            });
        }
        updateTooltips();
    }
    private void updateTooltips() {
        for(int i=0;i<3;i++) {
            Component text=Component.translatable("gui.omnira.engine."+new String[]{"enabled","redstone","direction"}[i])
                    .append(": ").append(Component.translatable(i==2?"gui.omnira.engine."+(menu.setting(i)?"cw":"ccw"):
                            "options."+(menu.setting(i)?"on":"off")));
            controls[i].setMessage(Component.empty());
            controls[i].setTooltip(Tooltip.create(text));
        }
    }
    @Override protected void containerTick() {super.containerTick();updateTooltips();}
    @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY) {
        CrystalWorkstationBackground.draw(graphics,BACKGROUND,leftPos,topPos);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY) {
        CrystalWorkstationBackground.labels(graphics,font,title,playerInventoryTitle);
        int current=menu.running()?menu.capacity():0;
        int electric=menu.running()?menu.energyPerSecond():0;
        String first=menu.kinetic()?current+" su | "+(menu.running()?16:0)+" rpm":
                menu.electrical()?electric+" FE/s":Component.translatable("gui.omnira.engine.direct").getString();
        float scale=Math.min(.75F,85F/Math.max(font.width(first),font.width(electric+" FE/s")));
        graphics.pose().pushPose();graphics.pose().translate(10,125,0);graphics.pose().scale(scale,scale,1);
        graphics.drawString(font,first,0,0,0xFFD6E8E9,false);
        if(menu.electrical() && menu.kinetic()) graphics.drawString(font,electric+" FE/s",0,12,0xFFD6E8E9,false);
        graphics.pose().popPose();
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        super.render(graphics,mouseX,mouseY,partialTick);renderTooltip(graphics,mouseX,mouseY);
    }
}
