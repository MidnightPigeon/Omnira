package com.mcmagic.omnira.reversal;

import com.mcmagic.omnira.fate.ManuscriptReward;
import com.mcmagic.omnira.item.FateCurioItem.Kind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="omnira")
public final class AncientTalents {
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||!p.isAlive())return;
        var talent=ManuscriptReward.equipped(p,"talent");
        var data=com.mcmagic.omnira.fate.FateRuntime.data(p);
        var regeneration=p.getEffect(MobEffects.REGENERATION);
        if(talent==Kind.LAND_KING&&p.onGround()){
            if(regeneration==null){p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,-1,1,true,false,true));data.putBoolean("OmniraLandRegeneration",true);}
        }else if(data.getBoolean("OmniraLandRegeneration")){
            if(regeneration!=null&&regeneration.isInfiniteDuration()&&regeneration.isAmbient()&&regeneration.getAmplifier()==1)p.removeEffect(MobEffects.REGENERATION);
            data.remove("OmniraLandRegeneration");
        }
        if(talent==Kind.LAND_KING){effect(p,MobEffects.DAMAGE_RESISTANCE,0);effect(p,MobEffects.DAMAGE_BOOST,1);}
        if(talent==Kind.SEA_KING)effect(p,MobEffects.WATER_BREATHING,0);
        if(talent==Kind.SKY_KING)effect(p,MobEffects.NIGHT_VISION,0);
        if(ManuscriptReward.equipped(p,"curse")==Kind.TIME_DISTORTION){
            int ticks=data.getInt("OmniraDistortionTicks")+1;
            if(ticks>=1200){ticks=0;if(p.getRandom().nextFloat()<.1F)p.addEffect(new MobEffectInstance(com.mcmagic.omnira.registry.ModEffects.TIME_PASSAGE,200,0));}
            data.putInt("OmniraDistortionTicks",ticks);
        }else data.remove("OmniraDistortionTicks");
    }
    private static void effect(ServerPlayer p,net.minecraft.core.Holder<MobEffect> effect,int amplifier){
        // Apply immediately on acquisition; keep night vision above its fade threshold.
        if(!p.hasEffect(effect)||p.tickCount%200==0)
            p.addEffect(new MobEffectInstance(effect,600,amplifier,true,false,true));
    }
    @SubscribeEvent public static void fall(net.neoforged.neoforge.event.entity.living.LivingFallEvent event){if(event.getEntity() instanceof ServerPlayer p&&ManuscriptReward.equipped(p,"talent")==Kind.SKY_KING)event.setCanceled(true);}
    private AncientTalents(){}
}
