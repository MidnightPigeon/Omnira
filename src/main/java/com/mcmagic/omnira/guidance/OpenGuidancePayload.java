package com.mcmagic.omnira.guidance;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record OpenGuidancePayload(String destination) implements CustomPacketPayload {
    public static final Type<OpenGuidancePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","open_guidance"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OpenGuidancePayload> CODEC=StreamCodec.of(
            (b,p)->b.writeUtf(p.destination,96),b->new OpenGuidancePayload(b.readUtf(96)));
    @Override public Type<OpenGuidancePayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->com.mcmagic.omnira.client.GuideBookClient.open(p.destination)));
        }
    }
}
