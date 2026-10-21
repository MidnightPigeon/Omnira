package com.mcmagic.omnira.codex.edit;

import java.util.Map;
import java.util.List;
import java.util.Collection;
import java.util.TreeSet;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootTable;

/** Uses existing authored loot. Tags can override per biome without new registrations. */
public final class EcologyLoot {
    private static final Map<String,String> TABLES = Map.ofEntries(
            Map.entry("dawn_crystal_fields", "radiant_courtyard"),
            Map.entry("shadow_woodland", "shadow_library_stacks"),
            Map.entry("mirror_dream_border", "mirror_gallery_gallery"),
            Map.entry("stilled_wastes", "golden_throne_tower"),
            Map.entry("fleeting_woods", "kirisame_workshop"),
            Map.entry("reversion_mire", "rusty_lake_mill_cellar"),
            Map.entry("epochal_cliffs", "drilling_platform"),
            Map.entry("recurrence_garden", "looking_glass_garden_rabbit"),
            Map.entry("eventide_ruins", "doomsday_rite"),
            Map.entry("folded_caverns", "fold_observatory_lower"),
            Map.entry("dislocated_geodes", "calibration_materials"),
            Map.entry("inverted_abyss", "skyreach_supplies"),
            Map.entry("void_starsea", "void_lighthouse"),
            Map.entry("topological_labyrinth", "topological_labyrinth"),
            Map.entry("phase_crystal_grove", "calibration_supplies"));
    // Compatibility biomes have no exclusive structure: reuse their region's authored treasures.
    private static final Map<String,List<String>> REGIONAL_TABLES = Map.of(
            "temporal_continent", List.of("golden_throne_tower", "kirisame_workshop", "rusty_lake_mill_cellar",
                    "drilling_platform", "looking_glass_garden_rabbit", "doomsday_rite"),
            "dream_islands", List.of("radiant_courtyard"));
    private EcologyLoot() {}
    static Map<String,String> defaults() { return TABLES; }

    /** A pack may tag biomes with omnira:codex/ecology/loot/<table namespace>/<table path>. */
    public static ResourceKey<LootTable> table(ServerLevel level, Holder<Biome> biome) {
        var mapped=mappedTables(biome);
        if(!mapped.isEmpty())return choose(level,mapped);
        var biomeKey=biome.unwrapKey().orElseThrow();
        if(biomeKey.location().getNamespace().equals("minecraft")){
            if(biome.is(net.minecraft.tags.BiomeTags.IS_NETHER))return key("minecraft:chests/nether_bridge");
            if(biome.is(net.minecraft.tags.BiomeTags.IS_END))return key("minecraft:chests/end_city_treasure");
            if(biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN))return key("minecraft:chests/shipwreck_treasure");
            return key("minecraft:chests/simple_dungeon");
        }
        // Native-dimension metadata only: never mistake the destination world for the source.
        var sources=java.util.stream.StreamSupport.stream(level.getServer().getAllLevels().spliterator(),false)
                .filter(source->source.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream().anyMatch(b->b.is(biomeKey)))
                .sorted(java.util.Comparator.comparing(source->source.dimension().location())).toList();
        for(var source:sources){
            var pool=dimensionTables(level,source.dimension().location(),source.getChunkSource().getGenerator().getBiomeSource().possibleBiomes());
            if(!pool.isEmpty())return choose(level,pool);
        }
        if (biome.is(net.minecraft.tags.BiomeTags.IS_NETHER) || sources.stream().anyMatch(source->source.dimension().equals(net.minecraft.world.level.Level.NETHER)))
            return key("minecraft:chests/nether_bridge");
        if (biome.is(net.minecraft.tags.BiomeTags.IS_END) || sources.stream().anyMatch(source->source.dimension().equals(net.minecraft.world.level.Level.END)))
            return key("minecraft:chests/end_city_treasure");
        if (biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN)) return key("minecraft:chests/shipwreck_treasure");
        return key("minecraft:chests/simple_dungeon");
    }

    private static List<ResourceKey<LootTable>> mappedTables(Holder<Biome> biome) {
        var override = biome.tags().map(TagKey::location)
                .filter(id -> id.getNamespace().equals("omnira") && id.getPath().startsWith("codex/ecology/loot/"))
                .sorted().findFirst();
        if (override.isPresent()) {
            String rest = override.get().getPath().substring("codex/ecology/loot/".length());
            int slash = rest.indexOf('/');
            if (slash > 0) return List.of(key(rest.substring(0,slash) + ":" + rest.substring(slash+1)));
        }
        if(biome.unwrapKey().isEmpty())return List.of();
        var id = biome.unwrapKey().orElseThrow().location();
        if (id.getNamespace().equals("omnira") && TABLES.containsKey(id.getPath()))
            return List.of(key("omnira:chests/" + TABLES.get(id.getPath())));
        if (id.getNamespace().equals("omnira") && REGIONAL_TABLES.containsKey(id.getPath())) {
            var pool=REGIONAL_TABLES.get(id.getPath());
            return pool.stream().map(path->key("omnira:chests/"+path)).toList();
        }
        return List.of();
    }

    static List<ResourceKey<LootTable>> dimensionTables(ServerLevel level, ResourceLocation dimension,
                                                       Collection<? extends Holder<Biome>> biomes){
        var tables=new TreeSet<ResourceLocation>();
        for(var biome:biomes)for(var table:mappedTables(biome))
            if(level.getServer().reloadableRegistries().getLootTable(table)!=LootTable.EMPTY)tables.add(table.location());
        for(var id:level.getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE)){
            if(!id.getNamespace().equals("minecraft")||!id.getPath().startsWith("chests/"))continue;
            String path=id.getPath().substring("chests/".length());
            boolean nether=path.equals("nether_bridge")||path.startsWith("bastion_");
            boolean end=path.equals("end_city_treasure");
            if(dimension.equals(net.minecraft.world.level.Level.OVERWORLD.location())&&!nether&&!end
                    ||dimension.equals(net.minecraft.world.level.Level.NETHER.location())&&(nether||path.equals("ruined_portal"))
                    ||dimension.equals(net.minecraft.world.level.Level.END.location())&&end)tables.add(id);
        }
        // Independent mod dimensions may provide chest tables without per-biome mappings.
        if(tables.isEmpty()&&!dimension.getNamespace().equals("minecraft"))
            for(var id:level.getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE))
                if(id.getNamespace().equals(dimension.getNamespace())&&id.getPath().startsWith("chests/"))tables.add(id);
        return tables.stream().map(id->ResourceKey.create(Registries.LOOT_TABLE,id))
                .filter(table->level.getServer().reloadableRegistries().getLootTable(table)!=LootTable.EMPTY).toList();
    }

    private static ResourceKey<LootTable> choose(ServerLevel level,List<ResourceKey<LootTable>> tables){
        return tables.get(level.random.nextInt(tables.size()));
    }

    private static ResourceKey<LootTable> key(String id) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(id));
    }
}
