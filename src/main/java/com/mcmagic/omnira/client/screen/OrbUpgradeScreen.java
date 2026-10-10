package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.OrbUpgradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class OrbUpgradeScreen extends ResponsiveContainerScreen<OrbUpgradeMenu> {
    private static final ResourceLocation ORIGINAL=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/container/crystal_ball.png");
    private Button mode;
    private final Button[] directions;
    public OrbUpgradeScreen(OrbUpgradeMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=176;imageHeight=217;inventoryLabelY=121;
        directions=new Button[6];
    }
    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init() {
        super.init();
        mode=addRenderableWidget(panelButton(leftPos+44,topPos+54,88,0,menu::whitelist));
        for(int i=0;i<directions.length;i++) {
            final int index=i;
            directions[i]=addRenderableWidget(panelButton(leftPos+10+(i%3)*52,topPos+76+(i/3)*20,51,index+1,()->(menu.directions()&(1<<index))!=0));
        }
    }
    private Button panelButton(int x,int y,int width,int id,java.util.function.BooleanSupplier enabled){
        return new Button(x,y,width,18,Component.empty(),button->send(id),supplier->supplier.get()){
            @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){
                boolean lit=enabled.getAsBoolean(),hover=isHoveredOrFocused();
                int x=getX(),y=getY(),w=getWidth(),h=getHeight();
                g.fill(x,y,x+w,y+h,hover?0xFFE6F6F8:lit?0xFFB1CED8:0xFF688091);
                g.fill(x+1,y+1,x+w-1,y+h-1,lit?(hover?0xFFCAE5EC:0xFFB1CED8):(hover?0xFF688091:0xFF4C606D));
                g.fill(x+1,y+h-2,x+w-1,y+h-1,lit?0xFF8CAEBB:0xFF384650);
                g.drawCenteredString(font,getMessage(),x+w/2,y+(h-font.lineHeight)/2,lit?0xFF263D48:0xFFE6F6F8);
            }
        };
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.fill(leftPos+4,topPos+2,leftPos+172,topPos+117,0xFFAABFCB);
        g.fill(leftPos+6,topPos+4,leftPos+170,topPos+115,0xFF384650);
        for(int i=0;i<3;i++) {
            int x=leftPos+62+i*18,y=topPos+30;
            g.fill(x-1,y-1,x+17,y+17,0xFFB1CED8);
            g.fill(x,y,x+16,y+16,0xFF688091);
        }
        g.blit(ORIGINAL,leftPos,topPos+118,0,123,176,99,176,222);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        String label=font.plainSubstrByWidth(title.getString(),156);
        g.drawString(font,label,(176-font.width(label))/2,10,0xE6F6F8,false);
        g.drawString(font,playerInventoryTitle,8,121,0xD6E8E9,false);
    }
    @Override protected void renderContent(GuiGraphics g,int mouseX,int mouseY,float partial) {
        mode.setMessage(Component.translatable("gui.omnira.orb."+(menu.whitelist()?"whitelist":"blacklist")));
        String[] names=menu.intake?new String[]{"front","back","left","right","down","up"}:new String[]{"down","up","front","back","left","right"};
        for(int i=0;i<directions.length;i++)directions[i].setMessage(Component.literal((menu.directions()&(1<<i))!=0?"+ ":"- ").append(Component.translatable("gui.omnira.orb."+names[i])));
        super.renderContent(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
    }
}
