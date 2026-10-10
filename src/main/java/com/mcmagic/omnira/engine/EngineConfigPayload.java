package com.mcmagic.omnira.engine;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EngineConfigPayload(int menu,int side,int capacity,int rpm,boolean reverse) implements CustomPacketPayload {
    public static final Type<EngineConfigPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:spacetime_engine_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf,EngineConfigPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeVarInt(p.menu);b.writeVarInt(p.side);b.writeVarInt(p.capacity);b.writeVarInt(p.rpm);b.writeBoolean(p.reverse);},
            b->new EngineConfigPayload(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean()));
    @Override public Type<EngineConfigPayload> type(){return TYPE;}
    public static boolean apply(net.minecraft.world.entity.player.Player player,EngineConfigPayload p){
        return player.containerMenu instanceof SpacetimeEngineMenu m&&m.containerId==p.menu&&m.stillValid(player)
                &&player.level().mayInteract(player,m.engine.getBlockPos())
                &&(SpacetimeEngineController.hasKinetics()?m.engine.configure(p.side,p.capacity,p.rpm,p.reverse):m.engine.configureRotation(p.side,p.reverse));
    }
    @net.neoforged.fml.common.EventBusSubscriber(modid="omnira") public static final class Registration {
        @net.neoforged.bus.api.SubscribeEvent public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){
            e.registrar("1").playToServer(TYPE,CODEC,(p,c)->c.enqueueWork(()->apply(c.player(),p)));
        }
    }
}
