package com.mcmagic.omnira.client;

import com.mcmagic.omnira.network.CondensationChannelPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** All table clicks are classified on release by the server; held items never receive a replay. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class CondensationInput {
    private static ClientLevel world;
    private static BlockHitResult target;
    private static int heldTicks;
    private static boolean waitRelease;
    private static boolean eligible(Minecraft mc){
        return mc.level!=null&&mc.player!=null&&mc.gameMode!=null&&mc.screen==null&&mc.isWindowActive()
                &&mc.player.isAlive()&&!mc.player.isSpectator()&&!mc.player.isUsingItem();
    }
    private static boolean matches(Minecraft mc){
        return mc.hitResult instanceof BlockHitResult hit&&hit.getType()==HitResult.Type.BLOCK
                &&hit.getBlockPos().equals(target.getBlockPos())
                &&mc.level.getBlockEntity(target.getBlockPos()) instanceof com.mcmagic.omnira.block.entity.SimpleCondensationTableBlockEntity;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(InputEvent.InteractionKeyMappingTriggered event){
        if(!event.isUseItem())return;
        var mc=Minecraft.getInstance();
        if(target!=null||waitRelease){event.setCanceled(true);event.setSwingHand(false);return;}
        if(event.getHand()!=InteractionHand.MAIN_HAND||!eligible(mc)||!(mc.hitResult instanceof BlockHitResult hit)
                ||hit.getType()!=HitResult.Type.BLOCK||!(mc.level.getBlockEntity(hit.getBlockPos()) instanceof com.mcmagic.omnira.block.entity.SimpleCondensationTableBlockEntity))return;
        event.setCanceled(true);event.setSwingHand(false);world=mc.level;target=hit;heldTicks=0;
        send(true);
    }
    private static void send(boolean held){
        if(Minecraft.getInstance().getConnection()!=null&&target!=null)
            PacketDistributor.sendToServer(new CondensationChannelPayload(held,target.getBlockPos(),false));
    }
    private static void stop(boolean cancel){
        if(target!=null&&Minecraft.getInstance().getConnection()!=null)PacketDistributor.sendToServer(new CondensationChannelPayload(false,target.getBlockPos(),cancel));
        target=null;world=null;heldTicks=0;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();boolean held=mc.options.keyUse.isDown();
        if(!held)waitRelease=false;
        if(target==null)return;
        boolean valid=world==mc.level&&eligible(mc)&&matches(mc);
        if(!held){
            stop(!valid);return;
        }
        if(!valid){stop(true);waitRelease=true;return;}
        if(++heldTicks%com.mcmagic.omnira.workstation.CondensationChannel.HEARTBEAT==0)send(true);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){target=null;world=null;heldTicks=0;waitRelease=false;}
    private CondensationInput(){}
}
