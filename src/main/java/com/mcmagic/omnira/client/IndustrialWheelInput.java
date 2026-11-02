package com.mcmagic.omnira.client;
import com.mcmagic.omnira.item.IndustrialWheelItem;
import com.mcmagic.omnira.item.staff.StaffUseClick;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class IndustrialWheelInput {
    private static final StaffUseClick CLICK=new StaffUseClick();
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(InputEvent.InteractionKeyMappingTriggered e){
        var mc=Minecraft.getInstance();
        if(!e.isUseItem()||mc.player==null||mc.screen!=null||!IndustrialWheelItem.available())return;
        if(!(mc.player.getMainHandItem().getItem() instanceof IndustrialWheelItem)&&!(mc.player.getOffhandItem().getItem() instanceof IndustrialWheelItem))return;
        e.setCanceled(true);e.setSwingHand(false);
        CLICK.begin();if(CLICK.consume())PacketDistributor.sendToServer(new com.mcmagic.omnira.network.IndustrialWheelPayload());
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||!mc.options.keyUse.isDown()||mc.screen!=null)CLICK.release();}
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Post e){if(e.getAction()==GLFW.GLFW_RELEASE&&Minecraft.getInstance().options.keyUse.matchesMouse(e.getButton()))CLICK.release();}
    @SubscribeEvent public static void key(InputEvent.Key e){if(e.getAction()==GLFW.GLFW_RELEASE&&Minecraft.getInstance().options.keyUse.matches(e.getKey(),e.getScanCode()))CLICK.release();}
}
