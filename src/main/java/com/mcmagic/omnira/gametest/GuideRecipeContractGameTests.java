package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.client.guide.*;
import com.mcmagic.omnira.client.guide.GuideRecipes.*;
import com.mcmagic.omnira.forging.AdvancedForgeRecipe;
import com.mcmagic.omnira.recipe.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Read the actual guide diagrams and reconstruct their displayed inputs without starting a client. */
@GameTestHolder("omnira_guide_recipe")
@PrefixGameTestTemplate(false)
public final class GuideRecipeContractGameTests {
    private static Slot at(Diagram diagram,int x,int y){return diagram.slots().stream().filter(s->s.x()==x&&s.y()==y).findFirst().orElseThrow();}
    private static ItemStack shown(Slot slot,int tick){return slot.items().isEmpty()?ItemStack.EMPTY:slot.items().get(Math.floorMod(tick,slot.items().size())).copy();}
    private static ItemStack shown(Diagram d,int x,int y){return shown(at(d,x,y),0);}
    private static void output(GameTestHelper h,Diagram d,int x,int y,ItemStack actual){
        h.assertTrue(at(d,x,y).output()&&ItemStack.matches(shown(d,x,y),actual),"Guide result/count/components differ: "+d.title().getString());
    }
    private static void entries(JsonObject chapter,Set<Item> items){
        if(chapter.has("entries"))for(var entry:chapter.getAsJsonArray("entries")){
            var data=entry.getAsJsonObject();
            if(!data.has("recipes")||data.get("recipes").getAsBoolean())items.add(BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.get("item").getAsString())));
        }
        if(chapter.has("chapters"))for(var child:chapter.getAsJsonArray("chapters"))entries(child.getAsJsonObject(),items);
    }
    private static Set<Item> bookItems(String locale)throws Exception{
        var book=GuideResourceTree.load(path->{
            try(var stream=GuideRecipeContractGameTests.class.getResourceAsStream("/assets/omnira/guide/"+locale+"/"+path)){
                if(stream==null)throw new java.io.IOException(path);
                return JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            }
        });
        var items=new LinkedHashSet<Item>();entries(book,items);return items;
    }
    private static void verify(GameTestHelper h,Diagram d){
        var level=h.getLevel();var source=d.source();var registries=level.registryAccess();
        for(var slot:d.slots())h.assertTrue(slot.x()-8>=0&&slot.x()+8<=124&&slot.y()-8>=36&&slot.y()+8<=146,"Guide recipe slot leaves its existing page: "+d.title().getString());
        if(source instanceof CraftingRecipe recipe){
            int width=recipe instanceof ShapedRecipe shaped?shaped.getWidth():recipe instanceof WrappedShapedRecipe wrapped?wrapped.shaped().getWidth():3;
            int height=(recipe.getIngredients().size()+width-1)/width;
            var inputs=new ArrayList<ItemStack>();
            for(int i=0;i<width*height;i++)inputs.add(i<recipe.getIngredients().size()?shown(d,17+i%width*18,72+i/width*18):ItemStack.EMPTY);
            var input=CraftingInput.of(width,height,inputs);
            h.assertTrue(recipe.matches(input,level),"Guide crafting grid rejected: "+d.title().getString());
            output(h,d,108,90,recipe.assemble(input,registries));
        }else if(source instanceof SmithingRecipe recipe){
            var input=new SmithingRecipeInput(shown(d,17,90),shown(d,35,90),shown(d,53,90));
            h.assertTrue(recipe.matches(input,level),"Guide smithing inputs rejected");output(h,d,108,90,recipe.assemble(input,registries));
        }else if(source instanceof CondensationRecipe recipe){
            var stack=recipe.advanced()?shown(d,24,88):ItemStack.EMPTY;
            h.assertTrue(recipe.matches(new SingleRecipeInput(stack),level),"Guide condensation amount rejected");
            output(h,d,recipe.advanced()?104:64,88,recipe.assemble(new SingleRecipeInput(stack),registries));
        }else if(source instanceof AnalysisRecipe recipe){
            var slot=at(d,24,87);
            for(int tick=0;tick<slot.items().size();tick++){
                var input=shown(slot,tick);
                h.assertTrue(recipe.matches(new SingleRecipeInput(input),level),"Guide analysis amount rejected");
                output(h,d,101,72,recipe.assemble(new SingleRecipeInput(input),registries));
                var remainder=recipe.remainder(input);var display=d.slots().stream().filter(s->s.x()==56&&s.y()==87).findFirst();
                if(display.isPresent())h.assertTrue(ItemStack.matches(shown(display.get(),tick),remainder),"Guide remainder is out of sync with input");
                else h.assertTrue(remainder.isEmpty(),"Guide omits a container remainder");
            }
            var secondary=recipe.secondaryOutput().displayOutputs();
            if(!secondary.isEmpty()){
                var shown=at(d,101,105).items();h.assertTrue(shown.size()==secondary.size(),"Guide byproduct options missing");
                for(int i=0;i<shown.size();i++)h.assertTrue(ItemStack.matches(shown.get(i),secondary.get(i)),"Guide byproduct count differs");
            }
        }else if(source instanceof AdvancedForgeRecipe recipe){
            var inventory=new SimpleContainer(9);
            for(int i=0;i<6;i++)inventory.setItem(i,shown(d,24+i%3*22,62+i/3*22));
            for(int i=0;i<3;i++)inventory.setItem(6+i,shown(d,24+i*22,118));
            var input=new AdvancedForgeRecipe.Input(inventory);
            h.assertTrue(recipe.matches(input,level),"Guide advanced assembly inputs rejected");
            var core=at(d,108,118);h.assertTrue(core.retained()&&!core.output()&&!core.items().isEmpty(),"Guide omits retained power core");
            for(var stack:core.items())h.assertTrue(stack.getItem() instanceof com.mcmagic.omnira.item.SpellCoreItem,"Invalid guide power core");
            output(h,d,108,84,recipe.assemble(input,registries));
        }else if(source instanceof AssemblyRecipe recipe){
            int[][] positions={{80,53},{96,81},{80,109},{48,109},{32,81},{48,53},{64,81}};
            var inventory=new SimpleContainer(7);
            for(int i=0;i<7;i++)inventory.setItem(i,shown(d,positions[i][0],positions[i][1]));
            var input=new AssemblyRecipe.Input(inventory);
            h.assertTrue(recipe.matches(input,level),"Guide assembly example rejected");output(h,d,114,127,recipe.assemble(input,registries));
        }else if(source instanceof SwordShapingRecipe recipe){
            int[][] positions={{60,47},{96,83},{60,119},{24,83}};
            var offerings=new ArrayList<ItemStack>();for(var p:positions)offerings.add(shown(d,p[0],p[1]));
            var input=new SwordShapingRecipe.Input(shown(d,60,83),offerings);
            h.assertTrue(recipe.matches(input,level),"Guide sword offerings rejected");
            h.assertTrue(shown(d,114,59).is(ModItems.PURE_VESSEL.get()),"Guide omits consumed vessel");output(h,d,114,136,recipe.assemble(input,registries));
        }else if(source instanceof StarlightConversionRecipe recipe){
            var input=new SingleRecipeInput(shown(d,24,88));
            h.assertTrue(recipe.matches(input,level),"Guide starlight input rejected");output(h,d,104,88,recipe.assemble(input,registries));
        }else if(source instanceof com.mcmagic.omnira.myriad.WorldDevelopmentRecipe recipe){
            var stacks=new ArrayList<ItemStack>();int count=recipe.materials().size();
            for(int i=0;i<count;i++)stacks.add(shown(d,(144-Math.min(3,count)*24)/2+i%3*24,76+i/3*22));
            h.assertTrue(recipe.matches(new com.mcmagic.omnira.myriad.WorldDevelopmentRecipe.Input(stacks),level),"Guide world evolution amount rejected");
            var actual=recipe.getResultItem(registries);if(!actual.isEmpty())output(h,d,64,124,actual);
        }else if(source==null&&d.slots().stream().anyMatch(s->s.output()&&s.items().stream().anyMatch(i->i.is(ModItems.LOW_TIER_MAGIC_CRYSTAL.get())))){
            int[][] positions={{43,48},{85,48},{21,86},{43,124},{85,124},{107,86},{50,80},{78,80}};
            var inventory=new SimpleContainer(9);for(int i=0;i<8;i++)inventory.setItem(i,shown(d,positions[i][0],positions[i][1]));
            h.assertTrue(inventory.getItem(7).is(ModItems.SPIRITUAL_CRYSTAL.get()),"Guide spell substrate differs");
            for(var ink:at(d,50,80).items())h.assertTrue(com.mcmagic.omnira.menu.CrystalProcessingTableMenu.isInk(ink),"Invalid spell ink alternative");
            output(h,d,64,104,com.mcmagic.omnira.menu.CrystalProcessingTableMenu.previewCrystal(inventory));
        }else throw new AssertionError("Unverified guide recipe source: "+source);
    }
    @GameTest(template="spell_arena") public static void allReferencedBookRecipePagesMatchExecution(GameTestHelper h)throws Exception{
        var chinese=bookItems("zh_cn");h.assertTrue(chinese.equals(bookItems("en_us")),"Languages reference different recipe items");
        int pages=0;
        for(var item:chinese)for(var diagram:GuideRecipes.find(h.getLevel(),item)){verify(h,diagram);pages++;}
        h.assertTrue(pages>100,"Guide contract did not reach the current book recipes");
        com.mcmagic.omnira.Omnira.LOGGER.info("Guide contract: {} recipe-enabled items, {} actual generated pages",chinese.size(),pages);h.succeed();
    }
    @GameTest(template="spell_arena") public static void reloadUpdatesCountsOutputsAndSceneExample(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();var before=List.copyOf(manager.getRecipes());
        var recipes=new ArrayList<RecipeHolder<?>>(before);
        var condensation=new CondensationRecipe(75,new ItemStack(Items.BRICK,3),Ingredient.of(Items.DIAMOND),7);
        var analysis=new AnalysisRecipe(Ingredient.of(Items.WATER_BUCKET,Items.STICK),1,new ItemStack(Items.CLAY_BALL,2),0,0);
        var sword=new SwordShapingRecipe(List.of(Ingredient.of(Items.DIAMOND),Ingredient.of(Items.EMERALD),Ingredient.of(Items.IRON_INGOT),Ingredient.of(Items.GOLD_INGOT)),new ItemStack(ModItems.CRYSTALLIZED_NAIL.get()));
        recipes.removeIf(r->r.id().equals(ResourceLocation.parse("omnira:sword_shaping/crystallized_bone_nail")));
        recipes.add(new RecipeHolder<>(ResourceLocation.parse("omnira:guide_count_probe"),condensation));
        recipes.add(new RecipeHolder<>(ResourceLocation.parse("omnira:guide_remainder_probe"),analysis));
        recipes.add(new RecipeHolder<>(ResourceLocation.parse("omnira:sword_shaping/crystallized_bone_nail"),sword));
        try{
            manager.replaceRecipes(recipes);
            var count=GuideRecipes.find(h.getLevel(),Items.BRICK).stream().filter(d->d.source()==condensation).findFirst().orElseThrow();verify(h,count);
            h.assertTrue(shown(count,24,88).getCount()==7&&shown(count,104,88).getCount()==3,"Guide cached hardcoded quantities");
            verify(h,GuideRecipes.find(h.getLevel(),Items.CLAY_BALL).stream().filter(d->d.source()==analysis).findFirst().orElseThrow());
            h.assertTrue(GuideRecipes.swordExample(h.getLevel()).orElseThrow()==sword,"Scene retained hardcoded sword offerings");
        }finally{manager.replaceRecipes(before);}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void energyFiguresUseActualFuelCatalogue(GameTestHelper h){
        for(var entry:List.of("temporal_energy","spatial_energy")){
            var fuels=com.mcmagic.omnira.reversal.ReversalFuels.all(entry.equals("spatial_energy"));var slots=GuideEnergy.pages(entry).getFirst().slots();
            h.assertTrue(slots.size()==fuels.size(),"Guide energy catalogue count differs");
            for(int i=0;i<slots.size();i++)h.assertTrue(ItemStack.matches(shown(slots.get(i),0),fuels.get(i).stack()),"Guide energy material differs");
        }
        h.succeed();
    }
    private GuideRecipeContractGameTests(){}
}
