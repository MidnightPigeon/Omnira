package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.engine.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SpacetimeEngineScreen extends ResponsiveContainerScreen<SpacetimeEngineMenu> {
    private final EditBox[] capacities=new EditBox[4];
    private final int[] lastCapacity={-1,-1,-1,-1};
    private Button outputSwitch;
    public SpacetimeEngineScreen(SpacetimeEngineMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=268;imageHeight=294;}
    @Override protected void init(){
        super.init();
        for(int i=0;i<4;i++){
            final int side=i;int y=topPos+43+i*28;
            int output=menu.displaySide(i);
            capacities[i]=number(leftPos+59,y,147,menu.capacity(output),6);
            lastCapacity[i]=menu.capacity(output);
            addRenderableWidget(new ApplyButton(leftPos+222,y,()->submit(side)));
        }
        outputSwitch=addRenderableWidget(new Button(leftPos+222,topPos+174,20,18,Component.empty(),button->{
            if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
        },supplier->supplier.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                MachinePanelArt.button(CrystalUi.pixels(g,getX(),getY()),20,18,active,isHoveredOrFocused());
                g.blit(net.minecraft.resources.ResourceLocation.parse("omnira:textures/gui/engine_controls.png"),getX()+2,getY()+1,0,0,16,16,64,16);
                g.fill(getX()+14,getY()+12,getX()+17,getY()+15,menu.enabled()?0xFF9CD8A6:0xFF68696E);
            }
        });
        switchTooltip();
    }
    private void switchTooltip(){outputSwitch.setTooltip(Tooltip.create(Component.translatable("gui.omnira.engine.enabled").append(": ").append(Component.translatable("options."+(menu.enabled()?"on":"off")))));}
    private EditBox number(int x,int y,int width,int value,int length){
        var box=new EditBox(font,x,y,width,18,Component.empty());box.setMaxLength(length);box.setFilter(s->s.matches("[0-9]*"));
        box.setBordered(false);box.setY(y+5);box.setHeight(13);
        box.setValue(Integer.toString(value));box.setTextColor(0xDFEFEC);addRenderableWidget(box);return box;
    }
    private void submit(int side){
        try{int potential=Integer.parseInt(capacities[side].getValue());
            if(potential<=menu.limit())PacketDistributor.sendToServer(new EngineConfigPayload(menu.containerId,menu.displaySide(side),potential,16,false));
        }catch(NumberFormatException ignored){}
    }
    @Override protected void containerTick(){
        super.containerTick();
        switchTooltip();
        for(int i=0;i<4;i++){
            int output=menu.displaySide(i);
            if(lastCapacity[i]!=menu.capacity(output)){lastCapacity[i]=menu.capacity(output);capacities[i].setValue(Integer.toString(lastCapacity[i]));}
        }
    }
    @Override public void resize(net.minecraft.client.Minecraft mc,int width,int height){
        String[] values=new String[4];int focus=-1;
        for(int i=0;i<4;i++)if(capacities[i]!=null){values[i]=capacities[i].getValue();if(capacities[i].isFocused())focus=i;}
        super.resize(mc,width,height);
        for(int i=0;i<4;i++)if(values[i]!=null)capacities[i].setValue(values[i]);
        if(focus>=0)setFocused(capacities[focus]);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key!=org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE&&key!=org.lwjgl.glfw.GLFW.GLFW_KEY_TAB)for(int i=0;i<4;i++)if(capacities[i].isFocused()){
            if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER||key==org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER){submit(i);return true;}
            return capacities[i].keyPressed(key,scan,modifiers)||capacities[i].canConsumeInput();
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        var art=CrystalUi.pixels(g,leftPos,topPos);
        MachinePanelArt.engineControls(art,true,CrystalUi.time(partial),new int[]{SpacetimeEngineGeometry.outputColor(0),
                SpacetimeEngineGeometry.outputColor(1),SpacetimeEngineGeometry.outputColor(2),SpacetimeEngineGeometry.outputColor(3)});
        CrystalWorkstationBackground.inventory(g,leftPos,topPos,menu,9);
        for(var slot:menu.slots){if(slot.container instanceof Inventory)continue;int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xFFC8E2DC);g.fill(x,y,x+16,y+16,0xFF304B58);}
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        CrystalUi.title(g,font,title,imageWidth/2,3,250,0xFFE4F1EC);
        g.drawCenteredString(font,"MPU",132,28,0xFFE4F1EC);
        for(int i=0;i<4;i++){
            int color=SpacetimeEngineGeometry.outputColor(i),y=48+i*28,x=7;g.fill(x,y-3,x+3,y+11,color);
            g.drawString(font,Component.translatable("gui.omnira.spacetime_engine."+new String[]{"front","right","back","left"}[i]),x+8,y,color,false);
        }
        var limit=Component.translatable("gui.omnira.pulse.limit",menu.limit());g.drawString(font,limit,(imageWidth-font.width(limit))/2,157,0xFFD0EAE4,false);
    }
    @Override protected void renderContent(GuiGraphics g,int x,int y,float partial){
        super.renderContent(g,x,y,partial);renderTooltip(g,x,y);
        if(x-leftPos>=6&&x-leftPos<55)for(int row=0;row<4;row++)if(y-topPos>=43+row*28&&y-topPos<61+row*28)
            g.renderTooltip(font,Component.translatable("gui.omnira.spacetime_engine."+new String[]{"north","east","south","west"}[menu.displaySide(row)]),x,y);
    }
    private final class ApplyButton extends Button {
        ApplyButton(int x,int y,Runnable action){
            super(x,y,20,18,Component.empty(),b->action.run(),DEFAULT_NARRATION);setTooltip(Tooltip.create(Component.translatable("gui.omnira.spacetime_engine.apply")));
        }
        @Override protected void renderWidget(GuiGraphics g,int x,int y,float partial){
            int a=getX(),b=getY(),ink=0xFFE3F5EC;MachinePanelArt.button(CrystalUi.pixels(g,a,b),20,18,active,isHoveredOrFocused());
            for(int i=0;i<3;i++)g.fill(a+4+i,b+9+i,a+6+i,b+11+i,ink);for(int i=0;i<7;i++)g.fill(a+7+i,b+11-i,a+9+i,b+13-i,ink);
        }
    }
}
