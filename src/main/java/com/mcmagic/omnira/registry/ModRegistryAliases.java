package com.mcmagic.omnira.registry;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Keeps existing worlds and stored items readable after public ID corrections. */
public final class ModRegistryAliases {
    private ModRegistryAliases() {}

    public static void install() {
        alias(ModBlocks.BLOCKS, "advanced_forge", "advanced_assembly_table");
        alias(ModItems.ITEMS, "advanced_forge", "advanced_assembly_table");
        alias(ModBlockEntityTypes.BLOCK_ENTITY_TYPES, "advanced_forge", "advanced_assembly_table");
        alias(ModMenuTypes.MENU_TYPES, "advanced_forge", "advanced_assembly_table");

        alias(ModBlocks.BLOCKS, "test_spell_core", "primordial_spell_core");
        alias(ModItems.ITEMS, "test_spell_core", "primordial_spell_core");
        alias(ModItems.ITEMS, "crystallized_nail", "crystallized_bone_nail");

        alias(ModBlocks.BLOCKS, "spatial_crystal", "rough_spatial_crystal_block");
        alias(ModItems.ITEMS, "spatial_crystal", "rough_spatial_crystal_block");
        alias(ModBlocks.BLOCKS, "excited_spatial_crystal", "excited_rough_spatial_crystal_block");
        alias(ModItems.ITEMS, "excited_spatial_crystal", "excited_rough_spatial_crystal_block");

        for (String variant : new String[] {"", "cracked_", "sandbound_"}) {
            alias(ModBlocks.BLOCKS, variant + "eventide_bricks", variant + "time_eroded_bricks");
            alias(ModItems.ITEMS, variant + "eventide_bricks", variant + "time_eroded_bricks");
            for (String shape : new String[] {"stairs", "slab", "wall"}) {
                String oldName = variant + "eventide_brick_" + shape;
                String newName = variant + "time_eroded_brick_" + shape;
                alias(ModBlocks.BLOCKS, oldName, newName);
                alias(ModItems.ITEMS, oldName, newName);
            }
        }

        ModRecipes.aliasAdvancedAssemblyTable();
    }

    private static void alias(DeferredRegister<?> register, String oldId, String newId) {
        register.addAlias(id(oldId), id(newId));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("omnira", path);
    }
}
