package com.mcmagic.omnira.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record SwordInterruptedPayload(int actions) implements CustomPacketPayload {
    public static final Type<SwordInterruptedPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","sword_interrupted"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SwordInterruptedPayload> CODEC=StreamCodec.of(
            (buffer,payload)->buffer.writeByte(payload.actions),buffer->new SwordInterruptedPayload(buffer.readUnsignedByte()));
    @Override public Type<SwordInterruptedPayload> type() {return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->context.enqueueWork(()->
                    com.mcmagic.omnira.client.SwordChargeInput.interrupted(payload.actions)));
        }
    }
}
