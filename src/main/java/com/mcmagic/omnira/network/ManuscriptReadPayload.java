package com.mcmagic.omnira.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record ManuscriptReadPayload() implements CustomPacketPayload {
    public static final Type<ManuscriptReadPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","manuscript_read"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ManuscriptReadPayload> CODEC=StreamCodec.unit(new ManuscriptReadPayload());
    @Override public Type<ManuscriptReadPayload> type() {return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
                if(context.player() instanceof ServerPlayer player) com.mcmagic.omnira.fate.ManuscriptReward.finish(player);
            }));
        }
    }
}
