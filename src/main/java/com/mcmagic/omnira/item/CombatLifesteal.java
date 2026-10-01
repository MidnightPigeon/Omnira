package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Owned ranged damage and melee share the same additive accessory leech rate. */
@EventBusSubscriber(modid="omnira")
public final class CombatLifesteal {
    @SubscribeEvent public static void afterDamage(LivingDamageEvent.Post event){
        if(event.getNewDamage()<=0||!(event.getSource().getEntity() instanceof Player player)
                ||event.getEntity()==player||!player.isAlive())return;
        double rate=player.getAttributeValue(ModAttributes.LIFESTEAL);
        if(event.getSource().is(DamageTypes.PLAYER_ATTACK)
                &&player.getMainHandItem().getItem() instanceof InfusedGrimoireItem book
                &&book.kind()==InfusedGrimoireItem.Kind.CORRUPTED)rate+=.5;
        if(rate>0)player.heal((float)(event.getNewDamage()*rate));
    }
    private CombatLifesteal(){}
}
