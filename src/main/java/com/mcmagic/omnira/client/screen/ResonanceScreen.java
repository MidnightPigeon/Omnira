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
    private static final int ACTION_Y=65;
    private final Button[] links;
    private Button unlink,shop;
    public ResonanceScreen(ResonanceMenu menu,Inventory inventory,Component title){super(menu,inventory,title);links=new Button[menu.capacity()];imageWidth=176;imageHeight=94;}
    public static int nodeColor(int kind) {
        return switch (kind) {
            case 2 -> 0xFF9CEDB4;
            case 3 -> 0xFF7CC0FB;
            case 4 -> 0xFFFFE58A;
            case 5 -> 0xFF1E1E24;
            case 6, 7 -> 0xFFB2B2B2;
            default -> 0xFFF7A2B6;
        };
    }

    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init() {
        super.init();
        shop=addRenderableWidget(new CrystalActionButton(leftPos+38,topPos+ACTION_Y,100,20,Component.translatable("gui.omnira.resonance.open_shop"),b->send(ResonanceMenu.OPEN_SHOP)));
        unlink=addRenderableWidget(new CrystalActionButton(leftPos+148,topPos+ACTION_Y+2,18,15,Component.literal("x"),b->send(ResonanceMenu.UNLINK)));
        unlink.setTooltip(Tooltip.create(Component.translatable("gui.omnira.resonance.unlink")));
        int columns=links.length/2,gap=4;
        int buttonWidth=(imageWidth-20-(columns-1)*gap)/columns;
        int gridLeft=leftPos+(imageWidth-columns*buttonWidth-(columns-1)*gap)/2;
        for(int i=0;i<links.length;i++) {
            final int index=i;
            links[i]=addRenderableWidget(new CrystalActionButton(gridLeft+(i%columns)*(buttonWidth+gap),topPos+23+(i/columns)*20,buttonWidth,18,Component.empty(),b->send(index)));
        }
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        // Reuse the terminal frame at native pixel scale, including its lower corners.
        int bodyHeight=imageHeight-14;
        g.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,bodyHeight,176,241);
        g.blit(TEXTURE,leftPos,topPos+bodyHeight,0,127,imageWidth,14,176,241);
        for(int i=0;i<links.length;i++){
            var button=links[i];int status=menu.status(i);
            int color=status==0?0xFF768390:nodeColor(status);
            g.fill(button.getX()+3,button.getY()+button.getHeight(),button.getX()+button.getWidth()-3,button.getY()+button.getHeight()+1,
                    color);
        }
        if(menu.kind()==0)g.drawCenteredString(font,Component.translatable("gui.omnira.resonance."+(menu.selected()<0?"select":"unavailable")),leftPos+88,topPos+ACTION_Y+(20-font.lineHeight)/2,0xDDD2E8);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        var label=font.substrByWidth(title,imageWidth-24);
        g.drawString(font,label.getString(),(imageWidth-font.width(label))/2,9,0xEEE2FF,false);
    }
    @Override protected void renderContent(GuiGraphics g,int mx,int my,float partial) {
        for(int i=0;i<links.length;i++) {
            int status=menu.status(i);links[i].active=status!=0;
            var label=Component.literal((i+1)+" ").append(Component.translatable("gui.omnira.resonance."+(status==7?"container":status==6?"mixed":status==5?"void":status==4?"shop":status==3?"fluid":status==2?"items":"empty")));
            links[i].setMessage(Component.literal(font.substrByWidth(label,links[i].getWidth()-6).getString()));
            links[i].setTooltip(Tooltip.create(label));
            links[i].setFGColor(i==menu.selected()?0xFFFFFF:status==0?0xF0A6B6:nodeColor(status));
        }
        unlink.visible=menu.selected()>=0;
        shop.visible=menu.kind()==4||menu.kind()==6;
        shop.setMessage(Component.translatable(menu.kind()==4?"gui.omnira.resonance.open_shop":"gui.omnira.resonance.open_container"));
        super.renderContent(g,mx,my,partial);renderTooltip(g,mx,my);
    }
}
