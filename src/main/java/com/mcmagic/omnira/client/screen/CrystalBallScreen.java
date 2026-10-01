package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.CrystalBallMenu;
import com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CrystalBallScreen extends AbstractContainerScreen<CrystalBallMenu> {
    private static final ResourceLocation ITEMS=texture("crystal_ball_storage");
    private static final ResourceLocation LIQUID=texture("liquid_crystal_ball_storage");
    private static final ResourceLocation PORT=texture("liquid_crystal_ball_port");
    private static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/container/"+name+".png");}
    public CrystalBallScreen(CrystalBallMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=176;imageHeight=244;inventoryLabelY=148;
    }
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button) {
        if(menu.liquid() && x>=left-31 && x<left-5 && y>=top+37 && y<top+93)return false;
        return super.hasClickedOutside(x,y,left,top,button);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.blit(menu.liquid()?LIQUID:ITEMS,leftPos,topPos,0,0,176,244,176,244);
        if(menu.ball instanceof LiquidCrystalBallBlockEntity liquid) {
            g.blit(PORT,leftPos-31,topPos+37,0,0,26,56,26,56);
            var fluid=liquid.tank.getFluid();
            if(!fluid.isEmpty()) {
                var extension=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
                int size=Math.max(1,(int)(56*Math.cbrt((double)fluid.getAmount()/liquid.tank.getCapacity())));
                var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(extension.getStillTexture(fluid));
                int color=extension.getTintColor(fluid);
                g.setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,1);
                int ox=leftPos+88-size/2,oy=topPos+60-size/2,bevel=Math.round(size*.28F);
                // Clip the liquid instead of repainting the approved orb decoration.
                for(int row=0;row<size;row++) {
                    int inset=Math.max(0,bevel-Math.min(row,size-1-row));
                    g.enableScissor(ox+inset,oy+row,ox+size-inset,oy+row+1);
                    g.blit(ox,oy,0,size,size,sprite);g.disableScissor();
                }
                g.setColor(1,1,1,1);
            }
            String amount=liquid.infinite()?"\u221e":Integer.toString(fluid.getAmount());
            g.drawCenteredString(font,amount+" mB",leftPos+88,topPos+94,0xE3F5FF);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        String label=font.plainSubstrByWidth(title.getString(),164);
        g.drawString(font,label,(176-font.width(label))/2,4,0xE6F6F8,false);
        g.drawString(font,playerInventoryTitle,8,148,0xD6E8E9,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
        if(menu.ball instanceof LiquidCrystalBallBlockEntity liquid && !liquid.tank.isEmpty()
                && mouseX>=leftPos+60 && mouseX<leftPos+116 && mouseY>=topPos+32 && mouseY<topPos+88)
            g.renderTooltip(font,liquid.tank.getFluid().getHoverName().copy().append(" ("+liquid.tank.getFluidAmount()+" / "+liquid.tank.getCapacity()+" mB)"),mouseX,mouseY);
    }
}
