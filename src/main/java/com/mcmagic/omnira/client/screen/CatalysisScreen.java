package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.alchemy.catalysis.CatalysisMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public final class CatalysisScreen extends ResponsiveContainerScreen<CatalysisMenu> {
    public CatalysisScreen(CatalysisMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=248;inventoryLabelY=154;}
    private void frame(GuiGraphics g,int x,int y,int w,int h,int inner){
        g.fill(x+2,y,x+w-2,y+h,0xFFBDD6DB);g.fill(x,y+2,x+w,y+h-2,0xFFBDD6DB);g.fill(x+2,y+2,x+w-2,y+h-2,inner);
        g.fill(x+2,y+2,x+3,y+h-2,0xFFE0F1ED);g.fill(x+w-3,y+2,x+w-2,y+h-2,0xFFE0F1ED);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        int x=leftPos,y=topPos;var art=CrystalUi.pixels(g,x,y);MachinePanelArt.dome(art);
        frame(g,x,y+151,176,96,0xFF3E5967);
        g.fill(x+49,y+83,x+127,y+103,0xFF243F50);g.fill(x+50,y+84,x+126,y+102,0xFF678A98);
        var fluid=menu.chamber.tank.getFluid();
        if(!fluid.isEmpty()){
            int height=Math.max(1,18*fluid.getAmount()/4000);var ext=IClientFluidTypeExtensions.of(fluid.getFluid());int color=ext.getTintColor(fluid);
            var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
            g.setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,.9F);
            enableUiScissor(g,x+50,y+102-height,x+126,y+102);for(int xx=x+50;xx<x+126;xx+=16)for(int yy=y+102-height;yy<y+102;yy+=16)g.blit(xx,yy,0,16,16,sprite);
            g.disableScissor();g.setColor(1,1,1,1);
            for(int side:new int[]{-1,1}){
                int xx=88+side*47;
                MachinePanelArt.flow(art,xx,99,xx,48,CrystalUi.time(partial),0xFF000000|(color&0xFFFFFF));
            }
        }
        for(var slot:menu.slots){int color=slot.index==0?0xFFCAD5B3:slot.index==1?0xFFAEDCCA:slot.index==2?0xFFD9DFEE:0xFFC2DBDF;
            g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,color);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xFF3D5968);}
        int ink=menu.chamber.plan()!=null?0xFFCEF2DF:0xFF88A6AF;
        for(int side:new int[]{-1,1})for(int i=0;i<3;i++)g.fill(x+87+side*(7+i*2),y+126+i,x+89+side*(7+i*2),y+128+i,ink);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){CrystalUi.title(g,font,title,88,5,166,0xFFE2F0F1);g.drawString(font,playerInventoryTitle,8,154,0xFFE2F0F1,false);}
    @Override protected void renderContent(GuiGraphics g,int x,int y,float partial){
        super.renderContent(g,x,y,partial);renderTooltip(g,x,y);
        int rx=x-leftPos,ry=y-topPos;
        if(rx>=49&&rx<127&&ry>=83&&ry<103){var f=menu.chamber.tank.getFluid();var text=new java.util.ArrayList<Component>();
            if(!f.isEmpty())text.add(f.getHoverName());text.add(Component.literal(f.getAmount()+" / 4000 mB"));g.renderComponentTooltip(font,text,x,y);}
        for(int i=0;i<5;i++){var slot=menu.slots.get(i);if(!slot.hasItem()&&rx>=slot.x&&rx<slot.x+16&&ry>=slot.y&&ry<slot.y+16)
            g.renderTooltip(font,Component.translatable("gui.omnira.catalysis."+new String[]{"substrate","seed","product","intake","output"}[i]),x,y);}
    }
}
