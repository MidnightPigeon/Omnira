package com.mcmagic.omnira.item;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Owned ranged damage and melee share the same additive accessory leech rate. */
@EventBusSubscriber(modid="omnira")
public final class CombatLifesteal {
    @SubscribeEvent public static void afterDamage(LivingDamageEvent.Post event){
        if(event.getNewDamage()<=0||!(event.getSource().getEntity() instanceof Player player)
                ||event.getEntity()==player||!player.isAlive())return;
        double rate=com.mcmagic.omnira.combat.HeldCombatAttributes.lifeSteal(player,event.getSource());
        boolean founder=com.mcmagic.omnira.weaver.FounderApprovalItem.held(player);
        if(rate>0)player.heal((float)(founder?Math.ceil(event.getNewDamage()*rate):event.getNewDamage()*rate));
    }
    private CombatLifesteal(){}
}
