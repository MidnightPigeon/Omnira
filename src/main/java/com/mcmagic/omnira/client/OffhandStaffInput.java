package com.mcmagic.omnira.client;

import com.mcmagic.omnira.item.StaffItem;
import com.mcmagic.omnira.network.OffhandStaffPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class OffhandStaffInput {
    private static final com.mcmagic.omnira.item.staff.StaffUseClick CLICK=new com.mcmagic.omnira.item.staff.StaffUseClick();
    @SubscribeEvent public static void use(InputEvent.InteractionKeyMappingTriggered event) {
        if(!event.isUseItem() || event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        CLICK.begin();
    }
    public static void castPending() {
        if(!CLICK.consume())return;
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.screen!=null || !mc.isWindowActive() || mc.player.isSpectator() || mc.player.isShiftKeyDown())return;
        if(mc.player.getOffhandItem().getItem() instanceof StaffItem)
            PacketDistributor.sendToServer(new OffhandStaffPayload());
        // The caller resolves ordinary item use first, but defers sword healing.
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Post event) {
        if(event.getAction()==GLFW.GLFW_RELEASE && Minecraft.getInstance().options.keyUse.matchesMouse(event.getButton()))CLICK.release();
    }
    @SubscribeEvent public static void key(InputEvent.Key event) {
        if(event.getAction()==GLFW.GLFW_RELEASE && Minecraft.getInstance().options.keyUse.matches(event.getKey(),event.getScanCode()))CLICK.release();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || !mc.options.keyUse.isDown())CLICK.release();
    }
}
