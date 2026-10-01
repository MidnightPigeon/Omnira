package com.mcmagic.omnira.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record SwordChargePayload(int action) implements CustomPacketPayload {
    public static final Type<SwordChargePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","sword_charge"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SwordChargePayload> CODEC=StreamCodec.of((b,p)->b.writeByte(p.action),b->new SwordChargePayload(b.readUnsignedByte()));
    @Override public Type<SwordChargePayload> type() {return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
                if(context.player() instanceof net.minecraft.server.level.ServerPlayer p && payload.action<=2)com.mcmagic.omnira.item.SwordActions.input(p,payload.action);
            }));
        }
    }
}
