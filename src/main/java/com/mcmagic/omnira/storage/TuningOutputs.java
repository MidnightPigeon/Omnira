package com.mcmagic.omnira.storage;

import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Reservations survive unloaded nodes and are re-keyed when structures change. */
public final class TuningOutputs extends SavedData {
    private record Claim(BlockPos node,UUID identity,BlockPos peer,UUID peerIdentity){}
    private final LinkedHashMap<String,Claim> claims=new LinkedHashMap<>();
    private final Map<UUID,UUID> removedLinks=new HashMap<>();
    private long refreshedAt=Long.MIN_VALUE;
    public boolean consumeRemovedLink(UUID node,UUID peer){
        if(peer!=null&&peer.equals(removedLinks.get(node))){removedLinks.remove(node);setDirty();return true;}
        return false;
    }
    public void disconnectShared(ServerLevel level){
        refresh(level);
        // Persist both ends before touching loaded nodes, so an unloaded end cannot revive the link.
        for(var claim:new ArrayList<>(claims.values())){
            if(claim.peer()==null)continue;
            var source=StorageContainerIdentity.terminalKey(level,claim.node().below());
            var target=StorageContainerIdentity.terminalKey(level,claim.peer().below());
            if(source==null||!source.equals(target))continue;
            removedLinks.put(claim.identity(),claim.peerIdentity());removedLinks.put(claim.peerIdentity(),claim.identity());
            claims.values().removeIf(value->value.identity().equals(claim.identity()));setDirty();
            for(var pos:List.of(claim.node(),claim.peer()))if(level.hasChunkAt(pos)
                    &&level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity node)node.refresh();
        }
    }
    public static TuningOutputs get(ServerLevel level){
        return level.getDataStorage().computeIfAbsent(new Factory<>(TuningOutputs::new,TuningOutputs::load,null),"omnira_tuning_outputs");
    }
    private void refresh(ServerLevel level){
        refreshedAt=level.getGameTime();
        var updated=new LinkedHashMap<String,Claim>();
        for(var entry:claims.entrySet()){
            var claim=entry.getValue();String key=entry.getKey();
            if(level.hasChunkAt(claim.node())){
                if(!(level.getBlockEntity(claim.node()) instanceof ResonanceCoreBlockEntity node)
                        ||!claim.identity().equals(node.identity)||!node.output||node.peer==null||node.kind()==0)continue;
                key=StorageContainerIdentity.key(node);
                claim=new Claim(node.getBlockPos(),node.identity,node.peer,node.peerIdentity);
            }
            if(key!=null)updated.putIfAbsent(key,claim);
        }
        if(!claims.equals(updated)){claims.clear();claims.putAll(updated);setDirty();}
    }
    public boolean available(ResonanceCoreBlockEntity node){
        return available(node,true);
    }
    private boolean available(ResonanceCoreBlockEntity node,boolean refreshNow){
        var level=(ServerLevel)node.getLevel();
        if(refreshNow||refreshedAt!=level.getGameTime())refresh(level);
        String key=StorageContainerIdentity.key(node);if(key==null)return false;
        for(var existing:claims.values())if(!existing.identity().equals(node.identity)&&level.hasChunkAt(existing.node())
                &&level.getBlockEntity(existing.node()) instanceof ResonanceCoreBlockEntity other&&StorageContainerIdentity.same(node,other))return false;
        var claim=claims.get(key);return claim==null||claim.identity().equals(node.identity);
    }
    public boolean claim(ResonanceCoreBlockEntity node){
        return claim(node,true);
    }
    /** Tick renewals share the ledger rebuild; interactive claims always refresh immediately. */
    public boolean renew(ResonanceCoreBlockEntity node){return claim(node,false);}
    private boolean claim(ResonanceCoreBlockEntity node,boolean refreshNow){
        if(!available(node,refreshNow))return false;
        var key=StorageContainerIdentity.key(node);var claim=new Claim(node.getBlockPos(),node.identity,node.peer,node.peerIdentity);
        if(!claim.equals(claims.put(key,claim)))setDirty();return true;
    }
    public void release(ResonanceCoreBlockEntity node){
        if(removedLinks.remove(node.identity)!=null)setDirty();
        if(claims.values().removeIf(claim->claim.identity().equals(node.identity)))setDirty();
    }
    public static TuningOutputs load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new TuningOutputs();
        for(var value:tag.getList("Claims",Tag.TAG_COMPOUND)){
            var entry=(CompoundTag)value;
            if(entry.hasUUID("Identity"))data.claims.put(entry.getString("Container"),new Claim(BlockPos.of(entry.getLong("Node")),entry.getUUID("Identity"),
                    entry.hasUUID("PeerIdentity")?BlockPos.of(entry.getLong("Peer")):null,entry.hasUUID("PeerIdentity")?entry.getUUID("PeerIdentity"):null));
        }
        for(var value:tag.getList("RemovedLinks",Tag.TAG_COMPOUND)){
            var entry=(CompoundTag)value;if(entry.hasUUID("Node")&&entry.hasUUID("Peer"))data.removedLinks.put(entry.getUUID("Node"),entry.getUUID("Peer"));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var entries=new ListTag();
        claims.forEach((key,claim)->{var entry=new CompoundTag();entry.putString("Container",key);entry.putLong("Node",claim.node().asLong());entry.putUUID("Identity",claim.identity());
            if(claim.peer()!=null&&claim.peerIdentity()!=null){entry.putLong("Peer",claim.peer().asLong());entry.putUUID("PeerIdentity",claim.peerIdentity());}entries.add(entry);});
        tag.put("Claims",entries);
        var removed=new ListTag();removedLinks.forEach((node,peer)->{var entry=new CompoundTag();entry.putUUID("Node",node);entry.putUUID("Peer",peer);removed.add(entry);});
        tag.put("RemovedLinks",removed);return tag;
    }
}
