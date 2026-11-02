package com.mcmagic.omnira.network;

import com.mcmagic.omnira.workstation.CondensationChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The client reports only held input and its hit; processing and mana are server-owned. */
public record CondensationChannelPayload(boolean held,BlockPos pos,boolean cancel) implements CustomPacketPayload {
    public static final Type<CondensationChannelPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:condensation_channel"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CondensationChannelPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeBoolean(p.held);b.writeBlockPos(p.pos);b.writeBoolean(p.cancel);},
            b->new CondensationChannelPayload(b.readBoolean(),b.readBlockPos(),b.readBoolean()));
    @Override public Type<CondensationChannelPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
                if(context.player() instanceof ServerPlayer player){if(payload.cancel)CondensationChannel.stop(player);else CondensationChannel.input(player,payload.held,payload.pos);}
            }));
        }
    }
}
