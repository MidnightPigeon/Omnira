package com.mcmagic.omnira.codex.edit;

import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

/** Extensions: omnira:codex/ecology/<group>, and .../exclude/<group> biome tags. */
public final class EcologySelection {
    public enum Group { EARTH, WATER, FIRE, AIR, LIGHT, DARK, TIME, SPACE, OMNI }

    private static final Set<String> TIME = Set.of("temporal_continent", "stilled_wastes",
            "fleeting_woods", "epochal_cliffs", "recurrence_garden", "eventide_ruins");
    private static final ResourceLocation EXCLUDED_BIOME=ResourceLocation.parse("omnira:spacetime_corridor");
    private static final Set<String> SPACE = Set.of("folded_caverns",
            "dislocated_geodes", "inverted_abyss", "topological_labyrinth", "phase_crystal_grove");
    private EcologySelection() {}

    public static TagKey<Biome> tag(Group group, boolean exclude) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("omnira",
                "codex/ecology/" + (exclude ? "exclude/" : "") + group.name().toLowerCase(Locale.ROOT)));
    }

    public static List<Holder.Reference<Biome>> candidates(Registry<Biome> registry, Group group) {
        return registry.holders().filter(holder -> matches(holder, group)).toList();
    }

    public static Optional<Holder<Biome>> select(Registry<Biome> registry, Group group, RandomSource random) {
        var pool = candidates(registry, group);
        return pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(random.nextInt(pool.size())));
    }

    public static boolean matches(Holder<Biome> biome, Group group) {
        if(biome.unwrapKey().map(key->key.location().equals(EXCLUDED_BIOME)).orElse(false))return false;
        if (group == Group.OMNI) return biome.unwrapKey().isPresent();
        if (biome.is(tag(group, true))) return false;
        if (biome.is(tag(group, false))) return true;
        var key = biome.unwrapKey();
        if (key.isEmpty()) return false;
        var id = key.get().location();
        String path = id.getPath();
        if (id.getNamespace().equals("omnira")) return switch (group) {
            case AIR -> path.equals("mirror_dream_border");
            case LIGHT -> Set.of("dawn_crystal_fields", "dream_islands", "void_starsea").contains(path);
            case DARK -> Set.of("shadow_woodland", "reversion_mire").contains(path);
            case TIME -> TIME.contains(path);
            case SPACE -> SPACE.contains(path);
            default -> false;
        };
        if (!id.getNamespace().equals("minecraft")) return false;
        boolean ocean = biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN);
        boolean nether = biome.is(net.minecraft.tags.BiomeTags.IS_NETHER);
        boolean end = biome.is(net.minecraft.tags.BiomeTags.IS_END);
        float temperature = biome.value().getBaseTemperature();
        boolean cave = Set.of("deep_dark", "lush_caves", "dripstone_caves").contains(path);
        return switch (group) {
            case EARTH -> cave || (!ocean && !nether && !end && temperature > 0.15F && temperature < 1.0F);
            case WATER -> ocean || (!nether && !end && temperature <= 0.15F);
            case FIRE -> (nether && !path.equals("soul_sand_valley")) || (!end && !nether && temperature >= 1.0F);
            case AIR -> end;
            default -> false;
        };
    }
}
