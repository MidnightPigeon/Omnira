package com.mcmagic.omnira.progression;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

/** Keep recorded objectives separate from the prerequisite needed to complete them. */
@EventBusSubscriber(modid="omnira")
public final class AdvancementDependencies {
    public static final String PREVIOUS="previous_completed";
    private static final Set<UUID> SYNCING=new HashSet<>();

    public static void sync(ServerPlayer player){
        if(!SYNCING.add(player.getUUID()))return;
        try{
            var manager=player.server.getAdvancements();
            var gated=manager.getAllAdvancements().stream().filter(a->a.id().getNamespace().equals("omnira")
                    &&a.value().parent().isPresent()&&a.value().criteria().containsKey(PREVIOUS)).toList();
            // Resource iteration order is unspecified; settle both grants and revoked parent chains.
            boolean changed;
            do{
                changed=false;
                for(var child:gated){
                    var parent=manager.get(child.value().parent().orElseThrow());
                    boolean eligible=parent!=null&&player.getAdvancements().getOrStartProgress(parent).isDone();
                    var progress=player.getAdvancements().getOrStartProgress(child).getCriterion(PREVIOUS);
                    if(progress!=null&&progress.isDone()!=eligible)
                        changed|=eligible?player.getAdvancements().award(child,PREVIOUS):player.getAdvancements().revoke(child,PREVIOUS);
                }
            }while(changed);
        }finally{SYNCING.remove(player.getUUID());}
    }

    @SubscribeEvent public static void earned(AdvancementEvent.AdvancementEarnEvent event){
        if(event.getEntity() instanceof ServerPlayer player&&event.getAdvancement().id().getNamespace().equals("omnira"))sync(player);
    }
    @SubscribeEvent public static void revoked(AdvancementEvent.AdvancementProgressEvent event){
        if(event.getProgressType()==AdvancementEvent.AdvancementProgressEvent.ProgressType.REVOKE
                &&event.getEntity() instanceof ServerPlayer player&&event.getAdvancement().id().getNamespace().equals("omnira"))sync(player);
    }
    @SubscribeEvent public static void reload(OnDatapackSyncEvent event){
        event.getRelevantPlayers().forEach(AdvancementDependencies::sync);
    }
}
