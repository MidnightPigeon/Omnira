package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_light_spirit")
@PrefixGameTestTemplate(false)
public final class LightSpiritGameTests {
    @GameTest(template="spell_arena")
    public static void registrationAndLoot(GameTestHelper h) {
        var spirit = ModEntityTypes.LIGHT_SPIRIT.get().create(h.getLevel());
        h.assertTrue(spirit != null && spirit.getMaxHealth() == 8, "Missing spirit attributes");
        h.assertTrue(!spirit.canPickUpLoot() && spirit.isNoGravity(), "Spirit pickup or flight flags incorrect");
        var key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("omnira", "entities/light_spirit"));
        var table = h.getLevel().getServer().reloadableRegistries().getLootTable(key);
        var params = new LootParams.Builder(h.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, spirit)
                .withParameter(LootContextParams.ORIGIN, spirit.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, h.getLevel().damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        int cores = 0;
        for (int i = 0; i < 1000; i++) {
            var drops = table.getRandomItems(params);
            h.assertTrue(drops.stream().anyMatch(s -> s.is(ModItems.ARCANE_DUST.get()) && s.getCount() >= 1 && s.getCount() <= 3), "Missing arcane dust");
            h.assertTrue(drops.stream().anyMatch(s -> s.is(DreamContent.LIGHT_CONDENSATE.get().asItem())), "Missing light condensate");
            if (drops.stream().anyMatch(s -> s.is(DreamContent.LIGHT_CRYSTAL_CORE.get().asItem()))) cores++;
        }
        h.assertTrue(cores > 15 && cores < 100, "Core probability outside expected range: " + cores);
        spirit.discard();
        h.succeed();
    }
}
