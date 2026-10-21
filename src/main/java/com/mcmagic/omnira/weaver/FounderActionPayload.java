package com.mcmagic.omnira.weaver;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record FounderActionPayload() implements CustomPacketPayload {
    public static final Type<FounderActionPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:founder_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,FounderActionPayload> CODEC=StreamCodec.unit(new FounderActionPayload());
    @Override public Type<FounderActionPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)FounderApprovalItem.cast(player,FounderApprovalItem.Action.BEND);
        }));}
    }
}
