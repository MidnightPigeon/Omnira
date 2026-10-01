package com.mcmagic.omnira.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> SPELL_ELEMENTS=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("omnira","spell_elements"));
    private ModItemTags() {}
}
