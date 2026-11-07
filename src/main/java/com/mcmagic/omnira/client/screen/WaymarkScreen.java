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

public final class WaymarkScreen extends ResponsiveContainerScreen<WaymarkMenu> {
    private EditBox input;
    private final List<Button> rows=new ArrayList<>();
    private final List<Button> deleteButtons=new ArrayList<>();
    private List<Integer> filtered=List.of();
    private Button previous,next;
    private int page,rowCount,removedCount;
    private String[] labels,locations,searchTexts;
    private double[] costs;
    private Component[] rowTexts;
    public WaymarkScreen(WaymarkMenu menu,Inventory inventory,Component title) {super(menu,inventory,title);imageWidth=280;imageHeight=220;}
    @Override protected void init() {
        imageWidth=280;imageHeight=menu.editing?124:220;
        super.init();rows.clear();deleteButtons.clear();
        labels=new String[menu.entries.size()];locations=new String[labels.length];searchTexts=new String[labels.length];costs=new double[labels.length];rowTexts=new Component[labels.length];
        for(int i=0;i<labels.length;i++){labels[i]=uncachedLabel(i);locations[i]=location(menu.entries.get(i));searchTexts[i]=(labels[i]+" "+locations[i]).toLowerCase(Locale.ROOT);rowTexts[i]=Component.literal(labels[i]+"\n"+locations[i]);}
        input=addRenderableWidget(new EditBox(font,leftPos+12,topPos+35,imageWidth-24,16,Component.translatable(menu.editing?"gui.omnira.waymark.name":"gui.omnira.waymark.search")));
        input.setMaxLength(32);
        input.setBordered(false);
        input.setTextColor(MachinePanelArt.LIGHT);
        input.setTextColorUneditable(0xFF98AFB7);
        if(menu.editing) {
            input.setValue(menu.entries.getFirst().name());
            addRenderableWidget(new CrystalActionButton(leftPos+imageWidth-92,topPos+imageHeight-30,80,20,Component.translatable("gui.done"),
                    b->PacketDistributor.sendToServer(new RenameWaymarkPayload(menu.containerId,input.getValue()))));
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
                        MachinePanelArt.frame(CrystalUi.pixels(g,getX(),getY()),0,0,getWidth(),getHeight(),
                                isHoveredOrFocused()?0xFF486B7B:0xFF304D5D);
                        String cost="-"+(int)Math.ceil(cost(filtered.get(index)));
                        g.drawString(font,font.plainSubstrByWidth(label(filtered.get(index)),getWidth()-font.width(cost)-18),getX()+5,getY()+4,MachinePanelArt.LIGHT,false);
                        g.drawString(font,cost,getX()+getWidth()-font.width(cost)-5,getY()+4,active?MachinePanelArt.MINT:0xFFECAAA3,false);
                        g.pose().pushPose();g.pose().translate(getX()+5,getY()+15,0);g.pose().scale(.75F,.75F,1);
                        String detail=locations[filtered.get(index)];
                        g.drawString(font,font.plainSubstrByWidth(detail,(int)((getWidth()-10)/.75F)),0,0,0xFFBCD5DD,false);
                        g.pose().popPose();
                    }
                }));
                var delete = new CrystalActionButton(leftPos+imageWidth-32,topPos+61+row*28,20,20,Component.literal("×"),b->{
                    int index=page*rowCount+slot;
                    if(index<filtered.size()) confirmDelete(filtered.get(index));
                });
                delete.setTooltip(Tooltip.create(Component.translatable("gui.omnira.waymark.delete")));
                deleteButtons.add(addRenderableWidget(delete));
            }
            previous=addRenderableWidget(new CrystalActionButton(leftPos+imageWidth-54,topPos+imageHeight-28,20,20,Component.literal("<"),b->{page--;updateRows();}));
            previous.setTooltip(Tooltip.create(Component.translatable("guide.omnira.previous")));
            next=addRenderableWidget(new CrystalActionButton(leftPos+imageWidth-30,topPos+imageHeight-28,20,20,Component.literal(">"),b->{page++;updateRows();}));
            next.setTooltip(Tooltip.create(Component.translatable("guide.omnira.next")));
            input.setResponder(value->{page=0;filter();});filter();
        }
    }
    private void confirmDelete(int index) {
        String search = input.getValue();
        int currentPage = page;
        minecraft.setScreen(new WaymarkDeleteScreen(Component.literal(label(index)), accepted -> {
            if (minecraft.player == null || minecraft.player.containerMenu != menu) {
                minecraft.setScreen(null);
                return;
            }
            minecraft.setScreen(this);
            input.setValue(search);
            page = currentPage;
            updateRows();
            if (accepted) PacketDistributor.sendToServer(new SelectWaymarkPayload(menu.containerId,-index-1));
        }));
    }

    private String label(int index) {return labels[index];}
    private String uncachedLabel(int index) {
        var name=menu.entries.get(index).name();
        if(name.equals(com.mcmagic.omnira.spacetime.AmberDirectory.MARK_NAME))return Component.translatable(name).getString();
        return name.isBlank()?Component.translatable("gui.omnira.waymark.unnamed").getString():name;
    }
    private static String dimensionName(net.minecraft.resources.ResourceLocation dimension) {
        // Minecraft's language table already falls back to en_us before using the supplied ID path.
        return Component.translatableWithFallback(dimension.toLanguageKey("dimension"),dimension.getPath()).getString();
    }
    private static String location(com.mcmagic.omnira.travel.WaymarkDirectory.Entry entry) {
        String dimension = dimensionName(entry.dimension());
        if (entry.biome() == null || entry.biome().getPath().isBlank()) return dimension;
        String biome = Component.translatableWithFallback(entry.biome().toLanguageKey("biome"),entry.biome().getPath()).getString();
        return biome.isBlank() ? dimension : dimension + " · " + biome;
    }
    private double cost(int index) {return costs[index];}
    private double calculateCost(int index) {
        if(minecraft.player.getItemInHand(menu.hand).getItem() instanceof RecallCrystalItem crystal)
            return com.mcmagic.omnira.mana.ManaCosts.cost(minecraft.player,crystal.baseCost(!menu.entries.get(index).dimension().equals(minecraft.level.dimension().location())));
        return 0;
    }
    private void filter() {
        String query=input.getValue().toLowerCase(Locale.ROOT);
        filtered=java.util.stream.IntStream.range(0,menu.entries.size()).filter(i->!menu.removedEntries.contains(i) && searchTexts[i].contains(query)).boxed().toList();
        updateRows();
    }
    private void updateRows() {
        int pages=Math.max(1,(filtered.size()+rowCount-1)/rowCount);page=Math.clamp(page,0,pages-1);
        previous.active=page>0;next.active=page<pages-1;
        for(int i=0;i<rows.size();i++) {
            int index=page*rowCount+i;var row=rows.get(i);row.visible=index<filtered.size();
            if(row.visible)costs[filtered.get(index)]=calculateCost(filtered.get(index));
            deleteButtons.get(i).visible=row.visible;
            deleteButtons.get(i).active=row.visible && !menu.entries.get(filtered.get(index)).name().equals(com.mcmagic.omnira.spacetime.AmberDirectory.MARK_NAME);
            row.active=row.visible && minecraft.player.getData(ModAttachments.MANA).canSpend(cost(filtered.get(index)));
            if(row.visible) {
                var text=rowTexts[filtered.get(index)];
                if(!row.getMessage().equals(text)){
                    row.setMessage(text);
                    row.setTooltip(Tooltip.create(text));
                }
            }
        }
    }
    @Override public void resize(net.minecraft.client.Minecraft mc,int width,int height){
        String value=input==null?null:input.getValue();int previousPage=page;boolean focused=input!=null&&input.isFocused();
        super.resize(mc,width,height);
        if(value!=null){input.setValue(value);page=previousPage;if(!menu.editing)updateRows();if(focused)setFocused(input);}
    }
    @Override protected void containerTick() {super.containerTick();if(!menu.editing) {
        if(removedCount!=menu.removedEntries.size()){removedCount=menu.removedEntries.size();filter();}else updateRows();
    }}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        var pixels=CrystalUi.pixels(g,leftPos,topPos);
        MachinePanelArt.frame(pixels,0,0,imageWidth,imageHeight,0xF02A4355);
        pixels.fill(12,25,imageWidth-12,26,MachinePanelArt.RIM);
        MachinePanelArt.frame(pixels,9,29,imageWidth-18,23,MachinePanelArt.DARK);
        pixels.fill(14,25,30,26,MachinePanelArt.LILAC);
        pixels.fill(imageWidth-30,25,imageWidth-14,26,MachinePanelArt.LILAC);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        CrystalUi.label(g,font,title,12,12,imageWidth-24,1,MachinePanelArt.LIGHT);
        if(menu.editing) {
            var entry=menu.entries.getFirst();
            CrystalUi.label(g,font,Component.literal(location(entry)),12,62,imageWidth-24,1,0xFFBCD5DD);
        } else {
            ManaBar.render(g,font,minecraft.player.getData(ModAttachments.MANA),10,imageHeight-23,100);
            if(filtered.isEmpty()) g.drawString(font,Component.translatable("gui.omnira.waymark.empty"),12,64,0xFFBCD5DD,false);
        }
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(input.isFocused() && key!=256&&key!=org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) return input.keyPressed(key,scan,modifiers) || input.canConsumeInput();
        return super.keyPressed(key,scan,modifiers);
    }
}
