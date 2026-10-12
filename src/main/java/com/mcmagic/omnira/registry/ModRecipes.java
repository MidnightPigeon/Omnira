package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.alchemy.ReactionRecipe;
import com.mcmagic.omnira.forging.AdvancedForgeRecipe;
import com.mcmagic.omnira.recipe.AnalysisRecipe;
import com.mcmagic.omnira.recipe.CondensationRecipe;
import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.recipe.StarlightConversionRecipe;
import com.mcmagic.omnira.recipe.VesselConversionRecipe;
import com.mcmagic.omnira.recipe.SwordShapingRecipe;
import com.mcmagic.omnira.recipe.SafeShapedRecipe;
import com.mcmagic.omnira.recipe.StarClusterOrbCraftRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Omnira.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Omnira.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ReactionRecipe>> REACTION_TYPE =
            TYPES.register("reaction", () -> RecipeRegistration.type(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "reaction")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReactionRecipe>> REACTION_SERIALIZER =
            SERIALIZERS.register("reaction", () -> RecipeRegistration.serializer(ReactionRecipe.CODEC, ReactionRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<AdvancedForgeRecipe>> ADVANCED_FORGE_TYPE =
            TYPES.register("advanced_assembly_table", () -> RecipeRegistration.type(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "advanced_assembly_table")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AdvancedForgeRecipe>> ADVANCED_FORGE_SERIALIZER =
            SERIALIZERS.register("advanced_assembly_table", () -> RecipeRegistration.serializer(AdvancedForgeRecipe.CODEC, AdvancedForgeRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<AnalysisRecipe>> ANALYSIS_TYPE =
            TYPES.register("analysis", () -> RecipeRegistration.type(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "analysis")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AnalysisRecipe>> ANALYSIS_SERIALIZER =
            SERIALIZERS.register("analysis", () -> RecipeRegistration.serializer(AnalysisRecipe.CODEC, AnalysisRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CondensationRecipe>> CONDENSATION_TYPE =
            TYPES.register("condensation", () -> RecipeRegistration.type(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "condensation")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CondensationRecipe>> CONDENSATION_SERIALIZER =
            SERIALIZERS.register("condensation", () -> RecipeRegistration.serializer(CondensationRecipe.CODEC, CondensationRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblyRecipe>> ASSEMBLY_TYPE =
            TYPES.register("assembly", () -> RecipeRegistration.type(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "assembly")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblyRecipe>> ASSEMBLY_SERIALIZER =
            SERIALIZERS.register("assembly", () -> RecipeRegistration.serializer(AssemblyRecipe.CODEC, AssemblyRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<StarlightConversionRecipe>> STARLIGHT_TYPE =
            TYPES.register("starlight_conversion", () -> RecipeRegistration.type(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "starlight_conversion")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StarlightConversionRecipe>> STARLIGHT_SERIALIZER =
            SERIALIZERS.register("starlight_conversion", () -> RecipeRegistration.serializer(StarlightConversionRecipe.CODEC, StarlightConversionRecipe.STREAM));

    public static final DeferredHolder<RecipeType<?>, RecipeType<VesselConversionRecipe>> VESSEL_CONVERSION_TYPE =
            TYPES.register("vessel_conversion", () -> RecipeRegistration.type(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "vessel_conversion")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VesselConversionRecipe>> VESSEL_CONVERSION_SERIALIZER =
            SERIALIZERS.register("vessel_conversion", () -> RecipeRegistration.serializer(VesselConversionRecipe.CODEC, VesselConversionRecipe.STREAM));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SwordShapingRecipe>> SWORD_SHAPING_TYPE =
            TYPES.register("sword_shaping", () -> RecipeRegistration.type(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "sword_shaping")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SwordShapingRecipe>> SWORD_SHAPING_SERIALIZER =
            SERIALIZERS.register("sword_shaping", () -> RecipeRegistration.serializer(SwordShapingRecipe.CODEC, SwordShapingRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SafeShapedRecipe>> SAFE_SHAPED_SERIALIZER =
            SERIALIZERS.register("safe_shaped", () -> RecipeRegistration.serializer(SafeShapedRecipe.CODEC, SafeShapedRecipe.STREAM));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StarClusterOrbCraftRecipe>> STELLAR_ORB_CRAFT_SERIALIZER =
            SERIALIZERS.register("star_cluster_orb_crafting", () -> RecipeRegistration.serializer(StarClusterOrbCraftRecipe.CODEC, StarClusterOrbCraftRecipe.STREAM));

    public static void aliasAdvancedAssemblyTable() {
        var oldId = ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "advanced_forge");
        var newId = ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID, "advanced_assembly_table");
        TYPES.addAlias(oldId, newId);
        SERIALIZERS.addAlias(oldId, newId);
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    private ModRecipes() {}
}
