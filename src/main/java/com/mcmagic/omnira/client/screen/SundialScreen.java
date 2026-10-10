package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.time.SundialBlockEntity;
import com.mcmagic.omnira.time.SundialMenu;
import com.mcmagic.omnira.time.SundialTimeline;
import com.mcmagic.omnira.time.SundialSelectionPayload;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import static com.mcmagic.omnira.time.SundialLayout.*;

public final class SundialScreen extends ResponsiveContainerScreen<SundialMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("omnira","textures/gui/sundial.png");
    private List<ItemStack> choices=List.of();
    private ItemStack choiceInput=ItemStack.EMPTY;
    private boolean consumeRelease;
    public SundialScreen(SundialMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);imageWidth=WIDTH;imageHeight=HEIGHT;
        inventoryLabelX=INVENTORY_X;inventoryLabelY=151;
    }
    private boolean over(double mx,double my,int x,int y,int w,int h){
        return mx>=leftPos+x&&mx<leftPos+x+w&&my>=topPos+y&&my<topPos+y+h;
    }
    @Override protected boolean mouseClickedContent(double mx,double my,int button){
        if(!choices.isEmpty()){
            consumeRelease=true;
            int index=hoveredChoice(mx,my);
            if(button==0&&index>=0&&index<choices.size())
                PacketDistributor.sendToServer(new SundialSelectionPayload(menu.containerId,choiceInput,choices.get(index)));
            choices=List.of();
            return true;
        }
        if(button==0&&over(mx,my,CHOICE_X-1,CHOICE_Y-1,18,18)){
            consumeRelease=true;
            choiceInput=menu.getSlot(0).getItem().copyWithCount(1);
            choices=SundialTimeline.choices(choiceInput);
            return true;
        }
        return super.mouseClickedContent(mx,my,button);
    }
    private float choiceScale(){return com.mcmagic.omnira.time.SundialLayout.choiceScale(choices.size());}
    private float choiceLeft(){return leftPos+CHOICE_X+8-choiceSide(choices.size())*10*choiceScale();}
    private float choiceTop(){return topPos+CHOICE_Y+8-choiceSide(choices.size())*10*choiceScale();}
    private int hoveredChoice(double mx,double my){
        int side=choiceSide(choices.size());float cell=20*choiceScale();
        double x=(mx-choiceLeft())/cell,y=(my-choiceTop())/cell;
        if(x<0||y<0||x>=side||y>=side)return -1;
        return choiceIndex((int)y*side+(int)x,side);
    }
    @Override protected boolean mouseReleasedContent(double x,double y,int button){
        if(consumeRelease){consumeRelease=false;return true;}
        return !choices.isEmpty()||super.mouseReleasedContent(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        return !choices.isEmpty()||super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){
        return !choices.isEmpty()||super.mouseScrolled(x,y,dx,dy);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(!choices.isEmpty()){
            if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)choices=List.of();
            return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }
    private void renderChoices(GuiGraphics g,int mouseX,int mouseY){
        int side=choiceSide(choices.size()),hover=hoveredChoice(mouseX,mouseY);
        g.pose().pushPose();
        g.pose().translate(choiceLeft(),choiceTop(),400);
        float scale=choiceScale();g.pose().scale(scale,scale,1);
        g.fill(-2,-2,side*20+2,side*20+2,0xFFD3E7DA);
        g.fill(0,0,side*20,side*20,0xFF304D45);
        var selected=menu.selectedOutput();
        for(int cell=0;cell<side*side;cell++){
            int index=choiceIndex(cell,side),x=cell%side*20,y=cell/side*20;
            boolean center=index<0,valid=index>=0&&index<choices.size();
            var stack=center?selected:valid?choices.get(index):ItemStack.EMPTY;
            boolean active=center||valid&&ItemStack.isSameItemSameComponents(stack,selected);
            g.fill(x+1,y+1,x+19,y+19,active?0xFFE4D59A:valid&&index==hover?0xFFC4F1DC:0xFF739A87);
            g.fill(x+2,y+2,x+18,y+18,center?0xFF547563:0xFF3E6056);
            if(!stack.isEmpty())g.renderItem(stack,x+2,y+2);
        }
        g.pose().popPose();
        if(hover>=0&&hover<choices.size()){
            g.pose().pushPose();g.pose().translate(0,0,600);
            g.renderTooltip(font,choices.get(hover),mouseX,mouseY);g.pose().popPose();
        }
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.blit(TEXTURE,leftPos,topPos,0,0,WIDTH,HEIGHT,WIDTH,HEIGHT);
        var art=CrystalUi.pixels(g,leftPos,topPos);
        MachinePanelArt.sundialTrim(art);
        int energyHeight=ENERGY_HEIGHT*menu.energy()/SundialBlockEntity.CAPACITY;
        g.fill(leftPos+ENERGY_X,topPos+ENERGY_Y+ENERGY_HEIGHT-energyHeight,
                leftPos+ENERGY_X+ENERGY_WIDTH,topPos+ENERGY_Y+ENERGY_HEIGHT,0xFFB8FFD0);
        int width=PROGRESS_WIDTH*menu.progress()/SundialBlockEntity.DURATION;
        g.fill(leftPos+PROGRESS_X,topPos+PROGRESS_Y,leftPos+PROGRESS_X+width,
                topPos+PROGRESS_Y+PROGRESS_HEIGHT,0xFFFFDB88);
        if(width>0)art.fill(PROGRESS_X+width-1,PROGRESS_Y,PROGRESS_X+width,PROGRESS_Y+PROGRESS_HEIGHT,0xFFFFF0C3);
        var chosen=menu.selectedOutput();
        if(!chosen.isEmpty())g.renderItem(chosen,leftPos+CHOICE_X,topPos+CHOICE_Y);
        if(over(mouseX,mouseY,CHOICE_X-1,CHOICE_Y-1,18,18))
            g.fill(leftPos+CHOICE_X,topPos+CHOICE_Y,leftPos+CHOICE_X+16,topPos+CHOICE_Y+16,0x40FFFFFF);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xE4ECE8,false);
    }
    @Override protected void renderContent(GuiGraphics g,int mouseX,int mouseY,float partial){
        if(!choices.isEmpty()&&!ItemStack.isSameItemSameComponents(choiceInput,menu.getSlot(0).getItem()))choices=List.of();
        super.renderContent(g,mouseX,mouseY,partial);
        if(!choices.isEmpty()){renderChoices(g,mouseX,mouseY);return;}
        renderTooltip(g,mouseX,mouseY);
        if(over(mouseX,mouseY,CHOICE_X-1,CHOICE_Y-1,18,18)){
            var chosen=menu.selectedOutput();
            Component text=Component.translatable("gui.omnira.sundial.choose");
            if(!chosen.isEmpty())text=chosen.getHoverName().copy().append(" - ").append(text);
            g.renderTooltip(font,text,mouseX,mouseY);
        }else if(over(mouseX,mouseY,PROGRESS_X-1,PROGRESS_Y-1,PROGRESS_WIDTH+2,PROGRESS_HEIGHT+2)){
            g.renderTooltip(font,Component.translatable("gui.omnira.sundial.progress",menu.progress()/20,SundialBlockEntity.DURATION/20),mouseX,mouseY);
        }else if(over(mouseX,mouseY,ENERGY_X-2,ENERGY_Y-2,ENERGY_WIDTH+4,ENERGY_HEIGHT+4)){
            g.renderTooltip(font,Component.translatable("gui.omnira.sundial.energy",menu.energy(),SundialBlockEntity.CAPACITY),mouseX,mouseY);
        }
    }
}
