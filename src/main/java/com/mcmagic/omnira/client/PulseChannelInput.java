package com.mcmagic.omnira.client;

import com.mcmagic.omnira.network.PulseChannelPayload;
import com.mcmagic.omnira.pulse.PulseChanneling;
import com.mcmagic.omnira.pulse.PulseChannelTargets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Defer only bare-handed receiver clicks; a short release replays the normal block interaction. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class PulseChannelInput {
    private static ClientLevel world;
    private static BlockHitResult target;
    private static int heldTicks;
    private static boolean waitRelease;
    private static boolean eligible(Minecraft mc){
        return mc.level!=null&&mc.player!=null&&mc.gameMode!=null&&mc.screen==null&&mc.isWindowActive()
                &&mc.player.isAlive()&&!mc.player.isSpectator()&&!mc.player.isUsingItem()
                &&mc.player.getMainHandItem().isEmpty()&&mc.player.getOffhandItem().isEmpty();
    }
    private static boolean matches(Minecraft mc){
        return mc.hitResult instanceof BlockHitResult hit&&hit.getType()==HitResult.Type.BLOCK
                &&hit.getBlockPos().equals(target.getBlockPos())&&hit.getDirection()==target.getDirection()
                &&PulseChannelTargets.accepts(mc.level,target.getBlockPos(),target.getDirection());
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void use(InputEvent.InteractionKeyMappingTriggered event){
        if(!event.isUseItem())return;
        var mc=Minecraft.getInstance();
        if(target!=null||waitRelease){event.setCanceled(true);event.setSwingHand(false);return;}
        if(event.getHand()!=InteractionHand.MAIN_HAND||!eligible(mc)||!(mc.hitResult instanceof BlockHitResult hit)
                ||hit.getType()!=HitResult.Type.BLOCK||!PulseChannelTargets.accepts(mc.level,hit.getBlockPos(),hit.getDirection()))return;
        event.setCanceled(true);event.setSwingHand(false);world=mc.level;target=hit;heldTicks=0;
        PulseChannelVisuals.receive(mc.player.getUUID(),0);send(true);
    }
    private static void send(boolean held){
        if(Minecraft.getInstance().getConnection()!=null&&target!=null)
            PacketDistributor.sendToServer(new PulseChannelPayload(held,target.getBlockPos(),target.getDirection()));
    }
    private static void stop(){
        var mc=Minecraft.getInstance();send(false);
        if(mc.player!=null)PulseChannelVisuals.receive(mc.player.getUUID(),-1);
        target=null;world=null;heldTicks=0;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();boolean held=mc.options.keyUse.isDown();
        if(!held)waitRelease=false;
        if(target==null)return;
        boolean valid=world==mc.level&&eligible(mc)&&matches(mc);
        if(!held){
            var hit=target;boolean click=valid&&heldTicks<=PulseChanneling.SHORT_CLICK_TICKS;
            stop();if(click)normalUse(mc,hit);return;
        }
        if(!valid){stop();waitRelease=true;return;}
        if(++heldTicks%PulseChanneling.HEARTBEAT_TICKS==0)send(true);
    }
    private static void normalUse(Minecraft mc,BlockHitResult hit){
        for(var hand:InteractionHand.values()){
            var result=mc.gameMode.useItemOn(mc.player,hand,hit);
            if(result.consumesAction()){if(result.shouldSwing())mc.player.swing(hand);return;}
            if(result==InteractionResult.FAIL)return;
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){target=null;world=null;heldTicks=0;waitRelease=false;}
    private PulseChannelInput(){}
}
