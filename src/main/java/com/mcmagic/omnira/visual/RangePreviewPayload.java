package com.mcmagic.omnira.visual;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

public record RangePreviewPayload(BlockPos source,int radius,boolean chunk) implements CustomPacketPayload {
    public static java.util.function.Consumer<RangePreviewPayload> receive=p->{};
    public static final Type<RangePreviewPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:machine_range_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RangePreviewPayload> CODEC=StreamCodec.of(
        (b,p)->{b.writeBlockPos(p.source);b.writeVarInt(p.radius);b.writeBoolean(p.chunk);},
        b->new RangePreviewPayload(b.readBlockPos(),b.readVarInt(),b.readBoolean()));
    public Type<RangePreviewPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira",bus=EventBusSubscriber.Bus.MOD) public static final class Registration {
        @SubscribeEvent public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->receive.accept(p)));
        }
    }
}
