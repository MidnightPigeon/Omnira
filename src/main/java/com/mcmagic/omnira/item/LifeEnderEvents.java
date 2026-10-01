package com.mcmagic.omnira.item;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.minecraft.world.effect.*;

@EventBusSubscriber(modid="omnira")
public final class LifeEnderEvents {
    @SubscribeEvent public static void attack(AttackEntityEvent event) {
        var p=event.getEntity();if(!LifeEnderItem.held(p))return;
        event.setCanceled(true);
        if(p instanceof net.minecraft.server.level.ServerPlayer server) {
            var back=p.getLookAngle().multiply(-1,0,-1).normalize().scale(1.15);
            p.setDeltaMovement(back.x,.4,back.z);p.fallDistance=0;p.hurtMarked=true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,20,1));
            server.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
        }
    }
}
