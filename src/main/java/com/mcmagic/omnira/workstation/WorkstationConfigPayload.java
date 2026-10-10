package com.mcmagic.omnira.workstation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WorkstationConfigPayload(int menu,String value,boolean rename) implements CustomPacketPayload {
    public static final Type<WorkstationConfigPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:workstation_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WorkstationConfigPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeVarInt(p.menu);b.writeUtf(p.value,256);b.writeBoolean(p.rename);},b->new WorkstationConfigPayload(b.readVarInt(),b.readUtf(256),b.readBoolean()));
    @Override public Type<WorkstationConfigPayload> type(){return TYPE;}
    @net.neoforged.fml.common.EventBusSubscriber(modid="omnira") public static final class Registration {
        @net.neoforged.bus.api.SubscribeEvent public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){
            e.registrar("1").playToServer(TYPE,CODEC,(p,c)->c.enqueueWork(()->{
                if(c.player().containerMenu instanceof CrystalWorkstationMenu m&&m.containerId==p.menu)m.configure(c.player(),p.value,p.rename);
            }));
        }
    }
}
