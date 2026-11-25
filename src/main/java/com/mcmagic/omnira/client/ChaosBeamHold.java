package com.mcmagic.omnira.client;

import com.mcmagic.omnira.network.ChaosBeamInput;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ChaosBeamHold {
    private static ChaosBeamInput held;
    public static void begin(ChaosBeamInput packet){held=packet;check();}
    private static void check(){
        var mc=Minecraft.getInstance();
        if(held==null)return;
        if(mc.player==null || mc.getConnection()==null){held=null;return;}
        if(mc.options.keyUse.isDown() && mc.screen==null && mc.isWindowActive())return;
        PacketDistributor.sendToServer(held);
        held=null;
    }
    private static void released(){
        if(held!=null && Minecraft.getInstance().getConnection()!=null)PacketDistributor.sendToServer(held);
        held=null;
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Post e){
        if(e.getAction()==org.lwjgl.glfw.GLFW.GLFW_RELEASE && Minecraft.getInstance().options.keyUse.matchesMouse(e.getButton()))released();
    }
    @SubscribeEvent public static void key(InputEvent.Key e){
        if(e.getAction()==org.lwjgl.glfw.GLFW.GLFW_RELEASE && Minecraft.getInstance().options.keyUse.matches(e.getKey(),e.getScanCode()))released();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){check();}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void repeat(InputEvent.InteractionKeyMappingTriggered e){
        if(com.mcmagic.omnira.client.guide.GuideProjection.controlsHand())return;
        if(held!=null && e.isUseItem()){e.setCanceled(true);e.setSwingHand(false);}
    }
    @SubscribeEvent public static void disconnect(ClientPlayerNetworkEvent.LoggingOut e){held=null;}
}
