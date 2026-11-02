package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.entity.ShadowMist;
import com.mcmagic.omnira.spell.SpellPayload;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import java.util.*;

/** Optional integration: effects are resolved by ID, with no Cataclysm class dependency. */
@EventBusSubscriber(modid="omnira")
public final class AbyssTipEffects {
    private record Hits(Set<UUID> targets,long expires){}
    private static final Map<Level,Map<UUID,Hits>> MISTS=new WeakHashMap<>();
    public static UUID castId(SpellPayload payload){
        for(var key:payload.keywords())if(key.startsWith("omnira:abyss_cast/")){
            try{return UUID.fromString(key.substring("omnira:abyss_cast/".length()));}catch(IllegalArgumentException ignored){}
        }
        return null;
    }
    public static Set<UUID> mistHits(Level level,UUID cast){
        var groups=MISTS.computeIfAbsent(level,k->new HashMap<>());
        var previous=groups.get(cast);
        var targets=previous==null?new HashSet<UUID>():previous.targets();
        groups.put(cast,new Hits(targets,level.getGameTime()+4200));return targets;
    }
    @SubscribeEvent public static void damage(LivingDamageEvent.Post event){
        var target=event.getEntity();
        if(target.level().isClientSide||!target.isAlive()||event.getNewDamage()<=0)return;
        var direct=event.getSource().getDirectEntity();
        if(direct instanceof SpellEntity spell){if(!spell.abyssTip())return;}
        else if(direct instanceof ShadowMist mist){
            if(mist.abyssCast()==null||!mistHits(target.level(),mist.abyssCast()).add(target.getUUID()))return;
        }else return;
        boolean curse=target.getRandom().nextBoolean();
        BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(curse?"cataclysm:abyssal_curse":"cataclysm:abyssal_fear"))
                .ifPresent(effect->target.addEffect(new MobEffectInstance(effect,600,curse?1:0),event.getSource().getEntity()));
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        for(var entry:MISTS.entrySet())if(entry.getKey().getGameTime()%200==0)
            entry.getValue().values().removeIf(h->h.expires()<entry.getKey().getGameTime());
    }
    @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event){MISTS.clear();}
    private AbyssTipEffects(){}
}
