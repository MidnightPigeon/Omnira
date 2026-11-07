package com.mcmagic.omnira.client.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

public final class WaymarkDeleteScreen extends ConfirmScreen {
    private final BooleanConsumer answer;
    public WaymarkDeleteScreen(Component name, BooleanConsumer answer) {
        super(answer, Component.translatable("gui.omnira.waymark.delete_title"),
                Component.translatable("gui.omnira.waymark.delete_confirm",name),Component.translatable("gui.yes"),Component.translatable("gui.cancel"));
        this.answer = answer;
    }
    @Override protected void addButtons(int y) {
        addRenderableWidget(new CrystalActionButton(width/2-155,y,150,20,Component.translatable("gui.yes"),b->answer.accept(true)));
        addRenderableWidget(new CrystalActionButton(width/2+5,y,150,20,Component.translatable("gui.cancel"),b->answer.accept(false)));
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial) {
        super.renderBackground(g,x,y,partial);
        MachinePanelArt.frame(CrystalUi.pixels(g,8,Math.max(4,height/6-18)),0,0,width-16,
                Math.min(height-8,height*2/3),0xF02A4355);
    }
    @Override public void onClose() { answer.accept(false); }
    @Override public boolean shouldCloseOnEsc() { return true; }
}
