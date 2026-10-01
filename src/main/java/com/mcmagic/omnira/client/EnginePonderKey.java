package com.mcmagic.omnira.client;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.client.screen.EngineGuideScreen;
import com.mcmagic.omnira.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=Omnira.MOD_ID,value=Dist.CLIENT)
public final class EnginePonderKey {
    private static final int HOLD_TICKS=20;
    private static int held;
    private static boolean requireRelease;
    private static Screen previousScreen;
    public record HoldProgress(float fraction) implements TooltipComponent {}
    private static boolean hovered() {
        var screen=Minecraft.getInstance().screen;
        if(screen==null || screen instanceof EngineGuideScreen) return false;
        if(ModList.get().isLoaded("jei") && com.mcmagic.omnira.compat.jei.OmniraJeiPlugin.engineUnderMouse()) return true;
        if(screen instanceof AbstractContainerScreen<?> container) {
            var slot=container.getSlotUnderMouse();
            return slot!=null && slot.getItem().is(ModItems.MANA_ENGINE.get());
        }
        return false;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        boolean down=InputConstants.isKeyDown(mc.getWindow().getWindow(),GLFW.GLFW_KEY_W);
        if(!down) requireRelease=false;
        if(previousScreen!=mc.screen) {previousScreen=mc.screen;held=0;}
        if(mc.level==null || requireRelease || !down || !hovered()) {held=0;return;}
        if(++held>=HOLD_TICKS) {
            requireRelease=true;held=0;
            mc.setScreen(new EngineGuideScreen(mc.screen));
        }
    }
    @SubscribeEvent public static void tooltip(RenderTooltipEvent.GatherComponents event) {
        if(!event.getItemStack().is(ModItems.MANA_ENGINE.get()) && !hovered()) return;
        event.getTooltipElements().add(Either.left(Component.translatable("gui.omnira.engine.hold_w").withStyle(ChatFormatting.GRAY)));
        if(held>0) event.getTooltipElements().add(Either.right(new HoldProgress(held/(float)HOLD_TICKS)));
    }
    @SubscribeEvent public static void key(ScreenEvent.KeyPressed.Pre event) {
        if(event.getKeyCode()==GLFW.GLFW_KEY_W && hovered()) event.setCanceled(true);
    }
    @SubscribeEvent public static void character(ScreenEvent.CharacterTyped.Pre event) {
        if((event.getCodePoint()=='w' || event.getCodePoint()=='W') && hovered()) event.setCanceled(true);
    }
    @SubscribeEvent public static void tooltipFactory(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(HoldProgress.class,progress->new ClientTooltipComponent() {
            @Override public int getHeight() {return 7;}
            @Override public int getWidth(Font font) {return 96;}
            @Override public void renderImage(Font font,int x,int y,GuiGraphics graphics) {
                graphics.fill(x,y+1,x+96,y+5,0xFF6F727A);
                graphics.fill(x+1,y+2,x+95,y+4,0xFF24262E);
                graphics.fill(x+1,y+2,x+1+Math.round(94*progress.fraction()),y+4,0xFFF1E2AD);
            }
        });
    }
}
