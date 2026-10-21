package com.mcmagic.omnira.myriad;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.DeferredItem;

/** This order describes the ritual's presentation, never pedestal placement. */
public enum WorldComponent {
    STRATA("strata_skeleton", 0xA2A1AA),
    WATER("circulating_waterways", 0x58B6F2),
    SKY("flowing_sky_canopy", 0xBCF0EC),
    HEAT("geothermal_ember", 0xFF934D),
    LIFE("living_embryoplasm", 0xA8DB83),
    TIME("ages_dial", 0xB4DDB0),
    BOUNDARY("boundary_crystal_membrane", 0xA3D6EE),
    POSSIBILITY("phantasm_germinal_shard", 0xC7A0EF);

    public final String id;
    public final int color;
    public final DeferredItem<Item> item;

    WorldComponent(String id, int color) {
        this.id = id;
        this.color = color;
        this.item = ModItems.ITEMS.register(id, () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    }

    public static WorldComponent of(ItemStack stack) {
        for (var component : values()) if (stack.is(component.item.get())) return component;
        return null;
    }

    public static void init() {}
}
