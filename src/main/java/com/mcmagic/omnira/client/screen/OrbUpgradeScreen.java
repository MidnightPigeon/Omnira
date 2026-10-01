package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.OrbUpgradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class OrbUpgradeScreen extends AbstractContainerScreen<OrbUpgradeMenu> {
    private static final ResourceLocation ORIGINAL=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/container/crystal_ball.png");
    private Button mode;
    private final Button[] directions;
    public OrbUpgradeScreen(OrbUpgradeMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=176;imageHeight=197;inventoryLabelY=101;
        directions=new Button[menu.intake?4:2];
    }
    private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init() {
        super.init();
        mode=addRenderableWidget(Button.builder(Component.empty(),b->send(0)).bounds(leftPos+44,topPos+54,88,18).build());
        for(int i=0;i<directions.length;i++) {
            final int index=i;
            directions[i]=addRenderableWidget(Button.builder(Component.empty(),b->send(index+1))
                    .bounds(leftPos+88-directions.length*20+i*40,topPos+76,39,18).build());
        }
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.fill(leftPos+4,topPos+2,leftPos+172,topPos+97,0xFFAABFCB);
        g.fill(leftPos+6,topPos+4,leftPos+170,topPos+95,0xFF384650);
        for(int i=0;i<3;i++) {
            int x=leftPos+62+i*18,y=topPos+30;
            g.fill(x-1,y-1,x+17,y+17,0xFFB1CED8);
            g.fill(x,y,x+16,y+16,0xFF688091);
        }
        g.blit(ORIGINAL,leftPos,topPos+98,0,123,176,99,176,222);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        String label=font.plainSubstrByWidth(title.getString(),156);
        g.drawString(font,label,(176-font.width(label))/2,10,0xE6F6F8,false);
        g.drawString(font,playerInventoryTitle,8,101,0xD6E8E9,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        mode.setMessage(Component.translatable("gui.omnira.orb."+(menu.whitelist()?"whitelist":"blacklist")));
        String[] names=menu.intake?new String[]{"front","back","left","right"}:new String[]{"down","up"};
        for(int i=0;i<directions.length;i++)directions[i].setMessage(Component.literal((menu.directions()&(1<<i))!=0?"+ ":"- ").append(Component.translatable("gui.omnira.orb."+names[i])));
        super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
    }
}
