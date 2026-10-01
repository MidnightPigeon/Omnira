package com.mcmagic.omnira.fate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** All talent/curse timers live here so cleansing never needs a per-kind dispatch. */
public final class FateRuntime {
    private static final String ROOT="OmniraFateRuntime";
    private static final java.util.List<String> LEGACY=java.util.List.of("OmniraLandRegeneration","OmniraDistortionTicks");
    public static CompoundTag data(ServerPlayer player){
        var persistent=player.getPersistentData();
        if(!persistent.contains(ROOT,10))persistent.put(ROOT,new CompoundTag());
        var runtime=persistent.getCompound(ROOT);
        // Migrate the original flat save keys; new effects must use this compound directly.
        for(String key:LEGACY)if(persistent.contains(key)){
            if(!runtime.contains(key))runtime.put(key,persistent.get(key).copy());
            persistent.remove(key);
        }
        return runtime;
    }
    public static void clear(ServerPlayer player){
        player.getPersistentData().remove(ROOT);
        for(String key:LEGACY)player.getPersistentData().remove(key);
    }
    private FateRuntime(){}
}
