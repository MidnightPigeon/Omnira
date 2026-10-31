package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.codex.*;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A distinct illuminated binding in the guidebook-sized responsive spread. */
public final class MyriadCodexScreen extends ResponsiveContainerScreen<MyriadCodexMenu> {
    private final long openedAt=net.minecraft.Util.getMillis();
    public MyriadCodexScreen(MyriadCodexMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=CodexLayout.WIDTH;imageHeight=CodexLayout.HEIGHT;inventoryLabelY=-1000;titleLabelY=-1000;}
    private void action(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init(){super.init();buttons();}
    private void button(int x,int y,int w,String key,java.util.function.BooleanSupplier selected,Runnable run){
        var widget=addRenderableWidget(new Button(leftPos+x,topPos+y,w,18,Component.translatable(key),b->run.run(),s->s.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                boolean on=selected.getAsBoolean();int color=on?0xFF426B73:isHoveredOrFocused()?0xFF83AEB7:0xFF52606E;
                if(on||isHoveredOrFocused())g.fill(getX(),getY(),getX()+getWidth(),getY()+height,on?0x28457B8C:0x16457B8C);
                g.fill(getX()+2,getY()+height-1,getX()+getWidth()-2,getY()+height,color);
                if(key.equals("codex.omnira.hollow")){
                    g.fill(getX()+3,getY()+4,getX()+11,getY()+12,color);
                    g.fill(getX()+4,getY()+5,getX()+10,getY()+11,0xFFBACAD5);
                    if(on)g.fill(getX()+5,getY()+6,getX()+9,getY()+10,0xFF397E8D);
                    fit(g,getMessage(),getX()+14,getY()+5,getWidth()-17,color);
                }else fit(g,getMessage(),getX()+3,getY()+5,getWidth()-6,color);
            }
        });widget.setTooltip(Tooltip.create(Component.translatable(key)));
    }
    private void control(int x,int y,int w,int h,String key,CodexControlArt.Icon icon,java.util.function.BooleanSupplier selected,Runnable run){
        var widget=addRenderableWidget(new Button(leftPos+x,topPos+y,w,h,Component.translatable(key),b->run.run(),s->s.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                boolean on=selected.getAsBoolean();
                CodexControlArt.button((a,b,c,d,color)->g.fill(getX()+a,getY()+b,getX()+c,getY()+d,color),getWidth(),getHeight(),active,isHoveredOrFocused(),on);
                int color=active?on?0xFF285A68:0xFF405F73:0xFF788E9C;
                if(key.equals("codex.omnira.hollow")){
                    CodexControlArt.checkbox((a,b,c,d,tint)->g.fill(getX()+5+a,getY()+5+b,getX()+5+c,getY()+5+d,tint),on);
                    fit(g,getMessage(),getX()+20,getY()+6,getWidth()-24,color);
                }else if(icon==CodexControlArt.Icon.EXECUTE){
                    CodexControlArt.icon((a,b,c,d,tint)->g.fill(getX()+15+a,getY()+getHeight()/2+b,getX()+15+c,getY()+getHeight()/2+d,tint),icon,color);
                    fit(g,getMessage(),getX()+29,getY()+6,getWidth()-33,color);
                }else if(icon!=null){
                    CodexControlArt.icon((a,b,c,d,tint)->g.fill(getX()+getWidth()/2+a,getY()+getHeight()/2+b,getX()+getWidth()/2+c,getY()+getHeight()/2+d,tint),icon,color);
                }else fit(g,getMessage(),getX()+4,getY()+6,getWidth()-8,color);
            }
        });widget.setTooltip(Tooltip.create(Component.translatable(key)));
    }
    private void buttons(){
        clearWidgets();
        control(282,18,20,20,"codex.omnira.close",CodexControlArt.Icon.CLOSE,()->false,this::onClose);
        for(int i=0;i<3;i++){int n=i;button(25,53+29*i,114,"codex.omnira.mode."+i,()->menu.mode()==n,()->action(n));}
        control(204,180,76,20,"codex.omnira.execute",CodexControlArt.Icon.EXECUTE,()->false,()->action(40));
        if(menu.mode()==1){
            for(int i=0;i<3;i++){int n=i;control(195+i*32,111,28,22,"codex.omnira.shape."+i,CodexControlArt.Icon.values()[i],()->menu.shape()==n,()->action(10+n));}
            control(213,141,58,20,"codex.omnira.hollow",null,menu::hollow,()->action(30));
            if(menu.shape()==2)for(int i=0;i<3;i++){int n=i;control(26+i*37,137,33,20,"codex.omnira.axis."+i,null,()->menu.axis()==n,()->action(20+n));}
        }
    }
    private int lastMode=-1,lastShape=-1;
    @Override protected void containerTick(){super.containerTick();if(lastMode!=menu.mode()||lastShape!=menu.shape()){lastMode=menu.mode();lastShape=menu.shape();buttons();}}
    private void fit(GuiGraphics g,Component text,int x,int y,int width,int color){CrystalUi.label(g,font,text,x,y,width,1,color);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        CodexBookArt.spread((x,y,xx,yy,c)->g.fill(leftPos+x,topPos+y,leftPos+xx,topPos+yy,c));
        fit(g,title,leftPos+24,topPos+25,112,0xFF334C60);
        fit(g,Component.translatable("codex.omnira.definition"),leftPos+181,topPos+25,113,0xFF334C60);
        CrystalWorkstationBackground.inventory(g,leftPos,topPos,menu);
        g.drawString(font,playerInventoryTitle,leftPos+CodexLayout.INVENTORY_X,topPos+CodexLayout.INVENTORY_Y-12,0xFF404040,false);
        ring(g,240,112,(net.minecraft.Util.getMillis()-openedAt)/50.0);
        for(int i=0;i<4;i++)if(menu.slots.get(i).isActive()){var s=menu.slots.get(i);slot(g,s.x,s.y);}
        if(menu.mode()==0){
            PixelUiIcons.arrow(CrystalUi.pixels(g,leftPos,topPos),226,112,29,0xFF547E91);
        }
        fit(g,Component.literal(menu.power()+" MPU"),leftPos+27,topPos+165,108,0xFF466778);
        int diameter=MyriadCodexEntity.diameter(menu.power());
        fit(g,Component.translatable("codex.omnira.diameter",diameter),leftPos+27,topPos+177,108,0xFF466778);
        if(!menu.error().isEmpty()){
            var lines=font.split(Component.translatable("codex.omnira.error."+menu.error()),116);
            for(int i=0;i<Math.min(2,lines.size());i++)g.drawString(font,lines.get(i),leftPos+180,topPos+202+i*9,0xFF9B3C4D,false);
        }
    }
    private void slot(GuiGraphics g,int x,int y){CodexControlArt.slot((a,b,c,d,color)->g.fill(leftPos+x+a,topPos+y+b,leftPos+x+c,topPos+y+d,color));}
    private void ring(GuiGraphics g,int cx,int cy,double time){
        g.pose().pushPose();g.pose().translate(leftPos+cx,topPos+cy,0);
        g.pose().mulPose(Axis.ZP.rotationDegrees((float)CodexControlArt.angle(time)));
        CodexControlArt.ring(g::fill);
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4;
            g.pose().pushPose();g.pose().translate(Math.cos(a)*CodexControlArt.RUNE_RADIUS,Math.sin(a)*CodexControlArt.RUNE_RADIUS,0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(i*45+90));
            CodexControlArt.rune(g::fill,i,CodexControlArt.runeColor(time,i));g.pose().popPose();
        }g.pose().popPose();
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override protected void renderContent(GuiGraphics g,int x,int y,float partial){
        super.renderContent(g,x,y,partial);renderTooltip(g,x,y);
        if(menu.mode()==0&&menu.getCarried().isEmpty()&&hoveredSlot==menu.slots.get(1)&&!hoveredSlot.hasItem())
            g.renderTooltip(font,Component.translatable("codex.omnira.air_target"),x,y);
        if(!menu.error().isEmpty()&&x>=leftPos+180&&x<leftPos+296&&y>=topPos+202&&y<topPos+220)
            g.renderComponentTooltip(font,java.util.List.of(Component.translatable("codex.omnira.error."+menu.error())),x,y);
    }
}
