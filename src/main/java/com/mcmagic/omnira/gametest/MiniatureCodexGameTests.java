package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.codex.MyriadCodexContent;
import com.mcmagic.omnira.myriad.*;
import com.mcmagic.omnira.myriad.MiniatureWorldBlockEntity.Stage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_miniature_codex")
@PrefixGameTestTemplate(false)
public final class MiniatureCodexGameTests {
    private static final ResourceLocation SERIES = ResourceLocation.parse("omnira:myriad_codex");
    private static final String[][] INPUTS = {
            {"active_world_seed", "world_generation_probability_cloud", "omni_microcore"},
            {"pure_mirror", "genesis_particles"}, {"boundary_crystal_membrane", "space_microcore"},
            {"ages_dial", "time_microcore"}, {"living_embryoplasm", "phantasm_germinal_shard"}, {"guide_book", "crystal_pedestal", "mana_engine"}
    };
    private static final Stage[] STAGES = {Stage.CODEX_WORLD, Stage.CODEX_MATTER, Stage.CODEX_FORM,
            Stage.CODEX_TIME, Stage.CODEX_LIFE, Stage.CODEX_COMPLETE};

    @GameTest(template="spell_arena") public static void sixStepsAppearInSharedHintsAndJeiData(GameTestHelper h) {
        var families = WorldDevelopmentSeries.collect(h.getLevel().getRecipeManager().getAllRecipesFor(MiniatureWorldContent.TYPE.get()));
        var series = families.stream().filter(s -> s.id().equals(SERIES)).findFirst().orElseThrow();
        h.assertTrue(series.recipes().size() == 6 && series.steps().size() == 6, "Codex must have exactly six steps");
        h.assertTrue(families.stream().filter(s -> !s.at(Stage.EMPTY).isEmpty()).count() <= WorldDevelopmentSeries.PANEL_COUNT,
                "Codex is outside initial hint panel capacity");
        var initial = families.stream().filter(s -> !s.at(Stage.EMPTY).isEmpty()).toList();
        h.assertTrue(initial.size() == 3 && initial.get(0).id().equals(WorldDevelopmentRecipe.MYRIAD)
                && initial.get(1).id().equals(ResourceLocation.parse("omnira:world_probability"))
                && initial.get(2).id().equals(SERIES), "Established hint panel order changed");
        for (int step = 0; step < STAGES.length; step++) {
            var row = series.steps().get(step);
            h.assertTrue(row.from() == (step == 0 ? Stage.EMPTY : STAGES[step - 1]) && row.to() == STAGES[step], "Broken codex chain");
            h.assertTrue(row.alternatives().size() == 1 && row.materials(0).size() == INPUTS[step].length, "Wrong ingredient layout");
            var material = row.materials(0);
            for (int i = 0; i < material.size(); i++) {
                var stack = item(INPUTS[step][i]);
                h.assertTrue(!stack.isEmpty() && material.get(i).test(stack) && material.get(i).count() == 1 && !material.get(i).fullStack(), "Wrong codex ingredient/count");
                h.assertTrue(material.get(i).examples().stream().allMatch(s -> s.getCount() == 1), "Hint/JEI stack count disagrees");
            }
        }
        h.assertTrue(Stage.PROBABILITY_COMPLETE.ordinal() == 22 && Stage.CODEX_WORLD.ordinal() == 23, "Existing stage ordinals changed");
        h.assertTrue(Stage.CODEX_COMPLETE.product().is(MyriadCodexContent.ITEM.get()), "Wrong JEI product");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void survivalCycleReloadsInFlightAndExtractsOnce(GameTestHelper h) {
        var base = h.absolutePos(new BlockPos(4,3,4));
        h.assertTrue(MiniatureWorldBlock.place(h.getLevel(), base), "Cannot place frame");
        var be = MiniatureWorldBlock.controller(h.getLevel(), base);
        h.assertTrue(be.initialSeries().stream().anyMatch(s -> s.id().equals(SERIES)), "Missing initial codex hint");
        for (int step = 0; step < STAGES.length; step++) {
            for(int i=0;i<INPUTS[step].length;i++){
                var stack=item(INPUTS[step][i]);
                if(INPUTS[step][i].equals("guide_book")){
                    var identity=com.mcmagic.omnira.guidance.GuideBookmark.identity(stack);
                    var bookmark=new com.mcmagic.omnira.guidance.GuideBookmark("myriad_codex",4);
                    h.assertTrue(bookmark.save(stack,identity), "Cannot save real guide bookmark");
                    stack=ItemStack.parse(h.getLevel().registryAccess(),stack.save(h.getLevel().registryAccess())).orElseThrow();
                    h.assertTrue(com.mcmagic.omnira.guidance.GuideBookmark.identity(stack).equals(identity)
                            &&com.mcmagic.omnira.guidance.GuideBookmark.read(stack).equals(bookmark), "Guide identity/bookmark did not serialize");
                }
                h.assertTrue(be.insert(stack)==1,"Codex ingredient rejected: "+INPUTS[step][i]);
                if(i+1<INPUTS[step].length){tick(be,1);h.assertTrue(!be.developing(),"Incomplete inputs started evolution");}
            }
            tick(be, 1);
            h.assertTrue(be.stage() == STAGES[step] && be.developing() && be.series().equals(SERIES), "Wrong evolution stage or series");
            h.assertTrue(be.size() == 0 && be.offerings().size() == INPUTS[step].length, "Ingredients not consumed exactly once");
            h.assertTrue(be.inventory.extractItem(0,1,false).isEmpty(), "Product escaped before authorship finished");
            h.assertTrue(be.takeProduct().isEmpty(), "Manual extraction skipped authorship animation");
            tick(be, be.remaining() / 2);
            int remaining = be.remaining();
            Stage previous = be.previousStage();
            be.loadWithComponents(be.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            be.resumeClock();
            h.assertTrue(be.stage() == STAGES[step] && be.previousStage() == previous && be.remaining() == remaining && be.series().equals(SERIES), "In-flight save lost continuity");
            tick(be, be.remaining());
            h.assertTrue(!be.developing() && be.stage() == STAGES[step], "Unexpected collapse/completion stage");
        }
        h.assertTrue(be.inventory.extractItem(0,1,true).is(MyriadCodexContent.ITEM.get()) && be.stage() == Stage.CODEX_COMPLETE, "Simulated extraction mutated result");
        h.assertTrue(be.inventory.extractItem(0,1,false).is(MyriadCodexContent.ITEM.get()), "Missing codex block item");
        h.assertTrue(be.stage() == Stage.EMPTY && be.takeProduct().isEmpty(), "Duplicate output or frame not reset");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void usedGuideIsSafeButFilledRecipeContainersRemainRejected(GameTestHelper h) {
        var book=item("guide_book");
        var id=com.mcmagic.omnira.guidance.GuideBookmark.identity(book);
        h.assertTrue(new com.mcmagic.omnira.guidance.GuideBookmark("myriad_codex",4).save(book,id), "Bookmark save failed");
        var finalRecipe=(WorldDevelopmentRecipe)h.getLevel().getRecipeManager().byKey(
                ResourceLocation.parse("omnira:world_development/codex/06_authorship")).orElseThrow().value();
        h.assertTrue(book.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
                &&finalRecipe.allocation(java.util.List.of(book,item("crystal_pedestal"),item("mana_engine")))!=null,
                "Guide custom data incorrectly treated as a filled container");
        h.assertTrue(finalRecipe.allocation(java.util.List.of(book))==null
                &&finalRecipe.allocation(java.util.List.of(book,item("crystal_pedestal")))==null
                &&finalRecipe.allocation(java.util.List.of(book,item("mana_engine")))==null,"Authorship accepted missing base or engine");
        var city=(WorldDevelopmentRecipe)h.getLevel().getRecipeManager().byKey(
                ResourceLocation.parse("omnira:world_development/10_city")).orElseThrow().value();
        var box=new ItemStack(net.minecraft.world.item.Items.BLUE_SHULKER_BOX);
        var purpur=new ItemStack(net.minecraft.world.item.Items.PURPUR_BLOCK,16);
        var crystal=new ItemStack(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),4);
        h.assertTrue(city.allocation(java.util.List.of(purpur,box,crystal))!=null, "Empty recipe container rejected");
        box.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(
                java.util.List.of(new ItemStack(net.minecraft.world.item.Items.DIAMOND,7))));
        h.assertTrue(!com.mcmagic.omnira.recipe.RecipeInputSafety.empty(box)
                &&city.allocation(java.util.List.of(purpur,box,crystal))==null, "Filled recipe container bypassed safety");
        h.assertTrue(box.get(net.minecraft.core.component.DataComponents.CONTAINER).stream().mapToInt(ItemStack::getCount).sum()==7,
                "Recipe safety mutated stored contents");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void authoredWorldStaysBoundedAndPedestalFollowsPages(GameTestHelper h){
        for(int i=0;i<=500;i++){
            var boxes=com.mcmagic.omnira.client.MiniatureCodexScene.boxes(i/100F,80);
            h.assertTrue(boxes.size()<400,"Scene detail exceeded bounded geometry budget");
            var names=new java.util.HashSet<String>();
            for(var b:boxes){
                h.assertTrue(names.add(b.id()),"Duplicate model component");
                h.assertTrue(b.width()>0&&b.height()>0&&b.depth()>0&&Float.isFinite(b.y()),"Invalid scene geometry");
                h.assertTrue(b.x()-b.width()/2>.3&&b.x()+b.width()/2<4.7&&b.z()-b.depth()/2>.3&&b.z()+b.depth()/2<4.7&&b.y()-b.height()/2>.3&&b.y()+b.height()/2<4,"Scene escaped framework");
            }
            h.assertTrue(names.contains("ground_2_2")&&names.contains("house_door")&&names.contains("hill_2"),"World anchors were replaced");
            h.assertTrue((i>448)==names.contains("pedestal_foot"),"Pedestal appeared before pages or failed to appear");
        }
        var end=com.mcmagic.omnira.client.MiniatureCodexScene.boxes(4.9F,80);
        var cover=end.stream().filter(b->b.id().equals("page_cover_1")).findFirst().orElseThrow();
        var cap=end.stream().filter(b->b.id().equals("pedestal_cap")).findFirst().orElseThrow();
        h.assertTrue(Math.abs(cover.y()-cover.height()/2-cap.y()-cap.height()/2)<.001,"Pedestal is not supporting the book");
        h.assertTrue(cover.y()>1.5&&cap.glass(),"Book was not lifted by a crystal pedestal");
        h.succeed();
    }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("omnira", path))); }
    private static void tick(MiniatureWorldBlockEntity be, int count) {
        for (int i = 0; i < count; i++) MiniatureWorldBlockEntity.tick(be.getLevel(), be.getBlockPos(), be.getBlockState(), be);
    }
    private MiniatureCodexGameTests() {}
}
