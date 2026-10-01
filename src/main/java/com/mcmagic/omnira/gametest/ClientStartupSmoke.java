package com.mcmagic.omnira.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Development-only: validates resource loading and client mixins, then exits normally. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ClientStartupSmoke {
    private static int readyTicks;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("omnira.clientSmoke"))return;
        var client=Minecraft.getInstance();
        if(client.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen && client.getOverlay()==null) {
            client.setScreen(new TitleScreen());
        }
        if(client.screen instanceof TitleScreen && client.getOverlay()==null && ++readyTicks==20) {
            if(org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(client.getWindow().getWindow(),org.lwjgl.glfw.GLFW.GLFW_VISIBLE)!=org.lwjgl.glfw.GLFW.GLFW_FALSE)
                throw new IllegalStateException("Smoke-test window must remain hidden");
            if(net.neoforged.fml.ModList.get().isLoaded("jei"))com.mcmagic.omnira.compat.jei.JeiPrioritySmoke.run();
            com.mojang.logging.LogUtils.getLogger().info("OMNIRA_CLIENT_SMOKE_PASSED: title screen and resources ready");
            client.stop();
        }
    }
}
