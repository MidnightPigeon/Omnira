package com.mcmagic.omnira.fate;

import com.mcmagic.omnira.item.FateCurioItem.Kind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="omnira")
public final class TimeThiefFate {
    @SubscribeEvent public static void damage(LivingDamageEvent.Post event){
        if(event.getNewDamage()>0 && event.getSource().getEntity() instanceof ServerPlayer player && player!=event.getEntity()
                && ManuscriptReward.equipped(player,"talent")==Kind.TIME_THIEF){
            stealEffect(event.getEntity(),player,MobEffects.MOVEMENT_SLOWDOWN,MobEffects.MOVEMENT_SPEED);
            stealEffect(event.getEntity(),player,MobEffects.WEAKNESS,MobEffects.DAMAGE_BOOST);
            stealEffect(event.getEntity(),player,MobEffects.WITHER,MobEffects.REGENERATION);
        }
    }
    private static void stealEffect(net.minecraft.world.entity.LivingEntity target,ServerPlayer player,
                                    net.minecraft.core.Holder<MobEffect> debuff,net.minecraft.core.Holder<MobEffect> buff){
        if(target.addEffect(new MobEffectInstance(debuff,200,1),player))
            player.addEffect(new MobEffectInstance(buff,200,1),player);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer player && player.tickCount%20==0)wearHands(player);
    }
    public static void wearHands(ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||ManuscriptReward.equipped(player,"curse")!=Kind.AMON_GAZE)return;
        var main=player.getMainHandItem();var off=player.getOffhandItem();
        main.hurtAndBreak(1,player,EquipmentSlot.MAINHAND);
        if(off!=main)off.hurtAndBreak(1,player,EquipmentSlot.OFFHAND);
    }
    private TimeThiefFate(){}
}
