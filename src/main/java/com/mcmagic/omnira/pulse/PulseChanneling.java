package com.mcmagic.omnira.pulse;

import com.mcmagic.omnira.mana.ManaEvents;
import com.mcmagic.omnira.network.PulseChannelStatePayload;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** A held gesture lends natural recovery to the native network, never stored mana. */
@EventBusSubscriber(modid="omnira")
public final class PulseChanneling {
    public static final int CHARGE_TICKS=100,HEARTBEAT_TICKS=5,HEARTBEAT_TIMEOUT=15,SHORT_CLICK_TICKS=6;
    static final int FIRST_SOURCE_OUTPUT=1_000_000;
    private record TargetKey(ServerLevel level,BlockPos root){}
    static final class Session {
        final ServerPlayer player;final ServerLevel level;final BlockPos pos;final Direction face;
        final PulseChannelTargets.Target target;final Block block;final BlockEntity entity;final int started,output;
        int lastHeld;boolean active;
        Session(ServerPlayer player,BlockPos pos,Direction face,PulseChannelTargets.Target target,int output){
            this.player=player;level=player.serverLevel();this.pos=pos.immutable();this.face=face;this.target=target;
            block=level.getBlockState(pos).getBlock();entity=level.getBlockEntity(pos);
            started=lastHeld=level.getServer().getTickCount();this.output=output;
        }
        double supply(){return active?ManaEvents.regenerationPerSecond(player)*10:0;}
    }
    private static final class State {
        final Map<UUID,Session> sessions=new HashMap<>();
        final Map<TargetKey,List<Session>> sources=new HashMap<>();int nextOutput=FIRST_SOURCE_OUTPUT;
    }
    private static final Map<MinecraftServer,State> SERVERS=new WeakHashMap<>();
    private static boolean eligible(ServerPlayer player){
        return player.isAlive()&&!player.isRemoved()&&!player.isSpectator()&&player.getMainHandItem().isEmpty()&&player.getOffhandItem().isEmpty()
                &&!player.isUsingItem()&&player.containerMenu==player.inventoryMenu;
    }
    private static boolean aimsAt(ServerPlayer player,BlockPos pos,Direction face){
        var hit=player.pick(player.blockInteractionRange(),1,false);
        return hit.getType()==HitResult.Type.BLOCK&&hit instanceof BlockHitResult block&&block.getBlockPos().equals(pos)&&block.getDirection()==face;
    }
    public static void input(ServerPlayer player,boolean held,BlockPos pos,Direction face){
        if(!held){stop(player);return;}
        var level=player.serverLevel();var target=eligible(player)&&aimsAt(player,pos,face)?PulseChannelTargets.resolve(level,pos,face):null;
        if(target==null||!level.mayInteract(player,pos)){
            stop(player);PacketDistributor.sendToPlayer(player,new PulseChannelStatePayload(player.getUUID(),-1));return;
        }
        var state=SERVERS.computeIfAbsent(level.getServer(),server->new State());var old=state.sessions.get(player.getUUID());
        if(old!=null&&old.player==player&&old.level==level&&old.pos.equals(pos)&&old.face==face&&old.target.equals(target)
                &&old.block==level.getBlockState(pos).getBlock()&&old.entity==level.getBlockEntity(pos)){
            old.lastHeld=level.getServer().getTickCount();return;
        }
        stop(player);
        var session=new Session(player,pos,face,target,state.nextOutput++);state.sessions.put(player.getUUID(),session);sync(session);
    }
    private static boolean valid(Session session,int now){
        var player=session.player;
        return eligible(player)&&player.serverLevel()==session.level&&now-session.lastHeld<=HEARTBEAT_TIMEOUT&&session.level.mayInteract(player,session.pos)
                &&session.level.hasChunkAt(session.pos)&&session.block==session.level.getBlockState(session.pos).getBlock()
                &&session.entity==session.level.getBlockEntity(session.pos)&&aimsAt(player,session.pos,session.face)
                &&session.target.equals(PulseChannelTargets.resolve(session.level,session.pos,session.face));
    }
    public static void tick(MinecraftServer server){
        var state=SERVERS.get(server);if(state==null)return;int now=server.getTickCount();
        for(var session:List.copyOf(state.sessions.values())){
            if(!valid(session,now)){stop(session.player);continue;}
            if(!session.active&&now-session.started>=CHARGE_TICKS){
                session.active=true;state.sources.computeIfAbsent(new TargetKey(session.level,session.target.root()),key->new ArrayList<>()).add(session);
                PulseNetworks.changed(session.level,session.target.inlet());sync(session);
            }
        }
    }
    static List<Session> sources(ServerLevel level,BlockPos root){
        var state=SERVERS.get(level.getServer());return state==null?List.of():state.sources.getOrDefault(new TargetKey(level,root),List.of());
    }
    public static boolean active(ServerPlayer player){
        var state=SERVERS.get(player.serverLevel().getServer());var session=state==null?null:state.sessions.get(player.getUUID());
        return session!=null&&session.player==player&&session.level==player.serverLevel()&&session.active;
    }
    public static void stop(ServerPlayer player){
        var state=SERVERS.get(player.serverLevel().getServer());if(state==null)return;var session=state.sessions.remove(player.getUUID());if(session==null)return;
        boolean supplied=session.active;session.active=false;var key=new TargetKey(session.level,session.target.root());var sources=state.sources.get(key);
        if(sources!=null){sources.remove(session);if(sources.isEmpty())state.sources.remove(key);}
        if(supplied)PulseNetworks.changed(session.level,session.target.inlet());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,new PulseChannelStatePayload(player.getUUID(),-1));
    }
    private static void sync(Session session){
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(session.player,new PulseChannelStatePayload(session.player.getUUID(),elapsed(session)));
    }
    private static int elapsed(Session session){return Math.clamp(session.level.getServer().getTickCount()-session.started,0,CHARGE_TICKS);}
    @SubscribeEvent public static void track(PlayerEvent.StartTracking event){
        if(event.getEntity() instanceof ServerPlayer observer&&event.getTarget() instanceof ServerPlayer player){
            var state=SERVERS.get(player.serverLevel().getServer());var session=state==null?null:state.sessions.get(player.getUUID());
            if(session!=null)PacketDistributor.sendToPlayer(observer,new PulseChannelStatePayload(player.getUUID(),elapsed(session)));
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)stop(player);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer player)stop(player);}
    public static void clear(MinecraftServer server){SERVERS.remove(server);}
    private PulseChanneling(){}
}
