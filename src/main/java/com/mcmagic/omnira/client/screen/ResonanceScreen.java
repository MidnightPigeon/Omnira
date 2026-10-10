package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.ResonanceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ResonanceScreen extends ResponsiveContainerScreen<ResonanceMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/container/resonance_terminal.png");
    private final Button[] links;
    private Button unlink,shop;
    public ResonanceScreen(ResonanceMenu menu,Inventory inventory,Component title){super(menu,inventory,title);links=new Button[menu.capacity()];imageWidth=176;imageHeight=241;inventoryLabelY=145;}
    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init() {
        super.init();
        shop=addRenderableWidget(new CrystalActionButton(leftPos+38,topPos+85,100,20,Component.translatable("gui.omnira.resonance.open_shop"),b->send(ResonanceMenu.OPEN_SHOP)));
        unlink=addRenderableWidget(new CrystalActionButton(leftPos+150,topPos+4,18,15,Component.literal("x"),b->send(ResonanceMenu.UNLINK)));
        unlink.setTooltip(Tooltip.create(Component.translatable("gui.omnira.resonance.unlink")));
        int columns=links.length/2,step=links.length==6?54:82;
        for(int i=0;i<links.length;i++) {
            final int index=i;
            links[i]=addRenderableWidget(new CrystalActionButton(leftPos+8+(i%columns)*step,topPos+21+(i/columns)*20,step-4,18,Component.empty(),b->send(index)));
        }
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.blit(TEXTURE,leftPos,topPos,0,0,176,241,176,241);
        for(int i=0;i<links.length;i++){
            var button=links[i];int status=menu.status(i);
            int color=status==5?0xFF897399:status==4?MachinePanelArt.GOLD:status==3?MachinePanelArt.BLUE:status==2?MachinePanelArt.MINT:0xFF768390;
            g.fill(button.getX()+3,button.getY()+button.getHeight(),button.getX()+button.getWidth()-3,button.getY()+button.getHeight()+1,
                    i==menu.selected()?MachinePanelArt.LIGHT:color);
        }
        if(menu.kind()==0)g.drawCenteredString(font,Component.translatable("gui.omnira.resonance."+(menu.selected()<0?"select":"unavailable")),leftPos+88,topPos+89,0xDDD2E8);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,font.substrByWidth(title,138).getString(),8,7,0xEEE2FF,false);g.drawString(font,playerInventoryTitle,8,145,0xDDD2E8,false);}
    @Override protected void renderContent(GuiGraphics g,int mx,int my,float partial) {
        for(int i=0;i<links.length;i++) {
            int status=menu.status(i);links[i].active=status!=0;
            var label=Component.literal((i+1)+" ").append(Component.translatable("gui.omnira.resonance."+(status==5?"void":status==4?"shop":status==3?"fluid":status==2?"items":"empty")));
            links[i].setMessage(Component.literal(font.substrByWidth(label,links[i].getWidth()-6).getString()));
            links[i].setTooltip(Tooltip.create(label));
            links[i].setFGColor(i==menu.selected()?0xFFFFFF:status==5?0xC6AFE9:status==4?0xFFE58A:status==3?0x8EC8FA:status==2?0xABE6BA:0xF0A6B6);
        }
        unlink.visible=menu.selected()>=0;
        shop.visible=menu.kind()==4||menu.kind()==6;
        shop.setMessage(Component.translatable(menu.kind()==4?"gui.omnira.resonance.open_shop":"gui.omnira.resonance.open_container"));
        super.renderContent(g,mx,my,partial);renderTooltip(g,mx,my);
    }
}
