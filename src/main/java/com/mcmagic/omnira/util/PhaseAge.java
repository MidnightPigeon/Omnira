package com.mcmagic.omnira.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/** Elapsed simulation ticks of a finite phase. Old saves are converted once the level is attached. */
public final class PhaseAge {
    private final String key, oldKey;
    private final int limit;
    private int ticks = -1;
    private long oldStart = -1;

    public PhaseAge(String key, String oldKey, int limit) {
        this.key = key;
        this.oldKey = oldKey;
        this.limit = limit;
    }

    public void start() { ticks = 0; oldStart = -1; }
    public void stop() { ticks = -1; oldStart = -1; }
    public boolean active() { return ticks >= 0 || oldStart >= 0; }

    public int ticks(Level level) {
        if (oldStart >= 0 && level != null) {
            ticks = (int) Math.clamp(level.getGameTime() - oldStart, 0L, limit);
            oldStart = -1;
        }
        return ticks;
    }

    public boolean advance(Level level) {
        int previous = ticks(level);
        if (previous < 0 || previous >= limit) return false;
        ticks++;
        return true;
    }

    // Server-side block entities already receive the extra Fleeting Woods tick.
    public boolean advanceAnimated(Level level, net.minecraft.core.BlockPos pos) {
        if (!advance(level)) return false;
        if (level.isClientSide && com.mcmagic.omnira.time.FleetingTime.accelerated(level,pos)) advance(level);
        return true;
    }

    public void load(CompoundTag tag) {
        ticks = tag.contains(key) ? Math.clamp(tag.getInt(key), -1, limit) : -1;
        oldStart = !tag.contains(key) && tag.contains(oldKey) ? tag.getLong(oldKey) : -1;
    }

    public void save(CompoundTag tag) {
        if (oldStart >= 0) tag.putLong(oldKey, oldStart);
        else tag.putInt(key, ticks);
    }
}
