package com.mcmagic.omnira.music;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

public record ViolinPayload(UUID player,int track) implements CustomPacketPayload {
    public static final Type<ViolinPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","violin_performance"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ViolinPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeUUID(p.player);b.writeInt(p.track);},b->new ViolinPayload(b.readUUID(),b.readInt()));
    @Override public Type<ViolinPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->ViolinClient.receive(p)));
        }
    }
}
