package com.mcmagic.omnira.network;

import com.mcmagic.omnira.client.ChaosBeamVisuals;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

/** Range zero closes the beam. Each paid pulse updates the existing caster's beam. */
public record ChaosBeamState(UUID caster, InteractionHand hand, int range, long pulseTick) implements CustomPacketPayload {
    public static final Type<ChaosBeamState> TYPE=new Type<>(ResourceLocation.parse("omnira:chaos_beam_state"));
    private static final StreamCodec<RegistryFriendlyByteBuf,ChaosBeamState> CODEC=StreamCodec.of((buf,v)->{
        buf.writeUUID(v.caster);buf.writeEnum(v.hand);buf.writeVarInt(v.range);buf.writeLong(v.pulseTick);
    },buf->new ChaosBeamState(buf.readUUID(),buf.readEnum(InteractionHand.class),buf.readVarInt(),buf.readLong()));
    @Override public Type<ChaosBeamState> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){
            e.registrar("1").playToClient(TYPE,CODEC,(v,c)->c.enqueueWork(()->ChaosBeamVisuals.receive(v)));
        }
    }
}
