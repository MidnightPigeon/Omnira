package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.menu.CrystalBallMenu;
import com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CrystalBallScreen extends ResponsiveContainerScreen<CrystalBallMenu> {
    @Override protected int leftExtension(){return menu.liquid()?31:0;}
    @Override protected int rightExtension(){return menu.ball.resonant()?31:0;}
    private static final ResourceLocation ITEMS=texture("crystal_ball_storage");
    private static final ResourceLocation LIQUID=texture("liquid_crystal_ball_storage");
    private static final ResourceLocation PORT=texture("liquid_crystal_ball_port");
    private static final ResourceLocation VOID=texture("void_crystal_ball_storage");
    private static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/container/"+name+".png");}
    public CrystalBallScreen(CrystalBallMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=176;imageHeight=244;inventoryLabelY=148;
    }
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button) {
        if(menu.ball.resonant()&&x>=left+178&&x<left+207&&y>=top+37&&y<top+95)return false;
        if(menu.liquid() && x>=left-31 && x<left-5 && y>=top+37 && y<top+93)return false;
        return super.hasClickedOutside(x,y,left,top,button);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.blit(menu.singleOrb()?VOID:menu.liquid()?LIQUID:ITEMS,leftPos,topPos,0,0,176,244,176,244);
        if(menu.ball instanceof com.mcmagic.omnira.space.sea.StarClusterOrbEntity star){
            int x=leftPos+62,y=topPos+86;
            g.fill(x,y,x+52,y+6,0xFFD0EAF1);g.fill(x+1,y+1,x+51,y+5,0xFF344F63);
            g.fill(x+1,y+1,x+1+Math.clamp(star.progress()*50/200,0,50),y+5,0xFFAEE5EF);
        }
        if(menu.voidOrb()){
            var glyph=CruiseOrbScreen.TRASH_GLYPH;
            for(int row=0;row<glyph.length;row++)for(int col=0;col<glyph[row].length();col++){
                char pixel=glyph[row].charAt(col);if(pixel==' ')continue;
                g.fill(leftPos+82+col,topPos+57+row,leftPos+83+col,topPos+58+row,pixel=='#'?0xD0B3D7DF:0xA078A5BD);
            }
        }
        if(menu.ball.resonant()){
            g.fill(leftPos+178,topPos+37,leftPos+207,topPos+95,0xE03D3659);
            for(int i=0;i<2;i++){
                int y=topPos+42+i*27;
                g.fill(leftPos+183,y,leftPos+201,y+18,0xFFB9A5DA);
                g.fill(leftPos+184,y+1,leftPos+200,y+17,0xFF403B53);
            }
        }
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
                    enableUiScissor(g,ox+inset,oy+row,ox+size-inset,oy+row+1);
                    g.blit(ox,oy,0,size,size,sprite);g.disableScissor();
                }
                g.setColor(1,1,1,1);
            }
            String amount=Integer.toString(fluid.getAmount());
            g.drawCenteredString(font,amount+" mB",leftPos+88,topPos+94,0xE3F5FF);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        String label=font.plainSubstrByWidth(title.getString(),164);
        g.drawString(font,label,(176-font.width(label))/2,4,0xE6F6F8,false);
        g.drawString(font,playerInventoryTitle,8,148,0xD6E8E9,false);
    }
    @Override protected boolean mouseReleasedContent(double x,double y,int button){
        if(menu.ball.resonant()&&!menu.getCarried().isEmpty()&&x>=leftPos+183&&x<leftPos+201){
            for(int i=0;i<2;i++)if(y>=topPos+42+i*27&&y<topPos+60+i*27){
                int index=menu.frequencyStart+i;slotClicked(menu.getSlot(index),index,button,net.minecraft.world.inventory.ClickType.PICKUP);return true;
            }
        }
        return super.mouseReleasedContent(x,y,button);
    }
    @Override protected void renderContent(GuiGraphics g,int mouseX,int mouseY,float partial) {
        super.renderContent(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
        if(menu.ball instanceof LiquidCrystalBallBlockEntity liquid && !liquid.tank.isEmpty()
                && mouseX>=leftPos+60 && mouseX<leftPos+116 && mouseY>=topPos+32 && mouseY<topPos+88)
            g.renderTooltip(font,liquid.tank.getFluid().getHoverName().copy().append(" ("+liquid.tank.getFluidAmount()+" / "+liquid.tank.getCapacity()+" mB)"),mouseX,mouseY);
    }
}
