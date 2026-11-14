package com.mcmagic.omnira.util;

import net.minecraft.nbt.CompoundTag;

public final class GameTimeCooldown {
    private GameTimeCooldown() {}

    public static boolean active(long now, long deadline, int duration) {
        long remaining = deadline - now;
        return deadline > now && remaining > 0 && remaining <= duration;
    }

    private static final String TRANSIT = "OmniraTransitTicks";

    public static boolean active(net.minecraft.world.entity.Entity entity, String key, long now, int duration) {
        var data = entity.getPersistentData();
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer)) return active(data, key, now, duration);
        if (data.contains(key)) {
            long deadline = data.getLong(key);
            data.remove(key);
            if (active(now, deadline, duration)) start(entity, key, now, (int)(deadline - now));
        }
        return data.contains(TRANSIT) && data.getCompound(TRANSIT).getInt(key) > 0;
    }

    public static void start(net.minecraft.world.entity.Entity entity, String key, long now, int duration) {
        var data = entity.getPersistentData();
        if (entity instanceof net.minecraft.server.level.ServerPlayer) {
            var timers = data.getCompound(TRANSIT);
            timers.putInt(key, duration);
            data.put(TRANSIT, timers);
            data.remove(key);
        } else data.putLong(key, now + duration);
    }

    public static void tickTransit(net.minecraft.server.level.ServerPlayer player) {
        var data = player.getPersistentData();
        if (!data.contains(TRANSIT)) return;
        var timers = data.getCompound(TRANSIT);
        for (var it = timers.getAllKeys().iterator(); it.hasNext();) {
            String key = it.next();
            int ticks = timers.getInt(key);
            if (ticks <= 1) it.remove();
            else timers.putInt(key, ticks - 1);
        }
        if (timers.isEmpty()) data.remove(TRANSIT);
    }

    public static boolean active(CompoundTag data, String key, long now, int duration) {
        long deadline = data.getLong(key);
        if (active(now, deadline, duration)) return true;
        if (deadline > now) data.remove(key);
        return false;
    }
}
