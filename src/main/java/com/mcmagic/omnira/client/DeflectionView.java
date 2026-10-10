package com.mcmagic.omnira.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Spread authoritative steering over frames without replacing the player's mouse input. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class DeflectionView {
    private static long lastFrame;
    private static Object lastLevel;
    @SubscribeEvent public static void frame(RenderFrameEvent.Pre event){
        var client=Minecraft.getInstance();long now=System.nanoTime();
        double seconds=lastFrame==0?0:Math.min(.1,(now-lastFrame)*1e-9);lastFrame=now;
        var player=client.player;
        if(lastLevel!=client.level){lastLevel=client.level;if(player!=null)player.getPersistentData().remove("DeflectionClientPending");return;}
        if(player==null||client.isPaused())return;
        var data=player.getPersistentData();float pending=data.getFloat("DeflectionClientPending");
        if(!player.isAlive()||player.isSpectator()){data.remove("DeflectionClientPending");return;}
        float angle=Math.abs(pending)<.001F?pending:(float)(pending*(1-Math.exp(-seconds/0.05)));
        if(angle==0)return;
        data.putFloat("DeflectionClientPending",pending-angle);
        player.setYRot(player.getYRot()+angle);player.yRotO+=angle;
        player.setYHeadRot(player.getYHeadRot()+angle);player.yHeadRotO+=angle;
        player.yBodyRot+=angle;player.yBodyRotO+=angle;
        player.setDeltaMovement(player.getDeltaMovement().yRot((float)-Math.toRadians(angle)));
    }
    private DeflectionView(){}
}
