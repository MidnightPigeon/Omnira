package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_integration_loading")
@PrefixGameTestTemplate(false)
public final class OptionalIntegrationGameTests {
    @GameTest(template="spell_arena")
    public static void kineticFactoriesFollowInstalledMods(GameTestHelper h) {
        boolean create=ModList.get().isLoaded("create");
        for(String prefix:new String[]{"crystal","infused_crystal"})
            for(String part:new String[]{"shaft","cogwheel","large_cogwheel","fluid_pipe"})
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("omnira:"+prefix+"_encased_"+part))==create,
                        "Optional component registration disagrees with Create presence");
        for(var block:java.util.List.of(ModBlocks.MANA_ENGINE.get(),ModBlocks.SPACETIME_PORT.get(),ModBlocks.REACTION_POOL.get(),com.mcmagic.omnira.engine.SpacetimeEngineContent.BLOCK.get()))
            h.assertTrue(hasKineticBase(block.getClass())==create,"Wrong block factory: "+block);
        h.succeed();
    }
    private static boolean hasKineticBase(Class<?> type) {
        for(Class<?> current=type;current!=null;current=current.getSuperclass())
            if(current.getName().startsWith("com.simibubi.create.")
                    || java.util.Arrays.stream(current.getInterfaces()).anyMatch(i->i.getName().equals("com.simibubi.create.content.kinetics.base.IRotate")))return true;
        return false;
    }
}
