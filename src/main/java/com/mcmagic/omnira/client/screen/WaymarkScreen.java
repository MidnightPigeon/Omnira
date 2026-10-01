package com.mcmagic.omnira.client.screen;

import com.mcmagic.omnira.client.hud.ManaBar;
import com.mcmagic.omnira.item.RecallCrystalItem;
import com.mcmagic.omnira.menu.WaymarkMenu;
import com.mcmagic.omnira.network.*;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class WaymarkScreen extends AbstractContainerScreen<WaymarkMenu> {
    private EditBox input;
    private final List<Button> rows=new ArrayList<>();
    private final List<Button> deleteButtons=new ArrayList<>();
    private List<Integer> filtered=List.of();
    private Button previous,next;
    private int page,rowCount;
    public WaymarkScreen(WaymarkMenu menu,Inventory inventory,Component title) {super(menu,inventory,title);imageWidth=256;imageHeight=220;}
    @Override protected void init() {
        imageWidth=Math.min(256,width-16);imageHeight=menu.editing?124:Math.min(220,height-16);
        super.init();rows.clear();deleteButtons.clear();
        input=addRenderableWidget(new EditBox(font,leftPos+12,topPos+32,imageWidth-24,18,Component.translatable(menu.editing?"gui.omnira.waymark.name":"gui.omnira.waymark.search")));
        input.setMaxLength(32);
        if(menu.editing) {
            input.setValue(menu.entries.getFirst().name());
            addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->PacketDistributor.sendToServer(new RenameWaymarkPayload(menu.containerId,input.getValue())))
                    .bounds(leftPos+imageWidth-92,topPos+imageHeight-30,80,20).build());
            setInitialFocus(input);
        } else {
            input.setHint(Component.translatable("gui.omnira.waymark.search"));
            rowCount=Math.max(1,(imageHeight-90)/28);
            for(int row=0;row<rowCount;row++) {
                final int slot=row;
                rows.add(addRenderableWidget(new Button(leftPos+10,topPos+58+row*28,imageWidth-46,26,Component.empty(),b->{
                    int index=page*rowCount+slot;
                    if(index<filtered.size()) PacketDistributor.sendToServer(new SelectWaymarkPayload(menu.containerId,filtered.get(index)));
                },supplier->supplier.get()) {
                    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
                        int index=page*rowCount+slot;if(index>=filtered.size()) return;
                        var entry=menu.entries.get(filtered.get(index));
                        g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),isHoveredOrFocused()?0xFFACBAC0:0xFFB5B5B5);
                        g.hLine(getX(),getX()+getWidth()-1,getY(),0xFFEAEAEA);
                        g.hLine(getX(),getX()+getWidth()-1,getY()+getHeight()-1,0xFF777777);
                        String cost="-"+(int)Math.ceil(cost(filtered.get(index)));
                        g.drawString(font,font.plainSubstrByWidth(label(filtered.get(index)),getWidth()-font.width(cost)-18),getX()+5,getY()+4,0xFF222222,false);
                        g.drawString(font,cost,getX()+getWidth()-font.width(cost)-5,getY()+4,active?0xFF334B53:0xFF92504B,false);
                        g.pose().pushPose();g.pose().translate(getX()+5,getY()+15,0);g.pose().scale(.75F,.75F,1);
                        String detail=dimensionName(entry.dimension());
                        g.drawString(font,font.plainSubstrByWidth(detail,(int)((getWidth()-10)/.75F)),0,0,0xFF4A4A4A,false);
                        g.pose().popPose();
                    }
                }));
                deleteButtons.add(addRenderableWidget(Button.builder(Component.literal("×"),b->{
                    int index=page*rowCount+slot;
                    if(index<filtered.size()) PacketDistributor.sendToServer(new SelectWaymarkPayload(menu.containerId,-filtered.get(index)-1));
                }).bounds(leftPos+imageWidth-32,topPos+61+row*28,20,20)
                        .tooltip(Tooltip.create(Component.translatable("gui.omnira.waymark.delete"))).build()));
            }
            previous=addRenderableWidget(Button.builder(Component.literal("<"),b->{page--;updateRows();}).bounds(leftPos+imageWidth-54,topPos+imageHeight-28,20,20).build());
            next=addRenderableWidget(Button.builder(Component.literal(">"),b->{page++;updateRows();}).bounds(leftPos+imageWidth-30,topPos+imageHeight-28,20,20).build());
            input.setResponder(value->{page=0;filter();});filter();
        }
    }
    private String label(int index) {
        var name=menu.entries.get(index).name();
        if(name.equals(com.mcmagic.omnira.spacetime.AmberDirectory.MARK_NAME))return Component.translatable(name).getString();
        return name.isBlank()?Component.translatable("gui.omnira.waymark.unnamed").getString():name;
    }
    private static String dimensionName(net.minecraft.resources.ResourceLocation dimension) {
        String key=dimension.toLanguageKey("dimension");
        return net.minecraft.client.resources.language.I18n.exists(key)
                ? Component.translatable(key).getString() : dimension.toString();
    }
    private double cost(int index) {
        if(minecraft.player.getItemInHand(menu.hand).getItem() instanceof RecallCrystalItem crystal)
            return com.mcmagic.omnira.mana.ManaCosts.cost(minecraft.player,crystal.baseCost(!menu.entries.get(index).dimension().equals(minecraft.level.dimension().location())));
        return 0;
    }
    private void filter() {
        String query=input.getValue().toLowerCase(Locale.ROOT);
        filtered=java.util.stream.IntStream.range(0,menu.entries.size()).filter(i->(label(i)+" "+dimensionName(menu.entries.get(i).dimension())).toLowerCase(Locale.ROOT).contains(query)).boxed().toList();
        updateRows();
    }
    private void updateRows() {
        int pages=Math.max(1,(filtered.size()+rowCount-1)/rowCount);page=Math.clamp(page,0,pages-1);
        previous.active=page>0;next.active=page<pages-1;
        for(int i=0;i<rows.size();i++) {
            int index=page*rowCount+i;var row=rows.get(i);row.visible=index<filtered.size();
            deleteButtons.get(i).visible=row.visible;
            deleteButtons.get(i).active=row.visible && !menu.entries.get(filtered.get(index)).name().equals(com.mcmagic.omnira.spacetime.AmberDirectory.MARK_NAME);
            row.active=row.visible && minecraft.player.getData(ModAttachments.MANA).canSpend(cost(filtered.get(index)));
            if(row.visible) row.setMessage(Component.literal(label(filtered.get(index))));
        }
    }
    @Override protected void containerTick() {super.containerTick();if(!menu.editing) updateRows();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFFC6C6C6);
        CrystalProcessingTableScreen.renderVanillaFrame(g,leftPos,topPos,imageWidth,imageHeight);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        g.drawString(font,title,12,12,0xFF303030,false);
        if(menu.editing) {
            var entry=menu.entries.getFirst();
            g.drawString(font,font.plainSubstrByWidth(dimensionName(entry.dimension()),imageWidth-24),12,60,0xFF555555,false);
        } else {
            ManaBar.render(g,font,minecraft.player.getData(ModAttachments.MANA),10,imageHeight-23,100);
            if(filtered.isEmpty()) g.drawString(font,Component.translatable("gui.omnira.waymark.empty"),12,64,0xFF555555,false);
        }
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(input.isFocused() && key!=256) return input.keyPressed(key,scan,modifiers) || input.canConsumeInput();
        return super.keyPressed(key,scan,modifiers);
    }
}
