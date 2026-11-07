package com.mcmagic.omnira.mana;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record SpacetimeBlinkPayload() implements CustomPacketPayload {
    public static final Type<SpacetimeBlinkPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:spacetime_blink"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SpacetimeBlinkPayload> CODEC=StreamCodec.unit(new SpacetimeBlinkPayload());
    @Override public Type<SpacetimeBlinkPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)SpacetimeBlink.cast(player);
        }));}
    }
}
