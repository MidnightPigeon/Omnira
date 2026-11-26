package com.mcmagic.omnira.loot;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

/** Transient provenance for one chest draw, including nested tables and the vanilla stack splitter. */
public final class FillerLootSource {
    public interface Context {
        FillerLootSource omnira$filler();
        void omnira$filler(FillerLootSource draw);
    }

    public net.minecraft.world.item.Item choice;
    public final Set<ItemStack> stacks = Collections.newSetFromMap(new IdentityHashMap<>());

    static Set<ItemStack> take(LootContext context) {
        var owner = (Context) context;
        var draw = owner.omnira$filler();
        owner.omnira$filler(null);
        return draw == null ? Set.of() : draw.stacks;
    }
}
