package com.mcmagic.omnira.pulse;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Connection order survives server reloads and never depends on block entity tick order. */
final class PulseOrder extends SavedData {
    private long next=1;private final Map<String,Long> orders=new HashMap<>();
    static PulseOrder get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PulseOrder::new,PulseOrder::load),"omnira_pulse_order");}
    long order(String key){return orders.computeIfAbsent(key,k->{setDirty();return next++;});}
    void disconnected(String key){if(orders.remove(key)!=null)setDirty();}
    static PulseOrder load(CompoundTag tag,HolderLookup.Provider lookup){
        var result=new PulseOrder();result.next=Math.max(1,tag.getLong("Next"));var entries=tag.getList("Entries",Tag.TAG_COMPOUND);
        for(int i=0;i<entries.size();i++){var entry=entries.getCompound(i);result.orders.put(entry.getString("Key"),entry.getLong("Order"));}return result;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){
        tag.putLong("Next",next);var entries=new ListTag();orders.forEach((key,order)->{var entry=new CompoundTag();entry.putString("Key",key);entry.putLong("Order",order);entries.add(entry);});tag.put("Entries",entries);return tag;
    }
}
