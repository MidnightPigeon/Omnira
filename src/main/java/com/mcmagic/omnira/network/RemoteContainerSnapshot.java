package com.mcmagic.omnira.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record RemoteContainerSnapshot(int menuId,CompoundTag snapshot) implements CustomPacketPayload {
    public static final Type<RemoteContainerSnapshot> TYPE=new Type<>(ResourceLocation.parse("omnira:remote_container_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RemoteContainerSnapshot> CODEC=StreamCodec.of(
            (b,p)->{b.writeVarInt(p.menuId);b.writeNbt(p.snapshot);},b->new RemoteContainerSnapshot(b.readVarInt(),b.readNbt()));
    @Override public Type<RemoteContainerSnapshot> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToClient(TYPE,CODEC,
                (p,c)->c.enqueueWork(()->com.mcmagic.omnira.client.RemoteContainerView.receive(p)));}
    }
}
