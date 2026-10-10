package com.mcmagic.omnira.client;

import com.mcmagic.omnira.alchemy.catalysis.CatalysisRecipe;
import com.mcmagic.omnira.network.CatalysisCataloguePayload;
import java.util.*;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class CatalysisCatalogueView {
    private static final List<CatalysisRecipe> incoming=new ArrayList<>();
    public static List<CatalysisRecipe> recipes=List.of();
    public static Runnable changed=()->{};
    public static void receive(CatalysisCataloguePayload payload){
        if(payload.first())incoming.clear();
        incoming.addAll(payload.recipes());
        if(payload.last()){recipes=List.copyOf(incoming);incoming.clear();changed.run();}
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){
        incoming.clear();recipes=List.of();changed=()->{};
    }
    private CatalysisCatalogueView(){}
}
