package com.mcmagic.omnira.network;

import com.mcmagic.omnira.pulse.PulseChanneling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The client reports only held input and its hit; charge time and MPU are server-owned. */
public record PulseChannelPayload(boolean held,BlockPos pos,Direction face) implements CustomPacketPayload {
    public static final Type<PulseChannelPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:pulse_channel"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PulseChannelPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeBoolean(p.held);b.writeBlockPos(p.pos);b.writeEnum(p.face);},
            b->new PulseChannelPayload(b.readBoolean(),b.readBlockPos(),b.readEnum(Direction.class)));
    @Override public Type<PulseChannelPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
                if(context.player() instanceof ServerPlayer player)PulseChanneling.input(player,payload.held,payload.pos,payload.face);
            }));
        }
    }
}
