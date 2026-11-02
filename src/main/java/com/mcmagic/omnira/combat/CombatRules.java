package com.mcmagic.omnira.combat;

import com.mcmagic.omnira.registry.ModAttributes;
import com.mcmagic.omnira.item.CrystalArmorEffects;
import com.mcmagic.omnira.space.sea.DeflectionFieldEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class CombatRules {
    public static double incoming(LivingEntity target){
        if(target instanceof Player player)com.mcmagic.omnira.item.staff.BossStaffParts.refresh(player);
        var attribute=target.getAttribute(ModAttributes.DAMAGE_TAKEN);
        double factor=attribute==null?1:attribute.getValue();
        if(target instanceof Player player)factor-=CrystalArmorEffects.equipped(player).reduction;
        if(DeflectionFieldEvents.raised(target))factor-=.25;
        return Math.max(0,factor);
    }
    public static double outgoing(ServerPlayer attacker,LivingEntity target){
        com.mcmagic.omnira.item.staff.BossStaffParts.refresh(attacker);
        double factor=attacker.getAttributeValue(ModAttributes.DAMAGE_DEALT);
        if(attacker!=target){
            if(com.mcmagic.omnira.throne.GoldenThrone.empowered(attacker))factor+=1;
            if(com.mcmagic.omnira.throne.GoldenToilet.empowered(attacker))factor+=.2;
        }
        return Math.max(0,factor);
    }
    public static void apply(LivingDamageEvent.Pre event,boolean amplify){
        if(OmniraDamage.isTrue(event.getSource())){event.setNewDamage(event.getOriginalDamage());return;}
        double attack=amplify&&event.getSource().getEntity() instanceof ServerPlayer player?outgoing(player,event.getEntity()):1;
        event.setNewDamage((float)Math.max(0,event.getNewDamage()*attack*incoming(event.getEntity())));
        com.mcmagic.omnira.mana.AffinityEffects.defend(event);
    }
    private CombatRules(){}
}
