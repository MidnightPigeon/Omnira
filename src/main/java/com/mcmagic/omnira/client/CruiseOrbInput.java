package com.mcmagic.omnira.client;

import com.mcmagic.omnira.vehicle.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class CruiseOrbInput {
    private static boolean useHeld;
    private static boolean descentHeld;
    private static int lastKeys=-1,lastVehicle=-1;
    @SubscribeEvent public static void inventory(ScreenEvent.Opening event){
        var mc=Minecraft.getInstance();
        if(mc.player!=null && mc.player.getVehicle() instanceof CruiseOrbEntity && event.getNewScreen() instanceof InventoryScreen){
            event.setCanceled(true);PacketDistributor.sendToServer(new CruiseOrbPayload(3,0));
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void panel(InputEvent.InteractionKeyMappingTriggered event){
        var mc=Minecraft.getInstance();
        if(!event.isUseItem() || mc.player==null || !(mc.player.getVehicle() instanceof CruiseOrbEntity orb))return;
        int button=orb.pointedButton(mc.player);if(button<0)return;
        event.setCanceled(true);event.setSwingHand(false);
        if(!useHeld){useHeld=true;PacketDistributor.sendToServer(new CruiseOrbPayload(button+1,0));}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(!mc.options.keyUse.isDown())useHeld=false;
        if(mc.player==null || !(mc.player.getVehicle() instanceof CruiseOrbEntity) && !(mc.player.getVehicle() instanceof CrystalBroomEntity)){lastVehicle=-1;lastKeys=-1;useHeld=false;descentHeld=false;}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void movement(MovementInputUpdateEvent event){
        if(event.getEntity().getVehicle() instanceof CrystalBroomEntity broom){
            var mc=Minecraft.getInstance();var input=event.getInput();
            int keys=mc.screen!=null || !mc.isWindowActive()?0:(input.up?1:0)|(input.down?2:0)|(input.left?4:0)|(input.right?8:0)|(input.jumping?16:0)|(input.shiftKeyDown?32:0);
            if(keys!=lastKeys || lastVehicle!=broom.getId() || event.getEntity().tickCount%10==0){
                PacketDistributor.sendToServer(new CruiseOrbPayload(0,keys));lastKeys=keys;lastVehicle=broom.getId();
            }
            input.forwardImpulse=input.leftImpulse=0;input.up=input.down=input.left=input.right=input.jumping=false;
            return;
        }
        if(!(event.getEntity().getVehicle() instanceof CruiseOrbEntity orb))return;
        var mc=Minecraft.getInstance();var input=event.getInput();
        int keys=mc.screen!=null || !mc.isWindowActive()?0:(input.up?1:0)|(input.down?2:0)|(input.left?4:0)|(input.right?8:0)|(input.jumping?16:0)|(input.shiftKeyDown?32:0);
        if(keys!=lastKeys || lastVehicle!=orb.getId() || event.getEntity().tickCount%10==0){
            PacketDistributor.sendToServer(new CruiseOrbPayload(0,keys));lastKeys=keys;lastVehicle=orb.getId();
        }
        input.forwardImpulse=input.leftImpulse=0;input.up=input.down=input.left=input.right=input.jumping=false;
        boolean shift=input.shiftKeyDown;
        if(orb.started() || orb.getDeltaMovement().lengthSqr()>.000001){if(shift)descentHeld=true;input.shiftKeyDown=false;}
        else if(descentHeld)input.shiftKeyDown=false;
        if(!shift)descentHeld=false;
    }
}
