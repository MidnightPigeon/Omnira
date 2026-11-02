package com.mcmagic.omnira.loot;

import com.mcmagic.omnira.menu.CrystalProcessingTableMenu;
import com.mcmagic.omnira.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** One roll after the root table is resolved, including all raw nested tables. */
public final class PreparedSpellLoot extends LootModifier {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "omnira");
    public static final MapCodec<PreparedSpellLoot> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).and(i.group(
            Codec.floatRange(0, 1).fieldOf("chance").forGetter(m -> m.chance),
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC.listOf()).fieldOf("replaceable")
                    .forGetter(m -> m.replaceable)
    )).apply(i, PreparedSpellLoot::new));
    static { TYPES.register("prepared_spell", () -> CODEC); }

    private final float chance;
    private final Map<ResourceLocation, List<ResourceLocation>> replaceable;
    private final Map<ResourceLocation, Set<ResourceLocation>> candidates;

    public PreparedSpellLoot(LootItemCondition[] conditions, float chance,
                             Map<ResourceLocation, List<ResourceLocation>> replaceable) {
        super(conditions);
        this.chance = chance;
        this.replaceable = replaceable.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, e -> List.copyOf(e.getValue())));
        this.candidates = replaceable.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, e -> Set.copyOf(e.getValue())));
    }

    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        replace(loot, context.getQueriedLootTableId(), context.getRandom());
        return loot;
    }

    public void replace(List<ItemStack> loot, ResourceLocation table, RandomSource random) {
        var allowed = candidates.get(table);
        if (allowed == null || allowed.isEmpty()) return;
        var slots = new java.util.ArrayList<Integer>();
        for (int i = 0; i < loot.size(); i++) {
            var stack = loot.get(i);
            // Never replace enchanted, named or otherwise customized rewards with the same item ID.
            if (!stack.isEmpty() && stack.getComponentsPatch().isEmpty()
                    && allowed.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) slots.add(i);
        }
        // No fallback insertion: a treasure with no ordinary filler retains all of its rewards.
        if (slots.isEmpty() || random.nextFloat() >= chance) return;
        loot.set(slots.get(random.nextInt(slots.size())), createCrystal(random));
    }

    public static ItemStack createCrystal(RandomSource random) {
        var cores = List.of(ModItems.EARTH_MICROCORE, ModItems.WATER_MICROCORE,
                ModItems.FIRE_MICROCORE, ModItems.AIR_MICROCORE);
        var elements = List.of(ModItems.SHARP_BREATH, ModItems.HEALING_DEW,
                ModItems.DISSOCIATION_THREAD, ModItems.CONSTRUCTION_MATRIX);
        var input = new SimpleContainer(CrystalProcessingTableMenu.TABLE_SLOT_COUNT);
        input.setItem(CrystalProcessingTableMenu.TARGET_CORE_SLOT, cores.get(random.nextInt(4)).get().getDefaultInstance());
        if (random.nextBoolean()) input.setItem(CrystalProcessingTableMenu.SHAPE_CORE_SLOT,
                cores.get(random.nextInt(4)).get().getDefaultInstance());
        int first = random.nextInt(4);
        input.setItem(CrystalProcessingTableMenu.ELEMENT_SLOT_START, elements.get(first).get().getDefaultInstance());
        if (random.nextBoolean()) {
            int second = random.nextInt(3);
            if (second >= first) second++;
            input.setItem(CrystalProcessingTableMenu.ELEMENT_SLOT_START + 1, elements.get(second).get().getDefaultInstance());
        }
        return CrystalProcessingTableMenu.previewCrystal(input);
    }

    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
