package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.recipe.AnalysisRecipe;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Omnira.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Omnira.MOD_ID);
    public static final DeferredHolder<RecipeType<?>,RecipeType<com.mcmagic.omnira.forging.AdvancedForgeRecipe>> ADVANCED_FORGE_TYPE=
            TYPES.register("advanced_assembly_table",()->new RecipeType<>() {});
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<com.mcmagic.omnira.forging.AdvancedForgeRecipe>> ADVANCED_FORGE_SERIALIZER=
            SERIALIZERS.register("advanced_assembly_table",()->new RecipeSerializer<>() {
                public MapCodec<com.mcmagic.omnira.forging.AdvancedForgeRecipe> codec(){return com.mcmagic.omnira.forging.AdvancedForgeRecipe.CODEC;}
                public StreamCodec<RegistryFriendlyByteBuf,com.mcmagic.omnira.forging.AdvancedForgeRecipe> streamCodec(){return com.mcmagic.omnira.forging.AdvancedForgeRecipe.STREAM_CODEC;}
            });
    public static final DeferredHolder<RecipeType<?>, RecipeType<AnalysisRecipe>> ANALYSIS_TYPE = TYPES.register("analysis", () -> new RecipeType<>() {
        @Override public String toString() { return "omnira:analysis"; }
    });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AnalysisRecipe>> ANALYSIS_SERIALIZER = SERIALIZERS.register("analysis", () -> new RecipeSerializer<>() {
        @Override public MapCodec<AnalysisRecipe> codec() { return AnalysisRecipe.CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, AnalysisRecipe> streamCodec() { return AnalysisRecipe.STREAM_CODEC; }
    });
    public static final DeferredHolder<RecipeType<?>,RecipeType<com.mcmagic.omnira.recipe.CondensationRecipe>> CONDENSATION_TYPE=
            TYPES.register("condensation",()->new RecipeType<>() {@Override public String toString() {return "omnira:condensation";}});
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<com.mcmagic.omnira.recipe.CondensationRecipe>> CONDENSATION_SERIALIZER=
            SERIALIZERS.register("condensation",()->new RecipeSerializer<>() {
                @Override public MapCodec<com.mcmagic.omnira.recipe.CondensationRecipe> codec() {return com.mcmagic.omnira.recipe.CondensationRecipe.CODEC;}
                @Override public StreamCodec<RegistryFriendlyByteBuf,com.mcmagic.omnira.recipe.CondensationRecipe> streamCodec() {return com.mcmagic.omnira.recipe.CondensationRecipe.STREAM_CODEC;}
            });
    public static final DeferredHolder<RecipeType<?>,RecipeType<com.mcmagic.omnira.recipe.AssemblyRecipe>> ASSEMBLY_TYPE=
            TYPES.register("assembly",()->new RecipeType<>() {@Override public String toString() {return "omnira:assembly";}});
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<com.mcmagic.omnira.recipe.AssemblyRecipe>> ASSEMBLY_SERIALIZER=
            SERIALIZERS.register("assembly",()->new RecipeSerializer<>() {
                @Override public MapCodec<com.mcmagic.omnira.recipe.AssemblyRecipe> codec() {return com.mcmagic.omnira.recipe.AssemblyRecipe.CODEC;}
                @Override public StreamCodec<RegistryFriendlyByteBuf,com.mcmagic.omnira.recipe.AssemblyRecipe> streamCodec() {return com.mcmagic.omnira.recipe.AssemblyRecipe.STREAM_CODEC;}
            });
    private ModRecipes() {}
    public static final DeferredHolder<RecipeType<?>,RecipeType<com.mcmagic.omnira.recipe.VesselConversionRecipe>> VESSEL_CONVERSION_TYPE=
            TYPES.register("vessel_conversion",()->new RecipeType<>() {});
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<com.mcmagic.omnira.recipe.VesselConversionRecipe>> VESSEL_CONVERSION_SERIALIZER=
            SERIALIZERS.register("vessel_conversion",()->new RecipeSerializer<>() {
                @Override public MapCodec<com.mcmagic.omnira.recipe.VesselConversionRecipe> codec(){return com.mcmagic.omnira.recipe.VesselConversionRecipe.CODEC;}
                @Override public StreamCodec<RegistryFriendlyByteBuf,com.mcmagic.omnira.recipe.VesselConversionRecipe> streamCodec(){return com.mcmagic.omnira.recipe.VesselConversionRecipe.STREAM;}
            });
    public static final DeferredHolder<RecipeType<?>,RecipeType<com.mcmagic.omnira.recipe.SwordShapingRecipe>> SWORD_SHAPING_TYPE=
            TYPES.register("sword_shaping",()->new RecipeType<>() {@Override public String toString(){return "omnira:sword_shaping";}});
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<com.mcmagic.omnira.recipe.SwordShapingRecipe>> SWORD_SHAPING_SERIALIZER=
            SERIALIZERS.register("sword_shaping",()->new RecipeSerializer<>() {
                @Override public MapCodec<com.mcmagic.omnira.recipe.SwordShapingRecipe> codec(){return com.mcmagic.omnira.recipe.SwordShapingRecipe.CODEC;}
                @Override public StreamCodec<RegistryFriendlyByteBuf,com.mcmagic.omnira.recipe.SwordShapingRecipe> streamCodec(){return com.mcmagic.omnira.recipe.SwordShapingRecipe.STREAM_CODEC;}
            });
    public static void aliasAdvancedAssemblyTable() {
        var oldId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"advanced_forge");
        var newId=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"advanced_assembly_table");
        TYPES.addAlias(oldId,newId);
        SERIALIZERS.addAlias(oldId,newId);
    }

    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }
}
