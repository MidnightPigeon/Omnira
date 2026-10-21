package com.mcmagic.omnira.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record PulseChannelStatePayload(UUID player,int elapsed) implements CustomPacketPayload {
    public static final Type<PulseChannelStatePayload> TYPE=new Type<>(ResourceLocation.parse("omnira:pulse_channel_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PulseChannelStatePayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeUUID(p.player);b.writeInt(p.elapsed);},b->new PulseChannelStatePayload(b.readUUID(),b.readInt()));
    @Override public Type<PulseChannelStatePayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->context.enqueueWork(()->
                    com.mcmagic.omnira.client.PulseChannelVisuals.receive(payload.player,payload.elapsed)));
        }
    }
}
