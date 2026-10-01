package com.mcmagic.omnira.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

public record BrushHoldPayload(int entity,boolean held) implements CustomPacketPayload {
    public static final Type<BrushHoldPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:brush_hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf,BrushHoldPayload> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.entity);b.writeBoolean(p.held);},b->new BrushHoldPayload(b.readVarInt(),b.readBoolean()));
    @Override public Type<BrushHoldPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira")
    public static final class Registration {
        @SubscribeEvent public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->com.mcmagic.omnira.client.ArchaeologyClient.hold(p.entity,p.held)));
        }
    }
}
