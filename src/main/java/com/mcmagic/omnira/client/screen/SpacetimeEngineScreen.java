package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.engine.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SpacetimeEngineScreen extends ResponsiveContainerScreen<SpacetimeEngineMenu> {
    private final EditBox[] capacities=new EditBox[4],speeds=new EditBox[4];
    private final boolean[] reverse=new boolean[4];
    private final int[] lastCapacity={-1,-1,-1,-1},lastSpeed={-1,-1,-1,-1};
    private final boolean[] lastReverse=new boolean[4];
    private final boolean kinetics=SpacetimeEngineController.hasKinetics();
    public SpacetimeEngineScreen(SpacetimeEngineMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=268;imageHeight=294;}
    @Override protected void init(){
        super.init();
        for(int i=0;i<4;i++){
            final int side=i;int y=topPos+43+i*28;
            int output=menu.displaySide(i);
            if(kinetics){capacities[i]=number(leftPos+59,y,66,menu.capacity(output),6);
                speeds[i]=number(leftPos+139,y,39,menu.rpm(output),3);}
            reverse[i]=menu.reversed(output);lastCapacity[i]=menu.capacity(output);lastSpeed[i]=menu.rpm(output);lastReverse[i]=reverse[i];
            addRenderableWidget(new SymbolButton(leftPos+(kinetics?190:164),y,Component.translatable("gui.omnira.spacetime_engine.rotation"),false,
                    ()->{reverse[side]=!reverse[side];if(!kinetics)submit(side);},()->reverse[side]));
            if(kinetics)addRenderableWidget(new SymbolButton(leftPos+222,y,Component.translatable("gui.omnira.spacetime_engine.apply"),true,()->submit(side),()->false));
        }
    }
    private EditBox number(int x,int y,int width,int value,int length){
        var box=new EditBox(font,x,y,width,18,Component.empty());box.setMaxLength(length);box.setFilter(s->s.matches("[0-9]*"));
        box.setBordered(false);box.setY(y+5);box.setHeight(13);
        box.setValue(Integer.toString(value));box.setTextColor(0xDFEFEC);addRenderableWidget(box);return box;
    }
    private void submit(int side){
        if(!kinetics){PacketDistributor.sendToServer(new EngineConfigPayload(menu.containerId,menu.displaySide(side),0,0,reverse[side]));return;}
        try{int su=Integer.parseInt(capacities[side].getValue()),rpm=Integer.parseInt(speeds[side].getValue());
            if(su<=menu.limit()&&rpm<=128)PacketDistributor.sendToServer(new EngineConfigPayload(menu.containerId,menu.displaySide(side),su,rpm,reverse[side]));
        }catch(NumberFormatException ignored){}
    }
    @Override protected void containerTick(){
        super.containerTick();
        for(int i=0;i<4;i++){
            int output=menu.displaySide(i);
            if(kinetics&&lastCapacity[i]!=menu.capacity(output)){lastCapacity[i]=menu.capacity(output);capacities[i].setValue(Integer.toString(lastCapacity[i]));}
            if(kinetics&&lastSpeed[i]!=menu.rpm(output)){lastSpeed[i]=menu.rpm(output);speeds[i].setValue(Integer.toString(lastSpeed[i]));}
            if(lastReverse[i]!=menu.reversed(output)){lastReverse[i]=menu.reversed(output);reverse[i]=lastReverse[i];}
        }
    }
    private void frame(GuiGraphics g,int x,int y,int w,int h){
        g.fill(x+2,y,x+w-2,y+h,0xFFBDDCD9);g.fill(x,y+2,x+w,y+h-2,0xFFBDDCD9);g.fill(x+2,y+2,x+w-2,y+h-2,0xF04F707C);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        var art=CrystalUi.pixels(g,leftPos,topPos);
        MachinePanelArt.engineControls(art,kinetics,CrystalUi.time(partial),new int[]{SpacetimeEngineGeometry.outputColor(0),
                SpacetimeEngineGeometry.outputColor(1),SpacetimeEngineGeometry.outputColor(2),SpacetimeEngineGeometry.outputColor(3)});
        frame(g,leftPos+43,topPos+201,182,92);
        for(var slot:menu.slots){int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xFFC8E2DC);g.fill(x,y,x+16,y+16,0xFF304B58);}
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        CrystalUi.title(g,font,title,imageWidth/2,3,250,0xFFE4F1EC);
        if(kinetics){g.drawCenteredString(font,"SU",92,28,0xFFE4F1EC);g.drawCenteredString(font,"rpm",158,28,0xFFE4F1EC);}
        for(int i=0;i<4;i++){
            int color=SpacetimeEngineGeometry.outputColor(i),y=48+i*28,x=kinetics?7:84;g.fill(x,y-3,x+3,y+11,color);
            g.drawString(font,Component.translatable("gui.omnira.spacetime_engine."+new String[]{"front","right","back","left"}[i]),x+8,y,color,false);
        }
        if(kinetics){var limit=Component.translatable("gui.omnira.spacetime_engine.limit",menu.limit());g.drawString(font,limit,(imageWidth-font.width(limit))/2,157,0xFFD0EAE4,false);}
    }
    @Override protected void renderContent(GuiGraphics g,int x,int y,float partial){
        super.renderContent(g,x,y,partial);renderTooltip(g,x,y);
        if(x-leftPos>=(kinetics?6:83)&&x-leftPos<(kinetics?55:160))for(int row=0;row<4;row++)if(y-topPos>=43+row*28&&y-topPos<61+row*28)
            g.renderTooltip(font,Component.translatable("gui.omnira.spacetime_engine."+new String[]{"north","east","south","west"}[menu.displaySide(row)]),x,y);
    }
    private final class SymbolButton extends Button {
        private final boolean apply;private final java.util.function.BooleanSupplier reversed;
        SymbolButton(int x,int y,Component tooltip,boolean apply,Runnable action,java.util.function.BooleanSupplier reversed){
            super(x,y,20,18,Component.empty(),b->action.run(),DEFAULT_NARRATION);this.apply=apply;this.reversed=reversed;setTooltip(Tooltip.create(tooltip));
        }
        @Override protected void renderWidget(GuiGraphics g,int x,int y,float partial){
            int a=getX(),b=getY(),ink=0xFFE3F5EC;MachinePanelArt.button(CrystalUi.pixels(g,a,b),20,18,active,isHoveredOrFocused());
            if(apply){for(int i=0;i<3;i++)g.fill(a+4+i,b+9+i,a+6+i,b+11+i,ink);for(int i=0;i<7;i++)g.fill(a+7+i,b+11-i,a+9+i,b+13-i,ink);}
            else{boolean r=reversed.getAsBoolean();g.fill(a+6,b+4,a+14,b+6,ink);g.fill(a+5,b+12,a+15,b+14,ink);
                g.fill(a+(r?14:4),b+5,a+(r?16:6),b+13,ink);g.fill(a+(r?4:14),b+9,a+(r?6:16),b+13,ink);
                for(int i=0;i<4;i++){int px=a+(r?5+i:14-i);g.fill(px,b+5-i,px+1,b+6+i,ink);}}
        }
    }
}
