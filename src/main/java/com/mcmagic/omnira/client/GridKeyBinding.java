package com.mcmagic.omnira.client;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.network.OpenGridPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=Omnira.MOD_ID,value=Dist.CLIENT)
public final class GridKeyBinding {
    public static final KeyMapping OPEN=new KeyMapping("key.omnira.open_grid",KeyConflictContext.IN_GAME,
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.omnira");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {event.register(OPEN);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        while(OPEN.consumeClick()) {
            var mc=Minecraft.getInstance();
            if(mc.player!=null && mc.screen==null) PacketDistributor.sendToServer(OpenGridPayload.INSTANCE);
        }
    }
}
