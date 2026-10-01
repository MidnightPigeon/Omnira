package com.mcmagic.omnira.item.bottle;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Large captures remain server-side; inventory packets carry only a single-use UUID. */
public final class BottleStorage extends SavedData {
    private final Map<UUID,CompoundTag> captures=new HashMap<>();
    public static BottleStorage get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(BottleStorage::new,BottleStorage::load,null),"omnira_bottle_captures");
    }
    private static BottleStorage load(CompoundTag tag,HolderLookup.Provider registries) {
        var data=new BottleStorage();
        for(var entry:tag.getList("Captures",Tag.TAG_COMPOUND)) {
            var value=(CompoundTag)entry;
            if(value.hasUUID("Id")) data.captures.put(value.getUUID("Id"),value.getCompound("Data"));
        }
        return data;
    }
    public UUID put(CompoundTag data) {var id=UUID.randomUUID();captures.put(id,data.copy());setDirty();return id;}
    public CompoundTag get(UUID id) {var data=captures.get(id);return data==null?null:data.copy();}
    public boolean claim(UUID id,UUID projectile) {
        var data=captures.get(id);if(data==null || data.hasUUID("InFlight")) return false;
        data.putUUID("InFlight",projectile);setDirty();return true;
    }
    public boolean owns(UUID id,UUID projectile) {
        var data=captures.get(id);
        return data!=null && (projectile==null?!data.hasUUID("InFlight"):data.hasUUID("InFlight") && data.getUUID("InFlight").equals(projectile));
    }
    public void unclaim(UUID id,UUID projectile) {
        if(owns(id,projectile)) {captures.get(id).remove("InFlight");setDirty();}
    }
    public void remove(UUID id) {captures.remove(id);setDirty();}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        var list=new ListTag();
        captures.forEach((id,data)->{var entry=new CompoundTag();entry.putUUID("Id",id);entry.put("Data",data.copy());list.add(entry);});
        tag.put("Captures",list);return tag;
    }
}
