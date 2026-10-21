package com.mcmagic.omnira.weaver;

import com.mcmagic.omnira.entity.WeaverSpirit;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid="omnira")
public final class WeaverEvents {
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event){
        if(event.getNewDamage()<=0)return;
        if(event.getEntity() instanceof WeaverSpirit boss)boss.attacked(event.getSource());
        if(event.getSource().getEntity() instanceof WeaverSpirit boss&&boss.isAlive()&&boss.redCore()&&boss!=event.getEntity())boss.heal((float)Math.ceil(event.getNewDamage()*.1));
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        var old=event.getOriginal().getPersistentData();if(old.contains("OmniraFounderCooldowns"))event.getEntity().getPersistentData().put("OmniraFounderCooldowns",old.getCompound("OmniraFounderCooldowns").copy());
    }
    private WeaverEvents(){}
}
