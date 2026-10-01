package com.mcmagic.omnira.travel;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Keeps destinations discoverable while their chunks are unloaded. */
public final class WaymarkDirectory extends SavedData {
    public record Entry(UUID id,ResourceLocation dimension,BlockPos pos,String name) {}
    private final Map<UUID,Entry> entries=new HashMap<>();
    private final Map<UUID,Set<UUID>> discovered=new HashMap<>();
    public static WaymarkDirectory get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WaymarkDirectory::new,WaymarkDirectory::load,null),"omnira_waymarks");
    }
    public Entry get(UUID id) {return entries.get(id);}
    public boolean knows(UUID player,UUID id) {return discovered.getOrDefault(player,Set.of()).contains(id);}
    public List<Entry> list(UUID player) {return list().stream().filter(e->knows(player,e.id())).toList();}
    public void discover(UUID player,UUID id) {
        if(entries.containsKey(id) && discovered.computeIfAbsent(player,key->new HashSet<>()).add(id)) setDirty();
    }
    public void forget(UUID player,UUID id) {
        var known=discovered.get(player);
        if(known!=null && known.remove(id)) setDirty();
    }
    public String nextLostName(UUID player) {
        var names=new HashSet<String>();
        for(var entry:list(player)) names.add(entry.name());
        for(int i=1;;i++) if(!names.contains("失落点位"+i)) return "失落点位"+i;
    }
    public List<Entry> list() {return entries.values().stream().sorted(Comparator.comparing(Entry::name).thenComparing(e->e.id().toString())).toList();}
    public void put(Entry entry) {if(!entry.equals(entries.put(entry.id(),entry))) setDirty();}
    public void remove(UUID id) {
        if(entries.remove(id)!=null) setDirty();
        for(var known:discovered.values()) if(known.remove(id)) setDirty();
    }
    public static WaymarkDirectory load(CompoundTag tag,HolderLookup.Provider registries) {
        var result=new WaymarkDirectory();
        for(var value:tag.getList("Waymarks",Tag.TAG_COMPOUND)) {
            var n=(CompoundTag)value;var dim=ResourceLocation.tryParse(n.getString("Dimension"));
            if(dim!=null && n.hasUUID("Id")) {
                var entry=new Entry(n.getUUID("Id"),dim,BlockPos.of(n.getLong("Position")),n.getString("Name"));
                result.entries.put(entry.id(),entry);
            }
        }
        for(var value:tag.getList("Discoveries",Tag.TAG_COMPOUND)) {
            var n=(CompoundTag)value;
            if(!n.hasUUID("Player")) continue;
            var known=new HashSet<UUID>();
            for(var target:n.getList("Waymarks",Tag.TAG_COMPOUND)) {
                var mark=(CompoundTag)target;
                if(mark.hasUUID("Id") && result.entries.containsKey(mark.getUUID("Id"))) known.add(mark.getUUID("Id"));
            }
            result.discovered.put(n.getUUID("Player"),known);
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        var list=new ListTag();
        for(var entry:entries.values()) {
            var n=new CompoundTag();n.putUUID("Id",entry.id());n.putString("Dimension",entry.dimension().toString());
            n.putLong("Position",entry.pos().asLong());n.putString("Name",entry.name());list.add(n);
        }
        tag.put("Waymarks",list);
        var players=new ListTag();
        for(var player:discovered.entrySet()) {
            var n=new CompoundTag();n.putUUID("Player",player.getKey());var marks=new ListTag();
            for(var id:player.getValue()) {var mark=new CompoundTag();mark.putUUID("Id",id);marks.add(mark);}
            n.put("Waymarks",marks);players.add(n);
        }
        tag.put("Discoveries",players);return tag;
    }
}
