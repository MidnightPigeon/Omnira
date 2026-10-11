package com.mcmagic.omnira.space.anchor;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** One batched decoration message per nearby player, not three particle packets per target. */
public record StabilityVisualPayload(List<Orbit> orbits) implements CustomPacketPayload {
    public record Orbit(Vec3 center,float radius){}
    public static java.util.function.Consumer<List<Orbit>> receive=list->{};
    public static final Type<StabilityVisualPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:stability_visuals"));
    public static final StreamCodec<RegistryFriendlyByteBuf,StabilityVisualPayload> CODEC=StreamCodec.of(
        (b,p)->{b.writeVarInt(p.orbits.size());for(var o:p.orbits){b.writeDouble(o.center.x);b.writeDouble(o.center.y);b.writeDouble(o.center.z);b.writeFloat(o.radius);}},
        b->{int n=b.readVarInt();if(n<0||n>128)throw new IllegalArgumentException("Orbit limit");var list=new ArrayList<Orbit>(n);for(int i=0;i<n;i++)list.add(new Orbit(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat()));return new StabilityVisualPayload(List.copyOf(list));});
    public Type<StabilityVisualPayload> type(){return TYPE;}
    private static final Map<ServerLevel,LinkedHashMap<Vec3,Orbit>> QUEUED=new WeakHashMap<>();
    public static void queue(ServerLevel level,Vec3 center,double radius){
        var queue=QUEUED.computeIfAbsent(level,k->new LinkedHashMap<>());
        if(queue.size()<4096)queue.put(center,new Orbit(center,(float)radius));
    }
    @EventBusSubscriber(modid="omnira",bus=EventBusSubscriber.Bus.MOD) public static final class Registration {
        @SubscribeEvent public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->receive.accept(p.orbits)));
        }
    }
    @EventBusSubscriber(modid="omnira") public static final class Events {
        @SubscribeEvent public static void flush(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event){
            if(!(event.getLevel() instanceof ServerLevel level))return;
            var queued=QUEUED.remove(level);if(queued==null)return;
            for(var player:level.players()){
                var nearby=queued.values().stream().filter(o->o.center.distanceToSqr(player.position())<=32*32).limit(128).toList();
                if(!nearby.isEmpty())net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new StabilityVisualPayload(nearby));
            }
        }
    }
}
