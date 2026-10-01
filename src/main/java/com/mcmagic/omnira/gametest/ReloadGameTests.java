package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("omnira_reload")
@PrefixGameTestTemplate(false)
public final class ReloadGameTests {
    private static void assertResources(GameTestHelper h) {
        var slots=CuriosApi.getPlayerSlots(h.getLevel());
        h.assertTrue(slots.containsKey("crystal_grid") && slots.containsKey("spell_core"),"Player slots missing after reload");
        h.assertTrue(new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get()).is(TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("curios","crystal_grid"))),"Grid tag missing");
        h.assertTrue(new ItemStack(ModItems.TEST_SPELL_CORE.get()).is(TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("curios","spell_core"))),"Core tag missing");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira","assembly/spell_core_analysis")).isPresent(),"Core recipe missing");
    }
    @GameTest(template="spell_arena",timeoutTicks=1200)
    public static void consecutiveResourceReloads(GameTestHelper h) {
        assertResources(h);
        reload(h,2);
    }
    private static void reload(GameTestHelper h,int remaining) {
        var server=h.getLevel().getServer();
        var future=server.reloadResources(java.util.List.copyOf(server.getPackRepository().getSelectedIds()));
        poll(h,future,remaining);
    }
    private static void poll(GameTestHelper h,java.util.concurrent.CompletableFuture<Void> future,int remaining) {
        h.runAfterDelay(1,()->{
            if(!future.isDone()) {poll(h,future,remaining);return;}
            h.assertTrue(!future.isCompletedExceptionally(),"Resource reload failed");
            assertResources(h);
            if(remaining>1) reload(h,remaining-1); else h.succeed();
        });
    }
}
