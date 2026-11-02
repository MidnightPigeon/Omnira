package com.mcmagic.omnira.gametest;

import com.google.gson.JsonParser;
import com.mcmagic.omnira.loot.PreparedSpellLoot;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.spell.SpellPayload;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.gametest.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@GameTestHolder("omnira_prepared_spell")
@PrefixGameTestTemplate(false)
public final class PreparedSpellLootGameTests {
    @GameTest(template="spell_arena") public static void personalLootrCrystalPersists(GameTestHelper h) {
        if (net.neoforged.fml.ModList.get().isLoaded("lootr")) LootrOrbTestCases.preparedCrystalPersists(h);
        else h.succeed();
    }

    private static PreparedSpellLoot modifier(float chance) {
        try (var in = PreparedSpellLootGameTests.class.getResourceAsStream("/data/omnira/loot_modifiers/prepared_spell.json")) {
            var json = JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8)).getAsJsonObject();
            json.addProperty("chance", chance);
            return PreparedSpellLoot.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    @GameTest(template="spell_arena") public static void generatedSpellsRespectRecipeRules(GameTestHelper h) {
        var random = RandomSource.create(54198);
        var shapes = new HashSet<String>(); var pairs = new HashSet<Set<String>>();
        int doubleCores = 0, doubleElements = 0;
        for (int i=0; i<4000; i++) {
            var crystal = PreparedSpellLoot.createCrystal(random);
            var pattern = crystal.get(ModDataComponents.SPELL_PATTERN);
            var payload = SpellPayload.of(crystal);
            h.assertTrue(crystal.is(ModItems.LOW_TIER_MAGIC_CRYSTAL) && crystal.getCount()==1, "Wrong crystal tier/count");
            h.assertTrue(pattern!=null && pattern.targetElement().isPrimary()
                    && (pattern.shapeElement()==null || pattern.shapeElement().isPrimary()), "Non-primary shape");
            shapes.add(pattern.serializedName());
            int count = payload.effects().size();
            h.assertTrue(count>=1 && count<=2 && new HashSet<>(payload.keywords()).size()==count, "Duplicate/missing elements");
            h.assertTrue(Set.of("harm","healing","dissociation","construction").containsAll(payload.keywords()), "Forbidden element/modifier");
            h.assertTrue(payload.effects().stream().allMatch(e -> !e.infused() && e.enhancement()==0 && e.delay()==0 && e.duration()==0), "Unexpected modifier");
            h.assertTrue(payload.baseCost()==10*(1+(pattern.shapeElement()==null?0:1)+count), "Crafting mana cost mismatch");
            if (pattern.shapeElement()!=null) doubleCores++;
            if (count==2) { doubleElements++; pairs.add(Set.copyOf(payload.keywords())); }
        }
        h.assertTrue(shapes.size()==20 && pairs.size()==6, "Missing legal shape/element combinations");
        h.assertTrue(doubleCores>1800 && doubleCores<2200 && doubleElements>1800 && doubleElements<2200, "Counts are not 50/50");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void replacesOnlyPlainFiller(GameTestHelper h) {
        var modifier = modifier(1); var random = RandomSource.create(62);
        var key = ResourceLocation.parse("omnira:chests/apprentice_hut");
        var named = new ItemStack(Items.PAPER);
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keep"));
        var loot = new ArrayList<>(List.of(new ItemStack(Items.PAPER,6),named,new ItemStack(Items.DIAMOND)));
        modifier.replace(loot,key,random);
        h.assertTrue(loot.size()==3 && loot.get(0).is(ModItems.LOW_TIER_MAGIC_CRYSTAL)
                && loot.get(1)==named && loot.get(2).is(Items.DIAMOND), "Special reward changed or extra reward inserted");
        for (String table : List.of("omnira:chests/shadow_library_sealed", "minecraft:chests/trial_chambers/intersection",
                "minecraft:chests/jungle_temple_dispenser", "minecraft:chests/trial_chambers/reward_common",
                "minecraft:gameplay/fishing", "foreign:chests/test")) {
            var excluded = new ArrayList<>(List.of(new ItemStack(Items.PAPER),new ItemStack(Items.DIAMOND)));
            modifier.replace(excluded,ResourceLocation.parse(table),random);
            h.assertTrue(excluded.get(0).is(Items.PAPER) && excluded.get(1).is(Items.DIAMOND), "Unrelated loot modified: "+table);
        }
        var noFiller = new ArrayList<>(List.of(new ItemStack(Items.DIAMOND)));
        modifier.replace(noFiller,key,random);
        h.assertTrue(noFiller.size()==1 && noFiller.getFirst().is(Items.DIAMOND), "No-filler fallback destroyed treasure");
        h.succeed();
    }

    @GameTest(template="spell_arena", timeoutTicks=200) public static void registeredRootTablesRollOnce(GameTestHelper h) {
        var params = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,h.absoluteVec(new net.minecraft.world.phys.Vec3(2,2,2)))
                .create(LootContextParamSets.CHEST);
        for (String id : List.of("omnira:chests/apprentice_hut", "omnira:chests/topological_labyrinth", "minecraft:chests/stronghold_library")) {
            var table = h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse(id)));
            h.assertTrue(ResourceLocation.parse(id).equals(table.getLootTableId()), "Config replacement lost table identity: "+id);
            int hits=0;
            for (int i=1;i<=2000;i++) {
                var loot=table.getRandomItems(params,i*17239L);
                long count=loot.stream().filter(s -> s.is(ModItems.LOW_TIER_MAGIC_CRYSTAL)).count();
                h.assertTrue(count<=1, "Nested table rolled twice: "+id);
                if(count==1)hits++;
            }
            h.assertTrue(hits>=65 && hits<=135, "Expected 5% root-table rolls, got "+hits+"/2000: "+id);
        }
        h.succeed();
    }
}
