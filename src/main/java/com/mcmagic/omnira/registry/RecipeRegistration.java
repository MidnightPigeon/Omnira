package com.mcmagic.omnira.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/** Shared registration plumbing; recipe validation remains in each recipe codec. */
public final class RecipeRegistration {
    public static <R extends Recipe<?>> RecipeType<R> type(ResourceLocation id) {
        return new RecipeType<>() {
            @Override public String toString() { return id.toString(); }
        };
    }

    public static <R extends Recipe<?>> RecipeSerializer<R> serializer(
            MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec) {
        return new RecipeSerializer<>() {
            @Override public MapCodec<R> codec() { return codec; }
            @Override public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() { return streamCodec; }
        };
    }

    private RecipeRegistration() {}
}
