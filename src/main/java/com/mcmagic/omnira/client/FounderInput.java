package com.mcmagic.omnira.client;

import com.mcmagic.omnira.weaver.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class FounderInput {
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered event){
        var mc=Minecraft.getInstance();
        if(event.isAttack()&&mc.screen==null&&mc.player!=null&&FounderApprovalItem.held(mc.player))
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new FounderActionPayload());
    }
}
