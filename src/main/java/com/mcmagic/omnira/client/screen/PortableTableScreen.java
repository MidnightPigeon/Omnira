package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.PortableTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PortableTableScreen extends ResponsiveContainerScreen<PortableTableMenu> {
    public PortableTableScreen(PortableTableMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title);
        imageHeight = 194;
        inventoryLabelY = 100;
    }
    private static final int[] SOCKET_RIMS = {0xffaddccc, 0xffa8cfea, 0xffcbbce6, 0xffe7d5ae};

    @Override protected java.util.List<Component> getTooltipFromContainerItem(net.minecraft.world.item.ItemStack stack) {
        return CrystalProcessingTableScreen.roleTooltip(super.getTooltipFromContainerItem(stack), stack, hoveredSlot, menu);
    }

    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        int x = leftPos, y = topPos;
        g.fill(x,y,x+176,y+194,0xffaec6cc);
        g.fill(x+2,y+2,x+174,y+192,0xff293e50);
        for (int i=-33;i<=33;i++) {
            int half = 34-Math.abs(i);
            g.fill(x+88-half,y+58+i,x+89+half,y+59+i,0xff4b687b);
        }
        for (var slot : menu.slots) {
            int sx=x+slot.x, sy=y+slot.y;
            g.fill(sx-1,sy-1,sx+17,sy+17,slot.index < SOCKET_RIMS.length ? SOCKET_RIMS[slot.index] : 0xff88adb6);
            g.fill(sx,sy,sx+16,sy+16,0xff213343);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        CrystalUi.title(g,font,title,88,7,166,0xffebfaff);
        g.drawString(font,playerInventoryTitle,8,inventoryLabelY,0xffd0e3ea,false);
    }
    @Override protected void renderContent(GuiGraphics g,int x,int y,float partial) {
        super.renderContent(g,x,y,partial);
        renderTooltip(g,x,y);
    }
}
