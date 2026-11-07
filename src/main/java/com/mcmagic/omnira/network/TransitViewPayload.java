package com.mcmagic.omnira.network;

import com.mcmagic.omnira.world.dimension.ModDimensions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid="omnira")
public record TransitViewPayload(Style style, int chargeTicks) implements CustomPacketPayload {
    public static final int PORTAL_TICKS = 100;
    public static final int SYNC_INTERVAL = 8;
    public enum Style { DREAM, INTERSTICE, TIME, SPACE }
    public static final Type<TransitViewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("omnira", "transit_view"));
    private static final StreamCodec<RegistryFriendlyByteBuf, TransitViewPayload> CODEC = StreamCodec.of(
            (buf, packet) -> { buf.writeEnum(packet.style); buf.writeVarInt(packet.chargeTicks); },
            buf -> new TransitViewPayload(buf.readEnum(Style.class), buf.readVarInt()));

    @Override public Type<TransitViewPayload> type() { return TYPE; }

    @SubscribeEvent public static void dimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getTo().equals(ModDimensions.DREAM_REALM)) send(player, Style.DREAM);
        // Interstice arrival is released by PortalPassageScreen only once terrain is ready.
    }

    public static void charging(net.minecraft.world.entity.Entity root, Style style, int ticks) {
        if (ticks != 1 && ticks % SYNC_INTERVAL != 0 && ticks != PORTAL_TICKS) return;
        var packet = new TransitViewPayload(style, ticks);
        root.getSelfAndPassengers().forEach(member -> {
            if (member instanceof ServerPlayer player) PacketDistributor.sendToPlayer(player, packet);
        });
    }

    public static void cancel(net.minecraft.world.entity.Entity root) {
        root.getSelfAndPassengers().forEach(member -> {
            if (member instanceof ServerPlayer player)
                PacketDistributor.sendToPlayer(player, new TransitViewPayload(Style.INTERSTICE, -1));
        });
    }

    public static void send(ServerPlayer player, Style style) {
        PacketDistributor.sendToPlayer(player, new TransitViewPayload(style, 0));
    }

    @EventBusSubscriber(modid="omnira", bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, CODEC, (packet, context) ->
                    context.enqueueWork(() -> com.mcmagic.omnira.client.hud.TransitView.start(packet.style, packet.chargeTicks)));
        }
    }
}
