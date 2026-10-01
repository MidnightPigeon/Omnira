package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.ResonanceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ResonanceScreen extends AbstractContainerScreen<ResonanceMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/container/resonance_terminal.png");
    private final Button[] links;
    private Button previous,next,unlink,shop;
    public ResonanceScreen(ResonanceMenu menu,Inventory inventory,Component title){super(menu,inventory,title);links=new Button[menu.capacity()];imageWidth=176;imageHeight=241;inventoryLabelY=145;}
    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init() {
        super.init();
        shop=addRenderableWidget(Button.builder(Component.translatable("gui.omnira.resonance.open_shop"),b->send(ResonanceMenu.OPEN_SHOP)).bounds(leftPos+38,topPos+85,100,20).build());
        unlink=addRenderableWidget(Button.builder(Component.literal("x"),b->send(ResonanceMenu.UNLINK)).bounds(leftPos+150,topPos+4,18,15).tooltip(Tooltip.create(Component.translatable("gui.omnira.resonance.unlink"))).build());
        int columns=links.length/2,step=links.length==6?54:82;
        for(int i=0;i<links.length;i++) {
            final int index=i;
            links[i]=addRenderableWidget(Button.builder(Component.empty(),b->send(index)).bounds(leftPos+8+(i%columns)*step,topPos+21+(i/columns)*20,step-4,18).build());
        }
        previous=addRenderableWidget(Button.builder(Component.literal("<"),b->send(ResonanceMenu.PREVIOUS)).bounds(leftPos+8,topPos+124,18,16).tooltip(Tooltip.create(Component.translatable("gui.omnira.resonance.previous"))).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),b->send(ResonanceMenu.NEXT)).bounds(leftPos+150,topPos+124,18,16).tooltip(Tooltip.create(Component.translatable("gui.omnira.resonance.next"))).build());
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.blit(TEXTURE,leftPos,topPos,0,0,176,241,176,241);
        if(menu.kind()==2)for(int i=0;i<27;i++)slot(g,8+i%9*18,70+i/9*18);
        if(menu.kind()==3) {
            g.drawCenteredString(font,font.substrByWidth(menu.fluidName(),156).getString(),leftPos+88,topPos+66,0xDDD2E8);
            g.drawCenteredString(font,menu.fluidVolume()+" / "+menu.fluidCapacity()+" mB",leftPos+88,topPos+77,0xDDD2E8);
            slot(g,80,88);
            g.fill(leftPos+54,topPos+112,leftPos+122,topPos+115,0xFF292238);
            g.fill(leftPos+54,topPos+112,leftPos+54+68*menu.progress()/100,topPos+115,0xFF89BCED);
        }
        if(menu.kind()==0)g.drawCenteredString(font,Component.translatable("gui.omnira.resonance."+(menu.selected()<0?"select":"unavailable")),leftPos+88,topPos+89,0xDDD2E8);
        if(menu.kind()==2)g.drawCenteredString(font,(menu.page()+1)+" / "+Math.max(1,menu.pages()),leftPos+88,topPos+128,0xDDD2E8);
        if(menu.kind()==2||menu.kind()==3)g.drawCenteredString(font,Component.translatable("gui.omnira.resonance."+(menu.kind()==3?"fluid_cost":"item_cost")),leftPos+88,topPos+(menu.kind()==3?119:60),0xBDB3CC);
    }
    private void slot(GuiGraphics g,int x,int y){g.fill(leftPos+x-1,topPos+y-1,leftPos+x+17,topPos+y+17,0xFFBBACD4);g.fill(leftPos+x,topPos+y,leftPos+x+16,topPos+y+16,0xFF504B67);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,font.substrByWidth(title,138).getString(),8,7,0xEEE2FF,false);g.drawString(font,playerInventoryTitle,8,145,0xDDD2E8,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        for(int i=0;i<links.length;i++) {
            int status=menu.status(i);links[i].active=status!=0;
            var label=Component.literal((i+1)+" ").append(Component.translatable("gui.omnira.resonance."+(status==4?"shop":status==3?"fluid":status==2?"items":"empty")));
            links[i].setMessage(Component.literal(font.substrByWidth(label,links[i].getWidth()-6).getString()));
            links[i].setTooltip(Tooltip.create(label));
            links[i].setFGColor(i==menu.selected()?0xFFFFFF:status==4?0xFFE58A:status==3?0x8EC8FA:status==2?0xABE6BA:0xF0A6B6);
        }
        previous.visible=next.visible=menu.kind()==2;previous.active=menu.page()>0;next.active=menu.page()+1<menu.pages();
        unlink.visible=menu.selected()>=0;
        shop.visible=menu.kind()==4;
        super.render(g,mx,my,partial);renderTooltip(g,mx,my);
    }
}
