package com.mcmagic.omnira.progression;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** Offline successes are sets, not an unbounded operation log. */
final class BranchPending extends SavedData {
    private final Map<UUID,Set<String>> objectives=new HashMap<>();
    static BranchPending get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(BranchPending::new,BranchPending::load,null),"omnira_branch_progress");
    }
    void add(UUID player,String id,String criterion){
        if(objectives.computeIfAbsent(player,k->new HashSet<>()).add(id+"/"+criterion))setDirty();
    }
    void deliver(ServerPlayer player){
        var pending=objectives.get(player.getUUID());if(pending==null)return;
        pending.removeIf(value->{int split=value.lastIndexOf('/');return BranchProgress.award(player,value.substring(0,split),value.substring(split+1));});
        if(pending.isEmpty())objectives.remove(player.getUUID());setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        objectives.forEach((id,values)->{var list=new ListTag();values.stream().sorted().forEach(v->list.add(StringTag.valueOf(v)));tag.put(id.toString(),list);});return tag;
    }
    static BranchPending load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new BranchPending();for(var key:tag.getAllKeys()){
            var values=new HashSet<String>();for(var value:tag.getList(key,Tag.TAG_STRING))values.add(value.getAsString());
            data.objectives.put(UUID.fromString(key),values);
        }return data;
    }
}
