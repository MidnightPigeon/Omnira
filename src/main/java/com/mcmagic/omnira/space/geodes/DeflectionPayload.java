package com.mcmagic.omnira.space.geodes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

public record DeflectionPayload(float degrees) implements CustomPacketPayload {
    public static final Type<DeflectionPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:deflection_turn"));
    public static final StreamCodec<RegistryFriendlyByteBuf,DeflectionPayload> CODEC=StreamCodec.of((b,p)->b.writeFloat(p.degrees),b->new DeflectionPayload(b.readFloat()));
    @Override public Type<DeflectionPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->{
                var data=c.player().getPersistentData();
                if(p.degrees==0){data.remove("DeflectionClientPending");return;}
                data.putFloat("DeflectionClientPending",data.getFloat("DeflectionClientPending")+p.degrees);
            }));
        }
    }
}
