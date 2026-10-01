package com.mcmagic.omnira.vehicle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record CruiseOrbPayload(int action,int keys) implements CustomPacketPayload {
    public static final Type<CruiseOrbPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","cruise_orb"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CruiseOrbPayload> CODEC=StreamCodec.of((b,p)->{b.writeByte(p.action);b.writeByte(p.keys);},b->new CruiseOrbPayload(b.readUnsignedByte(),b.readUnsignedByte()));
    @Override public Type<CruiseOrbPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
                if(context.player() instanceof ServerPlayer p && p.getVehicle() instanceof CruiseOrbEntity orb){
                    if(payload.action==0)orb.input(p,payload.keys);
                    else if(payload.action>=1 && payload.action<=3)orb.action(p,payload.action);
                }else if(context.player() instanceof ServerPlayer p && p.getVehicle() instanceof CrystalBroomEntity broom && payload.action==0)broom.input(p,payload.keys);
            }));
        }
    }
}
