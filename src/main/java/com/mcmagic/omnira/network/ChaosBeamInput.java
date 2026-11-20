package com.mcmagic.omnira.network;

import com.mcmagic.omnira.spell.ChaosBeam;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record ChaosBeamInput(int session) implements CustomPacketPayload {
    public static final Type<ChaosBeamInput> TYPE=new Type<>(ResourceLocation.parse("omnira:chaos_beam_input"));
    private static final StreamCodec<RegistryFriendlyByteBuf,ChaosBeamInput> CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,ChaosBeamInput::session,ChaosBeamInput::new);
    @Override public Type<ChaosBeamInput> type(){return TYPE;}

    @EventBusSubscriber(modid="omnira")
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){
            e.registrar("1").playBidirectional(TYPE,CODEC,(packet,ctx)->ctx.enqueueWork(()->{
                if(ctx.flow().isClientbound())com.mcmagic.omnira.client.ChaosBeamHold.begin(packet);
                else if(ctx.player() instanceof ServerPlayer p)ChaosBeam.release(p,packet.session());
            }));
        }
    }
}
