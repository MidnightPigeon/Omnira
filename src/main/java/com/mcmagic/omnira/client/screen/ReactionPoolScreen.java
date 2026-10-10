package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.alchemy.ReactionPoolMenu;
import com.mcmagic.omnira.alchemy.ReactionPoolFluidLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** The tank, reaction inventory and upgrade strip retain separate crystal frames. */
public final class ReactionPoolScreen extends ResponsiveContainerScreen<ReactionPoolMenu> {
    public ReactionPoolScreen(ReactionPoolMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=250;imageHeight=240;inventoryLabelX=45;inventoryLabelY=144;titleLabelX=14;titleLabelY=8;}
    private void frame(GuiGraphics g,int x,int y,int w,int h){
        g.fill(x+3,y,x+w-3,y+h,0xFF8FACB6);g.fill(x,y+3,x+w,y+h-3,0xFF8FACB6);
        g.fill(x+2,y+3,x+w-2,y+h-3,0xFFCEE2E7);g.fill(x+4,y+5,x+w-4,y+h-5,0xFF476773);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        int x=leftPos,y=topPos;
        MachinePanelArt.pool(CrystalUi.pixels(g,x,y));frame(g,x+35,y+139,180,100);
        g.fill(x+19,y+28,x+66,y+102,0xFF284956);
        for(var layer:ReactionPoolFluidLayout.layers(menu.pool.fluids,menu.pool.capacity())){
            var fluid=menu.pool.fluids.getFluidInTank(layer.row());int top=y+layer.top(),liquidBottom=y+layer.bottom();
            var extension=IClientFluidTypeExtensions.of(fluid.getFluid());int color=extension.getTintColor(fluid);
            var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(extension.getStillTexture(fluid));
            g.setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,.85F);
            enableUiScissor(g,x+22,top,x+63,liquidBottom);
            for(int xx=x+22;xx<x+63;xx+=16)for(int yy=top;yy<liquidBottom;yy+=16)g.blit(xx,yy,0,16,16,sprite);
            g.disableScissor();g.setColor(1,1,1,1);
        }
        // Chamfered rim and luminous side rails echo the large pool rather than a generic list panel.
        g.fill(x+18,y+26,x+67,y+29,0xFFDBEFF0);g.fill(x+20,y+29,x+22,y+101,0xFFABD5DE);
        g.fill(x+63,y+29,x+65,y+101,0xFFABD5DE);g.fill(x+18,y+101,x+67,y+104,0xFFDBEFF0);
        for(int i=0;i<4;i++){
            int yy=y+27+i*19;var fluid=menu.pool.fluids.getFluidInTank(i);
            g.fill(x+71,yy,x+144,yy+17,0xFF2D4D5B);
            if(fluid.isEmpty())continue;
            int color=IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
            g.fill(x+72,yy+1,x+74,yy+16,0xFF000000|(color&0xFFFFFF));
            String name=font.plainSubstrByWidth(fluid.getHoverName().getString(),65);
            g.drawString(font,name,x+77,yy+1,0xFFE8F5F7,false);
            g.drawString(font,fluid.getAmount()+" mB",x+77,yy+9,0xFFB6DEE8,false);
        }
        for(var slot:menu.slots){
            g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xFFAECBD5);
            g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xFF476373);
        }
        int color=menu.pool.powered()?0xFF9FEBC4:0xFF627D85;
        // Two opposed chevrons form the reaction status glyph, not a text button.
        for(int i=0;i<5;i++){
            g.fill(x+74+i,y+116+i,x+76+i,y+118+i,color);g.fill(x+74+i,y+125-i,x+76+i,y+127-i,color);
            g.fill(x+86-i,y+116+i,x+88-i,y+118+i,color);g.fill(x+86-i,y+125-i,x+88-i,y+127-i,color);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        CrystalUi.title(g,font,title,imageWidth/2,titleLabelY,226,0xFFE9F6F8);
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFFE9F6F8,false);
    }
    @Override protected void renderContent(GuiGraphics g,int x,int y,float partial){
        super.renderContent(g,x,y,partial);renderTooltip(g,x,y);
        if(x>=leftPos+72&&x<leftPos+90&&y>=topPos+113&&y<topPos+131){
            var text=new java.util.ArrayList<Component>();
            text.add(Component.translatable(menu.pool.complete()?"gui.omnira.pool.complete":"gui.omnira.pool.incomplete"));
            text.add(Component.translatable("gui.omnira.pool.efficiency",menu.pool.efficiency()>=1?String.format(java.util.Locale.ROOT,"%.1f",100/menu.pool.efficiency()):"--"));
            text.add(Component.translatable("gui.omnira.pool.parallel",menu.pool.reactionCount()));
            text.add(Component.literal(menu.pool.fluids.total()+" / "+menu.pool.capacity()+" mB"));
            g.renderComponentTooltip(font,text,x,y);
        }
        int row=ReactionPoolFluidLayout.rowAt(menu.pool.fluids,menu.pool.capacity(),x-leftPos,y-topPos);
        if(row>=0){
            var fluid=menu.pool.fluids.getFluidInTank(row);
            if(!fluid.isEmpty())g.renderTooltip(font,fluid.getHoverName(),x,y);
        }
    }
    @Override protected boolean mouseClickedContent(double x,double y,int button){
        int row=ReactionPoolFluidLayout.rowAt(menu.pool.fluids,menu.pool.capacity(),x-leftPos,y-topPos);
        if(button==0&&row>=0){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,row);return true;}
        return super.mouseClickedContent(x,y,button);
    }
}
