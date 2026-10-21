package com.mcmagic.omnira.myriad;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

public final class WorldSeedContent {
    public static final DeferredItem<Item> DORMANT = ModItems.ITEMS.register("dormant_world_seed", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> ACTIVE = ModItems.ITEMS.register("active_world_seed", () -> new ActiveWorldSeedItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static void init() {}
    private WorldSeedContent() {}
}
