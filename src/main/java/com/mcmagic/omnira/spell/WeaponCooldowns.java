package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.weaver.FounderApprovalItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Per-player countdowns, accessible during handoff even if the previous wielder is offline. */
@EventBusSubscriber(modid="omnira")
public final class WeaponCooldowns extends SavedData {
    private final Map<UUID, Map<UUID, Integer>> players = new HashMap<>();
    private final Map<UUID, UUID> owners = new HashMap<>();

    private static final Factory<WeaponCooldowns> FACTORY = new Factory<>(WeaponCooldowns::new, WeaponCooldowns::load, null);

    static WeaponCooldowns get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                FACTORY, "omnira_weapon_cooldowns");
    }

    private void put(UUID player, UUID cast, int remaining) {
        players.computeIfAbsent(player, key -> new HashMap<>()).put(cast, remaining);
        owners.put(cast, player);
    }

    UUID start(UUID player, int ticks) {
        UUID cast = UUID.randomUUID();
        put(player, cast, ticks);
        setDirty();
        return cast;
    }

    boolean active(UUID player, UUID cast) {
        UUID owner = owners.get(cast);
        if (owner == null) return false;
        if (!owner.equals(player)) {
            var previous = players.get(owner);
            put(player, cast, previous.remove(cast));
            if (previous.isEmpty()) players.remove(owner);
            setDirty();
        }
        return true;
    }

    private void advance(UUID player) {
        var active = players.get(player);
        if (active == null) return;
        var entries = active.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            if (entry.getValue() > 1) entry.setValue(entry.getValue() - 1);
            else {
                owners.remove(entry.getKey());
                entries.remove();
            }
        }
        if (active.isEmpty()) players.remove(player);
        setDirty();
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var timers = get(player.server);
            if ((timers.players.containsKey(player.getUUID()) || FounderApprovalItem.hasTimers(player))
                    && !com.mcmagic.omnira.time.StilledTime.pauseSpellCooldown(player)) {
                timers.advance(player.getUUID());
                FounderApprovalItem.advance(player);
            }
            com.mcmagic.omnira.util.GameTimeCooldown.tickTransit(player);
        }
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        players.forEach((owner, timers) -> {
            var data = new CompoundTag();
            timers.forEach((cast, ticks) -> data.putInt(cast.toString(), ticks));
            tag.put(owner.toString(), data);
        });
        return tag;
    }

    private static WeaponCooldowns load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new WeaponCooldowns();
        for (String owner : tag.getAllKeys()) {
            var data = tag.getCompound(owner);
            for (String cast : data.getAllKeys()) {
                int ticks = data.getInt(cast);
                if (ticks > 0 && ticks <= 72000) result.put(UUID.fromString(owner), UUID.fromString(cast), ticks);
            }
        }
        return result;
    }
}
