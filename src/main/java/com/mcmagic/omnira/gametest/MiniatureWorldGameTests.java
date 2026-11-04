package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.myriad.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_miniature_world")
@PrefixGameTestTemplate(false)
public final class MiniatureWorldGameTests {
    @GameTest(template="spell_arena") public static void bastionAcceptsBlackstoneBlocksAndStairsOnly(GameTestHelper h){
        var recipe=(WorldDevelopmentRecipe)h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("omnira:world_development/06_bastion")).orElseThrow().value();
        var material=recipe.materials().get(2);
        for(var item:new Item[]{Items.BLACKSTONE,Items.POLISHED_BLACKSTONE,Items.POLISHED_BLACKSTONE_BRICKS,Items.CHISELED_POLISHED_BLACKSTONE,
                Items.CRACKED_POLISHED_BLACKSTONE_BRICKS,Items.GILDED_BLACKSTONE,Items.BLACKSTONE_STAIRS,Items.POLISHED_BLACKSTONE_STAIRS,Items.POLISHED_BLACKSTONE_BRICK_STAIRS})
            h.assertTrue(material.test(new ItemStack(item)),"Rejected valid blackstone variant or stairs: "+item);
        for(var item:new Item[]{Items.BLACKSTONE_SLAB,Items.POLISHED_BLACKSTONE_SLAB,Items.POLISHED_BLACKSTONE_BRICK_SLAB,Items.BLACKSTONE_WALL,
                Items.POLISHED_BLACKSTONE_WALL,Items.POLISHED_BLACKSTONE_BRICK_WALL,Items.POLISHED_BLACKSTONE_BUTTON,Items.POLISHED_BLACKSTONE_PRESSURE_PLATE})
            h.assertTrue(!material.test(new ItemStack(item)),"Accepted excluded blackstone shape/device: "+item);
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=60) public static void crystalShellLightsItsSurroundings(GameTestHelper h){
        var be=frame(h);var level=h.getLevel();var base=be.getBlockPos();
        h.assertTrue(level.getBlockState(base).getLightEmission()==12&&level.getBlockState(base.offset(4,4,4)).getLightEmission()==12,"Frame shell must emit light 12");
        h.assertTrue(level.getBlockState(base.offset(2,2,2)).getLightEmission()==0,"Invisible interior cell became a light source");
        h.runAfterDelay(20,()->{
            h.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,base.offset(-1,2,2))>=11,"Frame did not illuminate adjacent world blocks");h.succeed();
        });
    }
    private static void tick(MiniatureWorldBlockEntity be,int count){for(int i=0;i<count;i++)MiniatureWorldBlockEntity.tick(be.getLevel(),be.getBlockPos(),be.getBlockState(),be);}
    private static void advance(GameTestHelper h,MiniatureWorldBlockEntity be,MiniatureWorldBlockEntity.Stage target,ItemStack...inputs){
        for(var stack:inputs)h.assertTrue(be.insert(stack)==stack.getCount(),"Rejected stage ingredient "+stack);
        tick(be,1);h.assertTrue(be.stage()==target&&be.developing(),"Wrong next stage: "+be.stage()+" expected "+target);
        tick(be,be.remaining());h.assertTrue(!be.developing(),"Animation did not finish");
    }
    @GameTest(template="spell_arena") public static void completeNineSceneSurvivalCycle(GameTestHelper h){
        var be=frame(h);be.insert(new ItemStack(Items.DIAMOND,7));
        advance(h,be,MiniatureWorldBlockEntity.Stage.OVERWORLD,new ItemStack(Items.OAK_LOG,64),new ItemStack(Items.DIRT,64),new ItemStack(com.mcmagic.omnira.registry.ModItems.PHILOSOPHERS_STONE.get()));
        h.assertTrue(be.material(0).is(Items.DIAMOND),"Same-dimension scene erased unrelated material");
        advance(h,be,MiniatureWorldBlockEntity.Stage.RUINS,new ItemStack(Items.COMPASS),new ItemStack(Items.OAK_BOAT),new ItemStack(Items.BREAD,64));
        advance(h,be,MiniatureWorldBlockEntity.Stage.PORTAL,new ItemStack(Items.WATER_BUCKET),new ItemStack(Items.DIAMOND_PICKAXE),new ItemStack(Items.FLINT_AND_STEEL));
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.PORTAL&&be.size()==1,"Portal auto-crossed or cleared prematurely");
        h.assertTrue(be.enterDimension()&&be.size()==0,"Manual Nether crossing did not clear materials");tick(be,60);
        advance(h,be,MiniatureWorldBlockEntity.Stage.NETHER,new ItemStack(Items.NETHERRACK,64),new ItemStack(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),16),new ItemStack(Items.PORKCHOP));
        advance(h,be,MiniatureWorldBlockEntity.Stage.FORTRESS,new ItemStack(Items.SOUL_SAND,16),new ItemStack(Items.LAVA_BUCKET),new ItemStack(Items.BLAZE_ROD,16));
        advance(h,be,MiniatureWorldBlockEntity.Stage.BASTION,new ItemStack(Items.CRIMSON_STEM,64),new ItemStack(Items.WARPED_STEM,64),new ItemStack(Items.POLISHED_BLACKSTONE_BRICKS,16),new ItemStack(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),32),new ItemStack(Items.NETHERITE_INGOT,4));
        be.insert(new ItemStack(Items.EMERALD));advance(h,be,MiniatureWorldBlockEntity.Stage.RIFT,new ItemStack(Items.ENDER_EYE,4));
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.END_EMPTY&&be.size()==0&&!be.enterDimension(),"End rift did not clear inventory/complete its crossing automatically");
        advance(h,be,MiniatureWorldBlockEntity.Stage.END,new ItemStack(Items.END_STONE,64),new ItemStack(Items.OBSIDIAN,32),new ItemStack(Items.END_CRYSTAL));
        advance(h,be,MiniatureWorldBlockEntity.Stage.CHORUS,new ItemStack(Items.CHORUS_FRUIT,16),new ItemStack(Items.CHORUS_FLOWER,4));
        advance(h,be,MiniatureWorldBlockEntity.Stage.CITY,new ItemStack(Items.PURPUR_STAIRS,16),new ItemStack(Items.BLUE_SHULKER_BOX),new ItemStack(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),4));
        be.insert(new ItemStack(Items.DIAMOND,64));be.insert(new ItemStack(Items.NETHER_STAR));tick(be,1);
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.VOID&&be.size()==0&&be.takeProduct().isEmpty(),"Collapse did not consume everything or produced early");
        tick(be,160);tick(be,300);h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.COMPLETE,"Final product did not persist");
        h.assertTrue(!be.takeProduct().isEmpty()&&be.takeProduct().isEmpty()&&be.stage()==MiniatureWorldBlockEntity.Stage.EMPTY,"Product duplication/reset error");h.succeed();
    }
    @GameTest(template="spell_arena") public static void alternativesHintsAndReload(GameTestHelper h){
        var be=frame(h);be.query(net.minecraft.core.Direction.NORTH);h.assertTrue(be.hint()==0,"Missing first hint");
        be.query(net.minecraft.core.Direction.NORTH,0);h.assertTrue(be.initialAlternative(0)==1,"Did not advance alternate initial recipe");
        be.query(net.minecraft.core.Direction.NORTH);h.assertTrue(be.hint()==-1,"Last hint did not hide");
        be.query(net.minecraft.core.Direction.SOUTH);tick(be,600);h.assertTrue(be.hint()==0,"Initial panels must persist until dismissed");
        be.query(net.minecraft.core.Direction.SOUTH,0);be.query(net.minecraft.core.Direction.SOUTH,0);h.assertTrue(be.initialAlternative(0)==0,"Initial alternatives must loop without hiding");
        be.query(net.minecraft.core.Direction.SOUTH);h.assertTrue(be.hint()==-1,"Blank click must dismiss initial panels");
        be.insert(new ItemStack(com.mcmagic.omnira.registry.ModItems.ANALYSIS_CRYSTAL.get()));be.insert(new ItemStack(com.mcmagic.omnira.paradox.ParadoxContent.GENESIS.get()));tick(be,1);
        h.assertTrue(!be.developing(),"Genesis route started without its Philosopher's Stone");
        be.insert(new ItemStack(com.mcmagic.omnira.registry.ModItems.PHILOSOPHERS_STONE.get()));tick(be,26);
        int remaining=be.remaining();var saved=be.packed();be.loadWithComponents(saved.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(),h.getLevel().registryAccess());
        h.assertTrue(be.remaining()==remaining&&be.developing()&&be.insert(new ItemStack(Items.STONE))==0,"Animation reload/uninterruptible input error");tick(be,remaining);
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.OVERWORLD,"Alternative did not develop");
        h.assertTrue(be.series().equals(WorldDevelopmentRecipe.MYRIAD),"Active evolution series was not preserved");
        be.preview(MiniatureWorldBlockEntity.Stage.END_EMPTY);
        advance(h,be,MiniatureWorldBlockEntity.Stage.END,new ItemStack(com.mcmagic.omnira.registry.ModItems.LIGHT_MICROCORE.get()),new ItemStack(com.mcmagic.omnira.registry.ModItems.DARK_MICROCORE.get()),new ItemStack(com.mcmagic.omnira.registry.ModItems.DREAM_CRYSTAL_SHARD.get()));
        be.preview(MiniatureWorldBlockEntity.Stage.CITY);be.insert(new ItemStack(com.mcmagic.omnira.paradox.ParadoxContent.STABLE_AGGREGATE.get()));tick(be,161);
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.COMPLETE,"Aggregate collapse alternative failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void oldWaitingEndRiftResumesWithoutMoreMaterials(GameTestHelper h){
        var be=frame(h);be.preview(MiniatureWorldBlockEntity.Stage.RIFT);
        var tag=be.packed().get(DataComponents.BLOCK_ENTITY_DATA).copyTag();be.loadWithComponents(tag,h.getLevel().registryAccess());
        tick(be,1);
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.END_EMPTY&&!be.developing()&&!be.enterDimension(),"Old completed rift did not resume at the empty End");
        h.assertTrue(be.size()==0,"Old rift migration fabricated materials");h.succeed();
    }
    @GameTest(template="spell_arena") public static void groupedRecipesAndPanelPicking(GameTestHelper h){
        var be=frame(h);var all=h.getLevel().getRecipeManager().getAllRecipesFor(MiniatureWorldContent.TYPE.get());
        var groups=WorldDevelopmentSeries.collect(all);
        h.assertTrue(groups.size()==3&&groups.stream().mapToInt(g->g.recipes().size()).sum()==all.size()&&groups.getFirst().recipes().size()==14,"Evolution series were lost or split into stage recipes");
        h.assertTrue(groups.stream().anyMatch(g->g.id().toString().equals("omnira:myriad_codex")&&g.recipes().size()==6),"Codex evolution was lost");
        var probability=groups.get(1);
        h.assertTrue(probability.recipes().size()==6&&probability.at(MiniatureWorldBlockEntity.Stage.EMPTY).size()==1&&probability.steps().size()==7&&probability.steps().stream().filter(s->s.alternatives().isEmpty()).count()==1,"Probability series did not group its six scenes and one manual crossing");
        var steps=groups.getFirst().steps();
        h.assertTrue(steps.size()==12&&steps.stream().filter(s->s.alternatives().isEmpty()).count()==1,"End rift and empty End were not merged");
        var crossing=steps.stream().filter(s->s.from()==MiniatureWorldBlockEntity.Stage.BASTION).findFirst().orElseThrow();
        h.assertTrue(crossing.to()==MiniatureWorldBlockEntity.Stage.END_EMPTY&&crossing.materials(0).size()==1,"End crossing contains unrelated stage inputs");
        var eyes=crossing.materials(0).getFirst().examples();
        h.assertTrue(eyes.size()==1&&eyes.getFirst().is(Items.ENDER_EYE)&&eyes.getFirst().getCount()==4,"End crossing display must require exactly four Ender Eyes");
        var selected=groups.getFirst().selectInputs(java.util.List.of(new ItemStack(Items.ENDER_EYE)));
        h.assertTrue(steps.get(selected.step()).equals(crossing),"Uses lookup did not select the Ender Eye stage");
        h.assertTrue(selected.alternative()==0&&groups.getFirst().selectInputs(java.util.List.of()).equals(new WorldDevelopmentSeries.Selection(0,0)),"Default/alternate selection is inconsistent");
        var alternate=groups.getFirst().selectInputs(java.util.List.of(new ItemStack(com.mcmagic.omnira.registry.ModItems.LIGHT_MICROCORE.get())));
        h.assertTrue(steps.get(alternate.step()).from()==MiniatureWorldBlockEntity.Stage.END_EMPTY&&alternate.alternative()==1,"Uses lookup did not select the alternative End-island inputs");
        h.assertTrue(steps.stream().filter(s->s.alternatives().size()==2).count()==3,"JEI alternatives were split into separate stages");
        h.assertTrue(groups.getFirst().at(MiniatureWorldBlockEntity.Stage.EMPTY).size()==2&&groups.getFirst().at(MiniatureWorldBlockEntity.Stage.END_EMPTY).size()==2&&groups.getFirst().at(MiniatureWorldBlockEntity.Stage.CITY).size()==2,"Lost an alternative route");
        for(var face:new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.WEST}){
            for(int panel=0;panel<3;panel++)h.assertTrue(WorldDevelopmentSeries.panelAt(face,new net.minecraft.world.phys.Vec3(2.5,WorldDevelopmentSeries.panelY(panel),2.5))==panel,"Face/panel coordinates disagree");
            h.assertTrue(WorldDevelopmentSeries.panelAt(face,new net.minecraft.world.phys.Vec3(2.5,3.2,2.5))==-1,"Gap was treated as a panel");
            var edge=switch(face){case NORTH,SOUTH->new net.minecraft.world.phys.Vec3(.1,3.85,2.5);default->new net.minecraft.world.phys.Vec3(2.5,3.85,.1);};
            h.assertTrue(WorldDevelopmentSeries.panelAt(face,edge)==-1,"Blank edge was treated as a panel");
        }
        be.query(net.minecraft.core.Direction.NORTH);be.query(net.minecraft.core.Direction.NORTH,0);
        var tag=be.packed().get(DataComponents.BLOCK_ENTITY_DATA).copyTag();be.loadWithComponents(tag,h.getLevel().registryAccess());tick(be,700);
        h.assertTrue(be.hint()==0&&be.initialAlternative(0)==1,"Panel selection or persistence was lost on reload");
        be.query(net.minecraft.core.Direction.NORTH,1);h.assertTrue(be.hint()==0&&be.initialAlternative(1)==0,"Single-route second series should stay visible");
        be.query(net.minecraft.core.Direction.NORTH,2);h.assertTrue(be.hint()==0&&be.initialAlternative(2)==0,"Codex route must keep the initial panels visible");
        be.preview(MiniatureWorldBlockEntity.Stage.END_EMPTY);be.query(net.minecraft.core.Direction.NORTH);tick(be,300);
        h.assertTrue(be.hint()==1,"Later-stage timed alternatives changed");tick(be,300);h.assertTrue(be.hint()==-1,"Later-stage hint no longer expires");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void probabilityCloudSurvivalCycleAndPersistence(GameTestHelper h){
        var be=frame(h);
        var cores=new Item[]{com.mcmagic.omnira.registry.ModItems.EARTH_MICROCORE.get(),com.mcmagic.omnira.registry.ModItems.WATER_MICROCORE.get(),com.mcmagic.omnira.registry.ModItems.FIRE_MICROCORE.get(),com.mcmagic.omnira.registry.ModItems.AIR_MICROCORE.get(),com.mcmagic.omnira.registry.ModItems.LIGHT_MICROCORE.get(),com.mcmagic.omnira.registry.ModItems.DARK_MICROCORE.get(),com.mcmagic.omnira.registry.ModItems.TIME_MICROCORE.get(),com.mcmagic.omnira.registry.ModItems.SPACE_MICROCORE.get(),com.mcmagic.omnira.registry.ModItems.OMNI_MICROCORE.get()};
        for(int i=0;i<8;i++)be.insert(new ItemStack(cores[i]));tick(be,1);
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.EMPTY,"Eight cores incorrectly started the nine-core recipe");
        advance(h,be,MiniatureWorldBlockEntity.Stage.WORKSHOP,new ItemStack(cores[8]));
        h.assertTrue(be.series().toString().equals("omnira:world_probability")&&be.size()==0,"Wrong series or unconsumed nine cores");
        be.insert(new ItemStack(Items.DIAMOND));
        advance(h,be,MiniatureWorldBlockEntity.Stage.DREAM_PORTAL,new ItemStack(com.mcmagic.omnira.registry.ModItems.CRYSTAL_PEDESTAL.get(),4),new ItemStack(Items.BLUE_BED));
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.DREAM_PORTAL&&be.size()==1,"Dream portal crossed early");
        h.assertTrue(be.enterDimension()&&be.size()==0,"Dream crossing did not clear remaining materials");tick(be,60);
        advance(h,be,MiniatureWorldBlockEntity.Stage.DREAM_LAYERS,new ItemStack(com.mcmagic.omnira.registry.ModItems.CRUDE_LIGHT_CORE.get(),4),new ItemStack(com.mcmagic.omnira.registry.ModItems.SHADOW_MANUSCRIPT.get(),4),new ItemStack(com.mcmagic.omnira.registry.ModItems.PURE_MIRROR.get()));
        be.insert(new ItemStack(Items.EMERALD));
        advance(h,be,MiniatureWorldBlockEntity.Stage.SPACETIME_EMPTY,new ItemStack(cores[6]),new ItemStack(cores[7]),new ItemStack(com.mcmagic.omnira.registry.ModItems.ADVANCED_RITUAL_ENERGY_CORE.get()),new ItemStack(com.mcmagic.omnira.registry.ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()));
        h.assertTrue(be.size()==0&&!be.enterDimension(),"Spacetime crossing must clear and complete automatically");
        var silt=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("omnira:sedimented_living_temporal_silt"));
        var crystal=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("omnira:excited_rough_spatial_crystal_block"));
        advance(h,be,MiniatureWorldBlockEntity.Stage.SPACETIME_LAYERS,new ItemStack(silt,16),new ItemStack(crystal,16),new ItemStack(com.mcmagic.omnira.registry.ModItems.PURE_MIRROR.get(),4));
        be.insert(new ItemStack(Items.DIAMOND));
        be.insert(new ItemStack(com.mcmagic.omnira.paradox.ParadoxContent.STABLE_AGGREGATE.get()));be.insert(new ItemStack(WorldSeedContent.ACTIVE.get()));
        be.insert(new ItemStack(com.mcmagic.omnira.registry.ModItems.SPACETIME_SPELL_CORE.get()));be.insert(new ItemStack(com.mcmagic.omnira.paradox.ParadoxContent.GENESIS.get(),4));tick(be,1);
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.PROBABILITY_EXPLOSION&&be.size()==0&&be.takeProduct().isEmpty(),"Collapse exposed output early or kept leftover materials");
        tick(be,90);var saved=be.packed();be.loadWithComponents(saved.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(),h.getLevel().registryAccess());tick(be,be.remaining());
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.PROBABILITY_COMPLETE&&be.inventory.extractItem(0,1,true).is(MiniatureWorldContent.PROBABILITY_CLOUD.get()),"Final product or reload failed");
        tick(be,600);h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.PROBABILITY_COMPLETE,"Uncollected cloud reset the frame");
        h.assertTrue(be.takeProduct().is(MiniatureWorldContent.PROBABILITY_CLOUD.get())&&be.takeProduct().isEmpty()&&be.stage()==MiniatureWorldBlockEntity.Stage.EMPTY,"Cloud extraction duplicated or did not reset");h.succeed();
    }
    @GameTest(template="spell_arena") public static void seriesCodecAndIsolation(GameTestHelper h){
        var level=h.getLevel();var ops=level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var original=(WorldDevelopmentRecipe)level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("omnira:world_development/01_forest")).orElseThrow().value();
        var custom=net.minecraft.resources.ResourceLocation.parse("example:another_world");
        var recipe=new WorldDevelopmentRecipe(original.from(),original.to(),original.materials(),original.duration(),custom);
        var encoded=(net.minecraft.nbt.CompoundTag)WorldDevelopmentRecipe.CODEC.codec().encodeStart(ops,recipe).getOrThrow();
        h.assertTrue(WorldDevelopmentRecipe.CODEC.codec().parse(ops,encoded).getOrThrow().series().equals(custom),"Series codec did not round-trip");
        encoded.remove("series");h.assertTrue(WorldDevelopmentRecipe.CODEC.codec().parse(ops,encoded).getOrThrow().series().equals(WorldDevelopmentRecipe.MYRIAD),"Legacy recipes lost their default series");
        var groups=WorldDevelopmentSeries.collect(java.util.List.of(new net.minecraft.world.item.crafting.RecipeHolder<>(custom,recipe),new net.minecraft.world.item.crafting.RecipeHolder<>(WorldDevelopmentRecipe.MYRIAD,original)));
        h.assertTrue(groups.size()==2&&groups.stream().allMatch(g->g.at(MiniatureWorldBlockEntity.Stage.EMPTY).size()==1),"Different evolutions merged their initial recipes");
        var be=frame(h);var tag=be.packed().get(DataComponents.BLOCK_ENTITY_DATA).copyTag();tag.putString("Stage","OVERWORLD");tag.putString("Series",custom.toString());
        be.loadWithComponents(tag,level.registryAccess());h.assertTrue(be.recipes().isEmpty(),"Frame accepted another series' intermediate recipes");
        tag.remove("Series");be.loadWithComponents(tag,level.registryAccess());h.assertTrue(!be.recipes().isEmpty(),"Legacy in-progress frame was stranded");h.succeed();
    }
    @GameTest(template="spell_arena") public static void capabilitiesSimulationAndResonance(GameTestHelper h){
        var be=frame(h);var level=h.getLevel();var top=be.getBlockPos().offset(2,4,2);
        var handler=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,top,net.minecraft.core.Direction.UP);
        h.assertTrue(handler!=null,"Top part has no container capability");
        h.assertTrue(handler.insertItem(0,new ItemStack(Items.DIAMOND,64),true).isEmpty()&&be.size()==0,"Simulation mutated inventory");
        handler.insertItem(0,new ItemStack(Items.DIAMOND,64),false);
        h.assertTrue(handler.insertItem(1,new ItemStack(Items.DIAMOND),false).getCount()==1,"Automation overfilled matching stack");
        h.assertTrue(handler.extractItem(0,12,true).getCount()==12&&be.material(0).getCount()==64,"Extraction simulation mutated state");
        h.assertTrue(handler.extractItem(0,12,false).getCount()==12&&be.material(0).getCount()==52,"Capability extraction incorrect");
        level.setBlockAndUpdate(top.above(),com.mcmagic.omnira.registry.ModBlocks.RESONANCE_CORE.get().defaultBlockState());
        var node=(com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity)level.getBlockEntity(top.above());
        h.assertTrue(node!=null,"Resonance node block entity missing");
        var linked=node.items();h.assertTrue(linked!=null,"Resonance node cannot see frame inventory");
        linked.insertItem(0,new ItemStack(Items.DIAMOND,3),false);
        h.assertTrue(be.material(0).getCount()==55,"Resonance capability targets a different inventory");
        be.preview(MiniatureWorldBlockEntity.Stage.COMPLETE);
        h.assertTrue(!handler.extractItem(0,1,true).isEmpty()&&be.stage()==MiniatureWorldBlockEntity.Stage.COMPLETE,"Simulated product reset frame");
        handler.extractItem(0,1,false);h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.EMPTY&&be.size()==0,"Automated product extraction did not reset");h.succeed();
    }
    @GameTest(template="spell_arena") public static void overlappingRequirementsNeverShareMaterials(GameTestHelper h){
        var ingredient=net.minecraft.world.item.crafting.Ingredient.of(Items.APPLE);
        var food=new WorldDevelopmentRecipe.Material(java.util.Optional.empty(),true,64);
        var apples=new WorldDevelopmentRecipe.Material(java.util.Optional.of(ingredient),false,1);
        var recipe=new WorldDevelopmentRecipe(MiniatureWorldBlockEntity.Stage.EMPTY,MiniatureWorldBlockEntity.Stage.OVERWORLD,java.util.List.of(food,apples),100);
        h.assertTrue(recipe.allocation(java.util.List.of(new ItemStack(Items.APPLE,64)))==null,"One item counted twice");
        var plan=recipe.allocation(java.util.List.of(new ItemStack(Items.APPLE),new ItemStack(Items.BREAD,64)));
        h.assertTrue(plan!=null&&plan[0]==1&&plan[1]==64,"Overlap allocation starved exact ingredient");h.succeed();
    }
    @GameTest(template="spell_arena") public static void foodUsesOneNativeStackAndExamplesMatch(GameTestHelper h){
        var recipe=(WorldDevelopmentRecipe)h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("omnira:world_development/02_ruins")).orElseThrow().value();
        var requirement=recipe.materials().get(2);
        for(var item:new Item[]{Items.BREAD,Items.HONEY_BOTTLE,Items.MUSHROOM_STEW}){
            var food=new ItemStack(item);int count=food.getMaxStackSize();
            var input=java.util.List.of(new ItemStack(Items.COMPASS),new ItemStack(Items.OAK_BOAT),food.copyWithCount(count));
            var plan=recipe.allocation(input);h.assertTrue(plan!=null&&plan[2]==count,"Wrong native food-stack requirement: "+item);
            h.assertTrue(recipe.allocation(java.util.List.of(input.get(0),input.get(1),food.copyWithCount(count-1)))==null,"Incomplete food stack accepted");
            h.assertTrue(requirement.examples().stream().anyMatch(s->s.is(item)&&s.getCount()==count),"JEI/hint food count disagrees");
        }
        h.assertTrue(recipe.allocation(java.util.List.of(new ItemStack(Items.COMPASS),new ItemStack(Items.OAK_BOAT),new ItemStack(Items.BREAD,32),new ItemStack(Items.APPLE,32)))==null,"Mixed half-stacks counted as one food stack");
        var full=new WorldDevelopmentRecipe.Material(java.util.Optional.empty(),true,64,true);
        var exact=new WorldDevelopmentRecipe.Material(java.util.Optional.of(net.minecraft.world.item.crafting.Ingredient.of(Items.APPLE)),false,1);
        var overlap=new WorldDevelopmentRecipe(MiniatureWorldBlockEntity.Stage.EMPTY,MiniatureWorldBlockEntity.Stage.OVERWORLD,java.util.List.of(full,exact),100);
        h.assertTrue(overlap.allocation(java.util.List.of(new ItemStack(Items.APPLE,64)))==null,"Full-stack and exact requirements shared an item");
        h.assertTrue(overlap.allocation(java.util.List.of(new ItemStack(Items.APPLE,64),new ItemStack(Items.BREAD,64)))!=null,"Food choice failed to leave exact ingredient available");h.succeed();
    }
    @GameTest(template="spell_arena") public static void loadedShulkerIsRetainedInsteadOfConsumed(GameTestHelper h){
        var be=frame(h);be.preview(MiniatureWorldBlockEntity.Stage.CHORUS);
        var box=new ItemStack(Items.BLUE_SHULKER_BOX);box.set(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.DIAMOND,7))));
        be.insert(new ItemStack(Items.PURPUR_BLOCK,16));be.insert(box);be.insert(new ItemStack(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),4));tick(be,1);
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.CHORUS&&!be.developing()&&be.size()==3,"Loaded shulker triggered destructive recipe");
        be.take();h.assertTrue(ItemStack.matches(be.take(),box),"Rejected shulker lost inventory data");
        h.assertTrue(be.insert(new ItemStack(Items.BLUE_SHULKER_BOX))==1,"Empty shulker rejected");
        be.insert(new ItemStack(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),4));tick(be,1);
        h.assertTrue(be.stage()==MiniatureWorldBlockEntity.Stage.CITY&&be.developing(),"Empty shulker could not create city");h.succeed();
    }
    @GameTest(template="spell_arena") public static void restrictedContainersCannotEnterFrameButLegacyContentsRemain(GameTestHelper h){
        var be=frame(h);var level=h.getLevel();var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var packed=be.packed();var container=new net.minecraft.world.SimpleContainer(9);
        h.assertTrue(!packed.canFitInsideContainerItems()&&!container.canPlaceItem(0,packed)
                &&new net.neoforged.neoforge.items.ItemStackHandler(1).insertItem(0,packed,false).getCount()==1,"Packed frame can nest through another container");
        h.assertTrue(new ItemStack(MiniatureWorldContent.ITEM.get()).canFitInsideContainerItems()
                &&container.canPlaceItem(0,new ItemStack(MiniatureWorldContent.ITEM.get())),"Unpacked frame cannot be stored");
        var bottle=new ItemStack(com.mcmagic.omnira.registry.ModItems.POCKET_MAGIC_BOTTLE.get());bottle.set(com.mcmagic.omnira.registry.ModDataComponents.BOTTLE_CAPTURE,java.util.UUID.randomUUID());
        var upgrades=new net.neoforged.neoforge.items.ItemStackHandler(3);upgrades.setStackInSlot(1,new ItemStack(com.mcmagic.omnira.registry.ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
        var data=new net.minecraft.nbt.CompoundTag();data.put("Upgrades",upgrades.serializeNBT(level.registryAccess()));
        data.putString("id",net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(com.mcmagic.omnira.registry.ModBlockEntityTypes.CRYSTAL_BALL.get()).toString());
        var orb=new ItemStack(com.mcmagic.omnira.registry.ModItems.CRYSTAL_BALL.get());orb.set(DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
        for(var stack:java.util.List.of(bottle,orb,new ItemStack(MiniatureWorldContent.ITEM.get()),be.packed())){
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack.copy());
            var hit=new net.minecraft.world.phys.BlockHitResult(be.getBlockPos().getCenter(),net.minecraft.core.Direction.NORTH,be.getBlockPos(),false);
            be.getBlockState().useItemOn(player.getMainHandItem(),level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(be.size()==0&&ItemStack.matches(player.getMainHandItem(),stack),"Manual insertion bypassed nesting rules");
            for(boolean simulate:new boolean[]{true,false})h.assertTrue(ItemStack.matches(be.inventory.insertItem(0,stack.copy(),simulate),stack)&&be.size()==0,"Capability insertion bypassed rules or simulation mutated storage");
        }
        var saved=be.saveWithoutMetadata(level.registryAccess());var materials=new net.minecraft.nbt.ListTag();materials.add(orb.save(level.registryAccess()));saved.put("Materials",materials);
        be.loadWithComponents(saved,level.registryAccess());h.assertTrue(ItemStack.matches(be.take(),orb),"Legacy restricted content was deleted or cannot be removed");
        h.assertTrue(be.insert(new ItemStack(Items.SHULKER_BOX))==1,"Empty recipe container blocked by nesting check");h.succeed();
    }
    @GameTest(template="spell_arena") public static void legacyFrameCanBePackedWithoutLeavingParts(GameTestHelper h){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(4,3,4));
        for(int i=0;i<343;i++)level.setBlock(base.offset(i%7,i/49,i/7%7),MiniatureWorldContent.BLOCK.get().defaultBlockState().setValue(MiniatureWorldBlock.PART,i),18);
        var be=MiniatureWorldBlock.controller(level,base);be.insert(new ItemStack(Items.DIAMOND,5));
        var corner=base.offset(6,6,6);h.assertTrue(MiniatureWorldBlock.controller(level,corner)==be,"Legacy offset decoding failed");
        level.destroyBlock(corner,true);
        for(int i=0;i<343;i++)h.assertTrue(!level.getBlockState(base.offset(i%7,i/49,i/7%7)).is(MiniatureWorldContent.BLOCK.get()),"Legacy frame left orphan parts");
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(10),e->e.getItem().is(MiniatureWorldContent.ITEM.get()));
        h.assertTrue(drops.size()==1,"Legacy removal did not produce exactly one frame");
        h.assertTrue(MiniatureWorldBlock.place(level,base),"Cannot place compact replacement");var replacement=MiniatureWorldBlock.controller(level,base);
        replacement.loadWithComponents(drops.getFirst().getItem().get(DataComponents.BLOCK_ENTITY_DATA).copyTag(),level.registryAccess());
        h.assertTrue(replacement.getBlockState().getValue(MiniatureWorldBlock.COMPACT)&&replacement.material(0).getCount()==5,"Legacy contents/compact placement lost");h.succeed();
    }
    @GameTest(template="spell_arena") public static void survivalPlacementAndInteraction(GameTestHelper h){
        var level=h.getLevel();var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var ground=h.absolutePos(new BlockPos(10,2,5));level.setBlockAndUpdate(ground,Blocks.STONE.defaultBlockState());
        player.setPos(ground.getX()+.5,ground.getY()+1,ground.getZ()-3);player.setYRot(0);
        var stack=new ItemStack(MiniatureWorldContent.ITEM.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var hit=new net.minecraft.world.phys.BlockHitResult(ground.getCenter().add(0,.5,0),net.minecraft.core.Direction.UP,ground,false);
        var context=new net.minecraft.world.item.context.BlockPlaceContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,stack,hit);
        var base=context.getClickedPos().relative(context.getHorizontalDirection(),2).offset(-2,0,-2);
        h.assertTrue(MiniatureWorldContent.ITEM.get().place(context).consumesAction()&&stack.isEmpty(),"Survival placement did not consume one frame");
        var be=MiniatureWorldBlock.controller(level,base);h.assertTrue(be!=null,"Placement origin mismatch");
        for(int part:new int[]{0,4,20,24,62,100,104,120,124}){
            var at=base.offset(MiniatureWorldBlock.offset(part));var state=level.getBlockState(at);
            var click=new net.minecraft.world.phys.BlockHitResult(at.getCenter(),net.minecraft.core.Direction.NORTH,at,false);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND,3));
            state.useItemOn(player.getMainHandItem(),level,player,net.minecraft.world.InteractionHand.MAIN_HAND,click);
            h.assertTrue(player.getMainHandItem().isEmpty()&&be.material(0).getCount()==3,"Part failed to accept whole held stack");
            player.getInventory().clearContent();
            h.assertTrue(state.useItemOn(ItemStack.EMPTY,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,click)==net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,"Empty hand swallowed before extraction");
            state.useWithoutItem(level,player,click);
            h.assertTrue(be.size()==0&&player.getInventory().countItem(Items.DIAMOND)==3,"Part failed to return last stack");
        }
        be.insert(new ItemStack(Items.EMERALD,5));be.preview(MiniatureWorldBlockEntity.Stage.END);
        var state=level.getBlockState(base);state.getBlock().playerWillDestroy(level,base,state,player);
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(10),e->e.getItem().is(MiniatureWorldContent.ITEM.get()));
        h.assertTrue(drops.size()==1,"Player mining duplicated/lost frame");
        var packed=ItemStack.parse(level.registryAccess(),drops.get(0).getItem().save(level.registryAccess())).orElseThrow();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,packed);
        var replace=new net.minecraft.world.item.context.BlockPlaceContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,packed,hit);
        h.assertTrue(MiniatureWorldContent.ITEM.get().place(replace).consumesAction(),"Packed frame cannot be placed");
        var restored=MiniatureWorldBlock.controller(level,base);
        h.assertTrue(restored.stage()==MiniatureWorldBlockEntity.Stage.END&&restored.material(0).getCount()==5,"Survival re-placement lost state");h.succeed();
    }
    private static MiniatureWorldBlockEntity frame(GameTestHelper h){
        var base=h.absolutePos(new BlockPos(4,3,4));
        h.assertTrue(MiniatureWorldBlock.place(h.getLevel(),base),"Cannot place clear frame");
        return MiniatureWorldBlock.controller(h.getLevel(),base);
    }
    @GameTest(template="spell_arena") public static void allPartsResolveAndSingleController(GameTestHelper h){
        var be=frame(h);int controllers=0;
        for(int i=0;i<MiniatureWorldBlock.PARTS;i++){
            var p=be.getBlockPos().offset(MiniatureWorldBlock.offset(i));
            h.assertTrue(MiniatureWorldBlock.controller(h.getLevel(),p)==be,"Part did not resolve controller");
            if(h.getLevel().getBlockEntity(p)!=null)controllers++;
        }
        h.assertTrue(controllers==1,"Frame allocated extra block entities");h.succeed();
    }
    @GameTest(template="spell_arena") public static void materialStackAndReload(GameTestHelper h){
        var be=frame(h);var items=new Item[]{Items.DIRT,Items.STONE,Items.DIAMOND,Items.APPLE,Items.BREAD,Items.COAL,Items.STICK,Items.GOLD_INGOT,Items.IRON_INGOT};
        for(var item:items)h.assertTrue(be.insert(new ItemStack(item,2))==2,"Cannot insert compatible stack");
        h.assertTrue(be.insert(new ItemStack(Items.SAND))==0,"Tenth material accepted");
        h.assertTrue(be.insert(new ItemStack(Items.DIRT,64))==62,"Same material capacity incorrect");
        h.assertTrue(be.insert(new ItemStack(Items.DIRT))==0,"Overfilled material");
        be.loadWithComponents(be.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        var top=be.take();h.assertTrue(top.is(Items.DIRT)&&top.getCount()==64,"Merged material did not move to stack top");
        for(int i=8;i>=1;i--)h.assertTrue(be.take().is(items[i]),"Not LIFO after reload");
        h.assertTrue(be.take().isEmpty(),"Extra material after extraction");
        var first=new ItemStack(Items.DIAMOND_SWORD);first.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("a"));
        var second=new ItemStack(Items.DIAMOND_SWORD);second.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("b"));
        h.assertTrue(be.insert(first)==1&&be.insert(second)==1&&be.size()==2,"Distinct item components merged");h.succeed();
    }
    @GameTest(template="spell_arena") public static void blockedPlacementLeavesWorldUntouched(GameTestHelper h){
        var base=h.absolutePos(new BlockPos(4,3,4));var wall=base.offset(4,4,4);h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        h.assertTrue(!MiniatureWorldBlock.place(h.getLevel(),base),"Blocked placement succeeded");
        h.assertTrue(h.getLevel().getBlockState(wall).is(Blocks.STONE),"Placement destroyed blocker");
        for(int i=0;i<MiniatureWorldBlock.PARTS;i++)h.assertTrue(!h.getLevel().getBlockState(base.offset(MiniatureWorldBlock.offset(i))).is(MiniatureWorldContent.BLOCK.get()),"Failed placement left parts");h.succeed();
    }
    @GameTest(template="spell_arena") public static void breakNonRootPacksExactlyOnce(GameTestHelper h){
        var be=frame(h);var level=h.getLevel();var base=be.getBlockPos();be.insert(new ItemStack(Items.DIAMOND,4));be.preview(MiniatureWorldBlockEntity.Stage.NETHER);
        level.destroyBlock(base.offset(4,3,4),true);
        for(int i=0;i<MiniatureWorldBlock.PARTS;i++)h.assertTrue(!level.getBlockState(base.offset(MiniatureWorldBlock.offset(i))).is(MiniatureWorldContent.BLOCK.get()),"Broken frame left a part");
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(10),e->e.getItem().is(MiniatureWorldContent.ITEM.get()));
        h.assertTrue(drops.size()==1&&drops.get(0).getItem().getCount()==1,"Frame duplicated or lost its drop");
        var saved=drops.get(0).getItem().get(DataComponents.BLOCK_ENTITY_DATA);h.assertTrue(saved!=null,"Missing packed state");
        h.assertTrue(MiniatureWorldBlock.place(level,base),"Cannot replace frame");var restored=MiniatureWorldBlock.controller(level,base);
        restored.loadWithComponents(saved.copyTag(),level.registryAccess());
        h.assertTrue(restored.stage()==MiniatureWorldBlockEntity.Stage.NETHER&&restored.take().getCount()==4,"Packed frame lost progress or materials");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rootRemovalAndCompletionOnlyOnce(GameTestHelper h){
        var be=frame(h);var base=be.getBlockPos();be.insert(new ItemStack(Items.APPLE,3));
        be.preview(MiniatureWorldBlockEntity.Stage.COMPLETE);
        h.assertTrue(be.takeProduct().is(com.mcmagic.omnira.registry.ModItems.OMNI_MICROCORE.get()),"Missing final product");
        h.assertTrue(be.takeProduct().isEmpty()&&be.stage()==MiniatureWorldBlockEntity.Stage.EMPTY,"Repeated final product or failure to reset");
        h.assertTrue(be.size()==0,"Product extraction did not reset frame");
        h.getLevel().destroyBlock(base,true);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(10),e->e.getItem().is(MiniatureWorldContent.ITEM.get()));
        h.assertTrue(drops.size()==1,"Root removal lost or duplicated frame");h.succeed();
    }
}
