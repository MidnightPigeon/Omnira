package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.workstation.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.network.PacketDistributor;

/** Vanilla machine layouts, with separate extensions for the added utility controls. */
public final class CrystalUtilityScreen extends ResponsiveContainerScreen<CrystalWorkstationMenu> {
    private static final ResourceLocation[] BACKGROUNDS={texture("furnace"),texture("grindstone"),texture("anvil")};
    public static final int BAR_X=68,BAR_Y=68,BAR_WIDTH=40,BAR_HEIGHT=6;
    public static final int LIST_X=180,LIST_Y=19,LIST_WIDTH=114,ROW_HEIGHT=14,ROWS=4;
    private EditBox name;
    private ItemStack previousInput=ItemStack.EMPTY;
    private boolean updatingName;
    private int scroll;
    private static ResourceLocation texture(String id){return ResourceLocation.withDefaultNamespace("textures/gui/container/"+id+".png");}
    private static ResourceLocation sprite(String id){return ResourceLocation.withDefaultNamespace("container/"+id);}
    public CrystalUtilityScreen(CrystalWorkstationMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);imageWidth=176;imageHeight=166;inventoryLabelY=menu.index%3==2?74:72;
    }
    @Override protected int rightExtension(){return menu.index==4?122:0;}
    @Override protected void init(){
        super.init();
        if(menu.index%3==2){
            name=addRenderableWidget(new EditBox(font,leftPos+62,topPos+24,103,12,Component.translatable("container.repair")));
            name.setBordered(false);name.setTextColor(0xFFFFFFFF);name.setTextColorUneditable(0xFFFFFFFF);name.setMaxLength(50);
            name.setResponder(value->{if(!updatingName&&menu.getSlot(0).hasItem())PacketDistributor.sendToServer(new WorkstationConfigPayload(menu.containerId,value,true));});
            syncName();
        }
    }
    private void syncName(){
        var input=menu.getSlot(0).getItem();previousInput=input.copy();updatingName=true;
        name.setValue(input.isEmpty()?"":input.getHoverName().getString());name.setEditable(!input.isEmpty());updatingName=false;
    }
    @Override protected void containerTick(){
        super.containerTick();if(name!=null&&!ItemStack.matches(previousInput,menu.getSlot(0).getItem()))syncName();
    }
    @Override public void resize(net.minecraft.client.Minecraft mc,int width,int height){
        String value=name==null?null:name.getValue();super.resize(mc,width,height);
        if(value!=null&&name!=null){updatingName=true;name.setValue(value);updatingName=false;}
    }
    private java.util.List<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> enchantments(){
        var all=new java.util.TreeMap<String,net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>>();
        for(int i=0;i<2;i++)for(var e:EnchantmentHelper.getEnchantmentsForCrafting(menu.getSlot(i).getItem()).keySet())all.put(e.getRegisteredName(),e);
        return java.util.List.copyOf(all.values());
    }
    private boolean selected(net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> e){
        var work=CrystalWorkstations.work(minecraft.level.getBlockEntity(menu.position));
        return work==null||!work.removedEnchantments.contains(e.getRegisteredName());
    }
    private boolean overList(double x,double y){return menu.index==4&&x>=leftPos+LIST_X&&x<leftPos+LIST_X+LIST_WIDTH&&y>=topPos+LIST_Y&&y<topPos+LIST_Y+ROWS*ROW_HEIGHT;}
    private boolean selectionAvailable(){return WorkstationRecipes.singleInput(menu.getSlot(0).getItem(),menu.getSlot(1).getItem());}
    @Override protected boolean hasClickedOutside(double x,double y,int guiLeft,int guiTop,int button){
        if(menu.index==4&&x>=guiLeft+176&&x<guiLeft+298&&y>=guiTop&&y<guiTop+80)return false;
        return super.hasClickedOutside(x,y,guiLeft,guiTop,button);
    }
    @Override protected boolean mouseClickedContent(double x,double y,int button){
        if(button==0&&overList(x,y)){
            if(!selectionAvailable())return true;
            var entries=enchantments();int i=scroll+(int)(y-topPos-LIST_Y)/ROW_HEIGHT;
            if(i<entries.size())PacketDistributor.sendToServer(new WorkstationConfigPayload(menu.containerId,entries.get(i).getRegisteredName(),false));
            return true;
        }
        return super.mouseClickedContent(x,y,button);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(name!=null&&name.isFocused()&&key!=org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)return name.keyPressed(key,scan,modifiers)||name.canConsumeInput();
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){
        if(overList(x/uiScale(),y/uiScale())){if(selectionAvailable())scroll=Math.clamp(scroll-(int)Math.signum(dy),0,Math.max(0,enchantments().size()-ROWS));return true;}
        return super.mouseScrolled(x,y,dx,dy);
    }
    private void magicFuel(GuiGraphics g,int amount){
        int x=leftPos+56,y=topPos+36;g.fill(x,y,x+14,y+14,0xFFC6C6C6);
        for(int row=0;row<14;row++){
            int half=Math.min(row,13-row),start=x+6-half,end=x+8+half;
            g.fill(start,y+row,end,y+row+1,0xFF436A7C);
            if(half>0)g.fill(start+1,y+row,end-1,y+row+1,row>=14-amount?0xFFB6F5EB:0xFF7797A5);
        }
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        int kind=menu.index%3;g.blit(BACKGROUNDS[kind],leftPos,topPos,0,0,imageWidth,imageHeight);
        if(kind==0){
            int fuel=menu.value(6)>0?14:menu.value(4)>0?Math.clamp((int)Math.ceil(14.0*menu.value(3)/menu.value(4)),0,14):0;
            if(menu.index==3)magicFuel(g,fuel);
            else if(fuel>0)g.blitSprite(sprite("furnace/lit_progress"),14,14,0,14-fuel,leftPos+56,topPos+36+14-fuel,14,fuel);
            int progress=menu.value(2)>0?Math.clamp((int)Math.ceil(24.0*menu.value(1)/menu.value(2)),0,24):0;
            if(progress>0)g.blitSprite(sprite("furnace/burn_progress"),24,16,0,0,leftPos+79,topPos+34,progress,16);
        }else if(kind==1){
            if((menu.getSlot(0).hasItem()||menu.getSlot(1).hasItem())&&!menu.getSlot(2).hasItem())g.blitSprite(sprite("grindstone/error"),leftPos+92,topPos+31,28,21);
        }else{
            g.blitSprite(sprite(menu.getSlot(0).hasItem()?"anvil/text_field":"anvil/text_field_disabled"),leftPos+59,topPos+20,110,16);
            if((menu.getSlot(0).hasItem()||menu.getSlot(1).hasItem())&&!menu.getSlot(2).hasItem())g.blitSprite(sprite("anvil/error"),leftPos+99,topPos+45,28,21);
            int fill=menu.index==2?Math.clamp(menu.value(0),0,16)*(BAR_WIDTH-2)/16:menu.value(2)>0?(int)Math.clamp((long)menu.value(1)*(BAR_WIDTH-2)/menu.value(2),0,BAR_WIDTH-2):0;
            int x=leftPos+BAR_X,y=topPos+BAR_Y;g.fill(x,y,x+BAR_WIDTH,y+BAR_HEIGHT,0xFF373737);g.fill(x+1,y+1,x+BAR_WIDTH-1,y+BAR_HEIGHT-1,0xFF797979);
            g.fill(x+1,y+1,x+1+fill,y+BAR_HEIGHT-1,menu.index==2?0xFF87C8B0:0xFF8ACBDD);
        }
        if(menu.index==4){
            int x=leftPos+176;g.fill(x,topPos,x+122,topPos+80,0xFF373737);g.fill(x+1,topPos+1,x+121,topPos+79,0xFFFFFFFF);g.fill(x+3,topPos+3,x+119,topPos+77,0xFFC6C6C6);
            CrystalUi.title(g,font,Component.translatable("gui.omnira.workstation.enchantments"),x+61,topPos+6,110,0xFF404040);
            if(menu.getSlot(0).hasItem()&&menu.getSlot(1).hasItem()){
                CrystalUi.title(g,font,Component.translatable("gui.omnira.workstation.unavailable"),x+61,topPos+40,110,0xFF606060);
                return;
            }
            var list=enchantments();scroll=Math.min(scroll,Math.max(0,list.size()-ROWS));
            for(int row=0;row<ROWS&&row+scroll<list.size();row++){
                var e=list.get(row+scroll);int y=topPos+LIST_Y+row*ROW_HEIGHT;boolean keep=selected(e);
                g.fill(leftPos+LIST_X,y,leftPos+LIST_X+LIST_WIDTH,y+12,keep?0xFFB4CDC0:0xFFD6BABE);
                g.drawString(font,keep?"+":"-",leftPos+LIST_X+3,y+2,0xFF404040,false);
                g.drawString(font,font.plainSubstrByWidth(e.value().description().getString(),98),leftPos+LIST_X+13,y+2,0xFF404040,false);
            }
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        int x=menu.index%3==2?60:8;
        CrystalUi.title(g,font,title,(x+imageWidth-8)/2,6,imageWidth-x-8,0xFF404040);
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xFF404040,false);
        if(menu.index%3==2&&menu.value(5)>0&&menu.getSlot(2).hasItem()){
            int color=minecraft.player.getAbilities().instabuild||minecraft.player.experienceLevel>=menu.value(5)?0xFF408020:0xFFB03030;
            CrystalUi.title(g,font,Component.translatable("container.repair.cost",menu.value(5)),142,67,56,color);
        }
    }
    @Override protected void renderContent(GuiGraphics g,int mx,int my,float partial){
        super.renderContent(g,mx,my,partial);renderTooltip(g,mx,my);
        if(menu.index%3==2&&mx>=leftPos+BAR_X&&mx<leftPos+BAR_X+BAR_WIDTH&&my>=topPos+BAR_Y&&my<topPos+BAR_Y+BAR_HEIGHT)
            g.renderTooltip(font,Component.translatable(menu.index==2?"gui.omnira.workstation.durability":"gui.omnira.workstation.forging",menu.index==2?menu.value(0):menu.value(1)/20,menu.value(2)/20),mx,my);
        if(menu.index%3==2&&isHovering(menu.getSlot(2).x,menu.getSlot(2).y,16,16,mx,my)&&menu.value(5)>0)
            g.renderTooltip(font,Component.translatable("container.repair.cost",menu.value(5)),mx,my);
        if(selectionAvailable()&&overList(mx,my)){
            var list=enchantments();int i=scroll+(my-topPos-LIST_Y)/ROW_HEIGHT;
            if(i<list.size())g.renderTooltip(font,list.get(i).value().description().copy().append(Component.translatable(selected(list.get(i))?"gui.omnira.workstation.keep":"gui.omnira.workstation.remove")),mx,my);
        }
    }
}
