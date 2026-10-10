package com.mcmagic.omnira.network;

import com.mcmagic.omnira.alchemy.catalysis.CatalysisRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public record CatalysisCataloguePayload(List<CatalysisRecipe> recipes,boolean first,boolean last) implements CustomPacketPayload {
    public CatalysisCataloguePayload {recipes=List.copyOf(recipes);if(recipes.size()>32)throw new IllegalArgumentException("Catalogue chunk too large");}
    public static final Type<CatalysisCataloguePayload> TYPE=new Type<>(ResourceLocation.parse("omnira:catalysis_catalogue"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CatalysisCataloguePayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeBoolean(p.first);b.writeBoolean(p.last);b.writeVarInt(p.recipes.size());for(var r:p.recipes)CatalysisRecipe.STREAM.encode(b,r);},
            b->{boolean first=b.readBoolean(),last=b.readBoolean();int n=b.readVarInt();if(n<0||n>32)throw new IllegalArgumentException("Invalid catalogue chunk");
                var recipes=new ArrayList<CatalysisRecipe>();for(int i=0;i<n;i++)recipes.add(CatalysisRecipe.STREAM.decode(b));return new CatalysisCataloguePayload(recipes,first,last);});
    public Type<CatalysisCataloguePayload> type(){return TYPE;}
    @net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
    public static final class Registration {
        @net.neoforged.bus.api.SubscribeEvent
        public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->context.enqueueWork(()->
                    com.mcmagic.omnira.client.CatalysisCatalogueView.receive(payload)));
        }
    }
}
