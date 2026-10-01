package com.mcmagic.omnira.network;

import com.mcmagic.omnira.Omnira;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.List;
import java.util.UUID;

public record GhostSensePayload(List<UUID> entities) implements CustomPacketPayload {
    public GhostSensePayload {entities=List.copyOf(entities);}
    public static final Type<GhostSensePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"ghost_sense"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GhostSensePayload> CODEC=StreamCodec.of(
            (buffer,value)->buffer.writeCollection(value.entities,(b,id)->b.writeUUID(id)),
            buffer->new GhostSensePayload(buffer.readList(b->b.readUUID())));
    @Override public Type<GhostSensePayload> type() {return TYPE;}
    @EventBusSubscriber(modid=Omnira.MOD_ID)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->context.enqueueWork(()->
                    com.mcmagic.omnira.client.GhostSense.receive(payload.entities)));
        }
    }
}
