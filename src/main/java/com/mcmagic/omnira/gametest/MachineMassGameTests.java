package com.mcmagic.omnira.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;

@GameTestHolder("omnira_machine_mass")
@PrefixGameTestTemplate(false)
public final class MachineMassGameTests {
    @GameTest(template="spell_arena") public static void weightlessMaterialsAndMachines(GameTestHelper h){
        h.assertTrue(net.neoforged.fml.ModList.get().isLoaded("sable"),"Run mass tests with Sable installed");
        var pos=h.absolutePos(new BlockPos(4,3,4));
        String[] ids={"spiritual_crystal_block","spiritual_crystal_stairs","spiritual_crystal_slab",
            "dream_crystal_block","dream_crystal_stairs","dream_crystal_slab",
            "spatial_crystal_block","spatial_crystal_stairs","spatial_crystal_slab",
            "crystal_processing_table","analysis_artisan_table","arcane_assembly_table","advanced_assembly_table",
            "advanced_crystal_aggregation_ring","hyperdimensional_drill","spacetime_reverser","void_infuser","split_time_sundial",
            "mana_engine","spacetime_engine","crystal_ball","liquid_crystal_ball","marisa_crystal_ball","resonance_core","wandering_star_cluster_orb",
            "crystal_pedestal","ritual_energy_core","advanced_ritual_energy_core",
            "rough_alchemy_cauldron","primitive_reaction_cauldron","large_alchemical_reaction_pool",
            "alchemical_catalysis_chamber","rough_alchemical_energy_core","enhanced_alchemical_energy_core",
            "advanced_condensation_table","pure_vessel"};
        var all=new java.util.LinkedHashSet<>(java.util.List.of(ids));
        for(var key:BuiltInRegistries.BLOCK.keySet())if(key.getNamespace().equals("omnira")){
            var name=key.getPath();
            if(name.contains("crystal") || name.endsWith("_bud") || name.startsWith("light_source_")
                    || name.startsWith("light_condensate"))all.add(name);
        }
        int states=0;
        for(String id:all){
            var key=ResourceLocation.fromNamespaceAndPath("omnira",id);
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(key),"Missing block "+id);
            for(var state:BuiltInRegistries.BLOCK.get(key).getStateDefinition().getPossibleStates()){
                h.assertTrue(PhysicsBlockPropertyHelper.getMass(h.getLevel(),pos,state)==0,"Nonzero mass: "+state);
                states++;
            }
        }
        com.mcmagic.omnira.Omnira.LOGGER.info("Verified zero Sable mass for {} blocks / {} states",all.size(),states);
        var simple=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("omnira","simple_condensation_table")).defaultBlockState();
        h.assertTrue(PhysicsBlockPropertyHelper.getMass(h.getLevel(),pos,simple)==.5,"Simple table mass changed");
        var advanced=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("omnira","advanced_condensation_table")).defaultBlockState();
        var floating=PhysicsBlockPropertyHelper.getFloatingMaterial(advanced);
        h.assertTrue(floating!=null&&floating.liftStrength()>0,"Advanced table lacks buoyancy");
        h.succeed();
    }
}
