package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.vehicle.CruiseOrbMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CruiseOrbScreen extends AbstractContainerScreen<CruiseOrbMenu> {
    private static final ResourceLocation ITEMS=texture("crystal_ball_storage"),LIQUID=texture("liquid_crystal_ball_storage"),PORT=texture("liquid_crystal_ball_port");
    private Button vortex;
    private float uiScale=1;
    private static final String[] CORE_GLYPH={
            "    ++++    ","  ++    ++  "," +  ####  + ","+  ######  +",
            "+ ######## +","+ ######## +","+ ######## +","+  ######  +",
            " +  ####  + ","  ++    ++  ","    ++++    ","            "};
    private static final String[] TRASH_GLYPH={
            "    ####    ","    #  #    "," ########## ","            ",
            "  #      #  ","  # +  + #  ","  # +  + #  ","  # +  + #  ",
            "  # +  + #  ","  #      #  ","   ######   ","            "};
    private static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/container/"+name+".png");}
    public CruiseOrbScreen(CruiseOrbMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=376;imageHeight=244;}
    @Override protected void init(){
        uiScale=Math.min(1,Math.min((width-12)/407F,(height-12)/244F));
        width=Math.round(width/uiScale);height=Math.round(height/uiScale);
        super.init();
        leftPos+=15;
        vortex=addRenderableWidget(Button.builder(Component.empty(),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0))
                .bounds(leftPos+176,topPos+51,24,24).tooltip(Tooltip.create(Component.translatable("gui.omnira.cruise_orb.vortex"))).build());
    }
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button){
        if(x>=left-31 && x<left-5 && y>=top+37 && y<top+93)return false;
        return super.hasClickedOutside(x,y,left,top,button);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.blit(LIQUID,leftPos,topPos,0,0,176,111,176,244);
        g.blit(ITEMS,leftPos+200,topPos,0,0,176,111,176,244);
        // Reuse the approved base and inventory ornament, not a new enclosing panel.
        g.blit(ITEMS,leftPos+100,topPos+111,0,111,176,133,176,244);
        g.blit(PORT,leftPos-31,topPos+37,0,0,26,56,26,56);
        utilitySlot(g,CruiseOrbMenu.CORE,CORE_GLYPH);
        utilitySlot(g,CruiseOrbMenu.TRASH,TRASH_GLYPH);
        var tank=menu.orb.storage.tank;var fluid=tank.getFluid();
        if(!fluid.isEmpty()){
            var ext=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
            var sprite=minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
            int color=ext.getTintColor(fluid),size=Math.max(1,(int)(56*Math.cbrt((double)fluid.getAmount()/tank.getCapacity())));
            g.setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,1);
            int x=leftPos+88-size/2,y=topPos+60-size/2,bevel=Math.round(size*.28F);
            for(int row=0;row<size;row++){
                int inset=Math.max(0,bevel-Math.min(row,size-1-row));g.enableScissor(x+inset,y+row,x+size-inset,y+row+1);
                g.blit(x,y,0,size,size,sprite);g.disableScissor();
            }
            g.setColor(1,1,1,1);
        }
        g.drawCenteredString(font,fluid.getAmount()+" / "+tank.getCapacity()+" mB",leftPos+88,topPos+94,0xE3F5FF);
        vortex.active=menu.orb.vortexTicks()==0;
    }
    private void utilitySlot(GuiGraphics g,int index,String[] glyph){
        var slot=menu.slots.get(index);int x=leftPos+slot.x,y=topPos+slot.y;
        g.fill(x-2,y-2,x+18,y+18,0xB86594A8);
        g.fill(x-1,y-1,x+17,y+17,0xB8244357);
        if(slot.hasItem())return;
        for(int row=0;row<glyph.length;row++)for(int col=0;col<glyph[row].length();col++){
            char pixel=glyph[row].charAt(col);if(pixel==' ')continue;
            g.fill(x+2+col,y+2+row,x+3+col,y+3+row,pixel=='#'?0xD0B3D7DF:0xA078A5BD);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,Component.translatable("block.omnira.liquid_crystal_ball"),39,4,0xE6F6F8,false);
        g.drawString(font,Component.translatable("block.omnira.crystal_ball"),260,4,0xE6F6F8,false);
        g.drawString(font,playerInventoryTitle,108,148,0xD6E8E9,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.pose().pushPose();g.pose().scale(uiScale,uiScale,1);
        mouseX=Math.round(mouseX/uiScale);mouseY=Math.round(mouseY/uiScale);
        super.render(g,mouseX,mouseY,partial);
        // Small spiral glyph, kept within the central command button.
        for(int i=0;i<26;i++){
            double a=i*.52,r=1+i*.26;int x=leftPos+188+(int)(Math.cos(a)*r),y=topPos+63+(int)(Math.sin(a)*r);
            g.fill(x,y,x+2,y+2,0xFFADDEEF);
        }
        renderTooltip(g,mouseX,mouseY);
        if(hoveredSlot!=null && !hoveredSlot.hasItem()){
            if(hoveredSlot.index==CruiseOrbMenu.CORE || hoveredSlot.index==CruiseOrbMenu.TRASH)
                g.renderTooltip(font,font.split(Component.translatable(hoveredSlot.index==CruiseOrbMenu.CORE
                        ?"gui.omnira.cruise_orb.core":"gui.omnira.cruise_orb.trash"),180),mouseX,mouseY);
        }
        if(!menu.orb.storage.tank.isEmpty() && mouseX>=leftPos+60 && mouseX<leftPos+116 && mouseY>=topPos+32 && mouseY<topPos+88)
            g.renderTooltip(font,menu.orb.storage.tank.getFluid().getHoverName(),mouseX,mouseY);
        g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(x/uiScale,y/uiScale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/uiScale,y/uiScale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged(x/uiScale,y/uiScale,button,dx/uiScale,dy/uiScale);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){return super.mouseScrolled(x/uiScale,y/uiScale,dx,dy);}
}
