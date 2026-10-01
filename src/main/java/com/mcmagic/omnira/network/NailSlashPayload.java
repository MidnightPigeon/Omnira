package com.mcmagic.omnira.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record NailSlashPayload(Vec3 origin,Vec3 forward,float reach) implements CustomPacketPayload {
    public static final Type<NailSlashPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","nail_slash"));
    public static final StreamCodec<RegistryFriendlyByteBuf,NailSlashPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeDouble(p.origin.x);b.writeDouble(p.origin.y);b.writeDouble(p.origin.z);
                b.writeDouble(p.forward.x);b.writeDouble(p.forward.y);b.writeDouble(p.forward.z);b.writeFloat(p.reach);},
            b->new NailSlashPayload(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat()));
    @Override public Type<NailSlashPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->context.enqueueWork(()->
                    com.mcmagic.omnira.client.NailSlashVisuals.receive(payload)));
        }
    }
}
