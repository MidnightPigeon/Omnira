package com.mcmagic.omnira.weaver;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record WeaverVisualPayload(int kind,Vec3 from,Vec3 to,float radius) implements CustomPacketPayload {
    public static final Type<WeaverVisualPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:weaver_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WeaverVisualPayload> CODEC=StreamCodec.of((b,p)->{
        b.writeByte(p.kind);b.writeDouble(p.from.x);b.writeDouble(p.from.y);b.writeDouble(p.from.z);b.writeDouble(p.to.x);b.writeDouble(p.to.y);b.writeDouble(p.to.z);b.writeFloat(p.radius);
    },b->new WeaverVisualPayload(b.readUnsignedByte(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat()));
    @Override public Type<WeaverVisualPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->context.enqueueWork(()->com.mcmagic.omnira.client.WeaverVisuals.receive(payload)));}
    }
}
