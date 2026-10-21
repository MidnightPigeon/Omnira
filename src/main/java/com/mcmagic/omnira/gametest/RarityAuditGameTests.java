package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.alchemy.AlchemyMaterials;
import com.mcmagic.omnira.myriad.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_rarity")
@PrefixGameTestTemplate(false)
public final class RarityAuditGameTests {
    @GameTest(template="spell_arena") public static void confirmedRaritiesAndFullRegistryReport(GameTestHelper h) {
        h.assertTrue(new ItemStack(AlchemyMaterials.ENHANCED_CORE_ITEM.get()).getRarity()==Rarity.UNCOMMON,"Enhanced alchemy core must be yellow");
        h.assertTrue(new ItemStack(WorldSeedContent.DORMANT.get()).getRarity()==Rarity.UNCOMMON,"Dormant seed must be yellow");
        h.assertTrue(new ItemStack(WorldSeedContent.ACTIVE.get()).getRarity()==Rarity.RARE,"Active seed must be blue");
        h.assertTrue(new ItemStack(MiniatureWorldContent.ITEM.get()).getRarity()==Rarity.RARE,"World frame must be blue");
        for(var component:WorldComponent.values())h.assertTrue(new ItemStack(component.item.get()).getRarity()==Rarity.UNCOMMON,"Component rarity drift: "+component.id);
        for(var item:java.util.List.of(com.mcmagic.omnira.paradox.ParadoxContent.UNSTABLE_SOURCE.get(),com.mcmagic.omnira.registry.ModItems.UNSTABLE_SPACETIME_AGGREGATE.get(),
                com.mcmagic.omnira.paradox.ParadoxContent.GENESIS.get(),com.mcmagic.omnira.registry.ModItems.ADVANCED_RITUAL_ENERGY_CORE.get()))
            h.assertTrue(new ItemStack(item).getRarity()==Rarity.UNCOMMON,"Expected yellow rarity: "+BuiltInRegistries.ITEM.getKey(item));
        for(var item:java.util.List.of(com.mcmagic.omnira.paradox.ParadoxContent.STABLE_SOURCE.get(),com.mcmagic.omnira.paradox.ParadoxContent.STABLE_AGGREGATE.get()))
            h.assertTrue(new ItemStack(item).getRarity()==Rarity.RARE,"Stable encapsulation must be blue");
        var report=new JsonObject();
        BuiltInRegistries.ITEM.forEach(item->{
            var id=BuiltInRegistries.ITEM.getKey(item);
            if(id.getNamespace().equals("omnira"))report.addProperty(id.getPath(),new ItemStack(item).getRarity().name());
        });
        try {
            var path=java.nio.file.Path.of("../../build/rarity-audit.json").toAbsolutePath().normalize();
            java.nio.file.Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(java.io.IOException ex){h.fail("Cannot write rarity audit: "+ex.getMessage());return;}
        h.succeed();
    }
    private RarityAuditGameTests() {}
}
