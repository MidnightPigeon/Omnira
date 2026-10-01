package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.client.screen.*;
import com.mcmagic.omnira.menu.*;
import com.mcmagic.omnira.recipe.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.SpellPayload;
import mezz.jei.api.*;
import mezz.jei.api.gui.builder.*;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.*;

@JeiPlugin
public final class OmniraJeiPlugin implements IModPlugin {
    private static mezz.jei.api.runtime.IJeiRuntime runtime;
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime available) {
        runtime=available;
        runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                List.of(new ItemStack(ModItems.PERMANENT_VOID_CRYSTAL.get()),new ItemStack(DreamContent.CRYSTAL_COLUMN_CAPITAL.get())));
    }
    @Override public void onRuntimeUnavailable() {runtime=null;}
    public static boolean engineUnderMouse() {
        if(runtime==null) return false;
        var type=mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
        ItemStack stack=runtime.getIngredientListOverlay().getIngredientUnderMouse(type);
        if(stack!=null && stack.is(ModItems.MANA_ENGINE.get())) return true;
        stack=runtime.getBookmarkOverlay().getIngredientUnderMouse(type);
        if(stack!=null && stack.is(ModItems.MANA_ENGINE.get())) return true;
        return Minecraft.getInstance().screen==runtime.getRecipesGui()
                && runtime.getRecipesGui().getIngredientUnderMouse(type).filter(item->item.is(ModItems.MANA_ENGINE.get())).isPresent();
    }
    public record Writing(List<ItemStack> inputs,ItemStack output) {}
    public static final RecipeType<AnalysisRecipe> ANALYSIS=RecipeType.create("omnira","analysis",AnalysisRecipe.class);
    public static final RecipeType<com.mcmagic.omnira.forging.AdvancedForgeRecipe> ADVANCED_FORGE=RecipeType.create("omnira","advanced_assembly_table",com.mcmagic.omnira.forging.AdvancedForgeRecipe.class);
    public static final RecipeType<CondensationRecipe> CONDENSATION=RecipeType.create("omnira","condensation",CondensationRecipe.class);
    public static final RecipeType<CondensationRecipe> ADVANCED_CONDENSATION=RecipeType.create("omnira","advanced_condensation",CondensationRecipe.class);
    public static final RecipeType<AssemblyRecipe> ASSEMBLY=RecipeType.create("omnira","assembly",AssemblyRecipe.class);
    public static final RecipeType<Writing> WRITING=RecipeType.create("omnira","writing",Writing.class);
    @Override public ResourceLocation getPluginUid() {return ResourceLocation.fromNamespaceAndPath("omnira","workstations");}
    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.MODULAR_STAFF.get(),new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<ItemStack>() {
            @Override public Object getSubtypeData(ItemStack stack,mezz.jei.api.ingredients.subtypes.UidContext context) {
                return com.mcmagic.omnira.item.staff.StaffAssembly.of(stack);
            }
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack,mezz.jei.api.ingredients.subtypes.UidContext context) {
                var assembly=com.mcmagic.omnira.item.staff.StaffAssembly.of(stack);
                return assembly.components().stream().map(part->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(part.getItem())
                        +part.getComponentsPatch().toString()).collect(java.util.stream.Collectors.joining("|"));
            }
        });
    }
    @Override public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(StaffAssemblyRecipes.previews().stream().map(AssemblyRecipe::result).toList());
    }

    private abstract static class Category<T> implements IRecipeCategory<T> {
        final RecipeType<T> type;
        final Item workstation;
        final IDrawable icon;
        Category(RecipeType<T> type,Item workstation,IGuiHelper gui) {
            this.type=type;this.workstation=workstation;
            icon=gui.createDrawableItemStack(new ItemStack(workstation));
        }
        @Override public RecipeType<T> getRecipeType() {return type;}
        @Override public Component getTitle() {return workstation.getDescription();}
        @Override public int getWidth() {return 176;}
        @Override public int getHeight() {return 116;}
        static int compactY(int y) {return Math.round(y*.70F);}
        @Override public IDrawable getIcon() {return icon;}
        static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder,RecipeIngredientRole role,int x,int y) {
            return builder.addSlot(role,x-8,compactY(y)-8).setStandardSlotBackground();
        }
        static void line(GuiGraphics g,int x,int y,int xx,int yy,int color) {
            y=compactY(y);yy=compactY(yy);
            int n=Math.max(Math.abs(xx-x),Math.abs(yy-y));
            for(int i=0;i<=n;i++) {
                int a=Math.round(x+(xx-x)*i/(float)Math.max(1,n)),b=Math.round(y+(yy-y)*i/(float)Math.max(1,n));
                g.fill(a,b,a+1,b+1,color);
            }
        }
        static void ring(GuiGraphics g,int[][] points) {
            for(int i=0;i<points.length;i++) {
                var a=points[i];var b=points[(i+1)%points.length];line(g,a[0],a[1],b[0],b[1],0xFF93ABB5);
            }
        }
        static void label(GuiGraphics g,Component text,int x,int y) {
            float scale=Math.min(.8F,(171F-x)/Math.max(1,Minecraft.getInstance().font.width(text)));
            g.pose().pushPose();g.pose().translate(x,compactY(y),0);g.pose().scale(scale,scale,1);
            g.drawString(Minecraft.getInstance().font,text,0,0,0xFF404040,false);g.pose().popPose();
        }
        static void mana(GuiGraphics g,int amount,String extra) {
            label(g,Component.translatable("jei.omnira.mana",amount),5,137);
            label(g,Component.translatable(extra),5,149);
        }
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui=registration.getJeiHelpers().getGuiHelper();
        RitualJei.register(registration,gui);
        registration.addRecipeCategories(new Category<com.mcmagic.omnira.forging.AdvancedForgeRecipe>(ADVANCED_FORGE,ModItems.ADVANCED_FORGE.get(),gui){
            @Override public Component getTitle(){return Component.translatable("block.omnira.advanced_assembly_table");}
            @Override public void setRecipe(IRecipeLayoutBuilder b,com.mcmagic.omnira.forging.AdvancedForgeRecipe r,IFocusGroup focus){
                for(int i=0;i<6;i++){int[] p=ArcaneAssemblyTableMenu.CENTERS[i];slot(b,RecipeIngredientRole.INPUT,p[0]-30,p[1]-20).addIngredients(r.slots().get(i));}
                // Facing into the entrance, local +X is the player's left.
                for(int i=0;i<3;i++)slot(b,RecipeIngredientRole.INPUT,30+i*50,118).addIngredients(r.slots().get(6+i));
                slot(b,RecipeIngredientRole.CATALYST,58,58).addItemStacks(List.of(new ItemStack(ModItems.TEST_SPELL_CORE.get()),new ItemStack(ModItems.DREAM_SPELL_CORE.get()),new ItemStack(ModItems.LIGHT_DARK_SPELL_CORE.get()),new ItemStack(ModItems.SPACETIME_SPELL_CORE.get())))
                        .addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.omnira.forge_core")));
                slot(b,RecipeIngredientRole.OUTPUT,147,58).addItemStack(r.displayResult());
            }
            @Override public void draw(com.mcmagic.omnira.forging.AdvancedForgeRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
                line(g,107,58,127,58,0xFF93ABB5);
                label(g,Component.translatable("jei.omnira.forge_nodes"),5,137);
                label(g,Component.translatable("jei.omnira.forge_core"),5,153);
            }
        });
        registration.addRecipeCategories(
            new Category<AnalysisRecipe>(ANALYSIS,ModItems.ANALYSIS_ARTISAN_TABLE.get(),gui) {
                @Override public void setRecipe(IRecipeLayoutBuilder b,AnalysisRecipe r,IFocusGroup focus) {
                    var inputs=Arrays.stream(r.ingredient().getItems()).map(stack->stack.copyWithCount(r.count())).toList();
                    var inputSlot=slot(b,RecipeIngredientRole.INPUT,88,28).addItemStacks(inputs);
                    if(r.inputDamage()>0) inputSlot.addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.omnira.input_damage",r.inputDamage())));
                    slot(b,RecipeIngredientRole.OUTPUT,52,94).addItemStack(r.result());
                    if(r.secondaryOutput().max()>0) {
                        slot(b,RecipeIngredientRole.OUTPUT,124,94).addItemStacks(r.secondaryOutput().displayOutputs())
                                .addRichTooltipCallback((view,tooltip)->{
                                    if(r.secondaryOutput().min()!=r.secondaryOutput().max()) tooltip.add(Component.translatable("jei.omnira.byproduct",r.secondaryOutput().min(),r.secondaryOutput().max()));
                                    if(r.secondaryOutput().choices().size()>1) tooltip.add(Component.translatable("jei.omnira.byproduct_choice"));
                                });
                    }
                    var remains=inputs.stream().map(ItemStack::getCraftingRemainingItem).filter(stack->!stack.isEmpty()).toList();
                    if(r.inputDamage()==0 && !remains.isEmpty()) slot(b,RecipeIngredientRole.OUTPUT,140,28).addItemStacks(remains)
                            .addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.omnira.remainder")));
                }
                @Override public void draw(AnalysisRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                    line(g,88,38,88,62,0xFF93ABB5);line(g,52,62,124,62,0xFF93ABB5);
                    line(g,52,62,52,84,0xFF93ABB5);line(g,124,62,124,84,0xFF93ABB5);
                    ring(g,new int[][]{{88,53},{96,62},{88,71},{80,62}});
                    mana(g,AnalysisRecipe.MANA_COST,"jei.omnira.analysis_time");
                }
            },
            new Category<CondensationRecipe>(CONDENSATION,ModItems.SIMPLE_CONDENSATION_TABLE.get(),gui) {
                @Override public void setRecipe(IRecipeLayoutBuilder b,CondensationRecipe r,IFocusGroup focus) {
                    slot(b,RecipeIngredientRole.OUTPUT,88,65).addItemStack(r.result());
                }
                @Override public void draw(CondensationRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                    ring(g,new int[][]{{88,30},{120,48},{120,82},{88,100},{56,82},{56,48}});
                    mana(g,r.manaCost(),"jei.omnira.condensation_time");
                }
            },
            new Category<CondensationRecipe>(ADVANCED_CONDENSATION,ModItems.ADVANCED_CONDENSATION_TABLE.get(),gui) {
                @Override public void setRecipe(IRecipeLayoutBuilder b,CondensationRecipe r,IFocusGroup focus) {
                    slot(b,RecipeIngredientRole.INPUT,52,65).addIngredients(r.ingredient());
                    slot(b,RecipeIngredientRole.OUTPUT,124,65).addItemStack(r.result());
                }
                @Override public void draw(CondensationRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                    ring(g,new int[][]{{88,35},{119,51},{119,85},{88,101},{57,85},{57,51}});
                    mana(g,r.manaCost(),"jei.omnira.advanced_condensation_time");
                }
            },
            new Category<AssemblyRecipe>(ASSEMBLY,ModItems.ARCANE_ASSEMBLY_TABLE.get(),gui) {
                @Override public void setRecipe(IRecipeLayoutBuilder b,AssemblyRecipe r,IFocusGroup focus) {
                    for(int i=0;i<7;i++) {
                        var p=ArcaneAssemblyTableMenu.CENTERS[i];
                        var input=slot(b,RecipeIngredientRole.INPUT,p[0]-10,p[1]-10).addIngredients(r.slots().get(i));
                        if(r.outerShapeless()) input.addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.omnira.assembly_outer_shapeless")));
                    }
                    slot(b,RecipeIngredientRole.OUTPUT,156,68).addItemStack(r.result())
                            .addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.omnira.assembly_center")));
                }
                @Override public void draw(AssemblyRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                    int[][] points=Arrays.stream(ArcaneAssemblyTableMenu.CENTERS).limit(6).map(p->new int[]{p[0]-10,p[1]-10}).toArray(int[][]::new);
                    ring(g,points);for(var p:points) line(g,p[0],p[1],78,68,0xFFB9C9D0);
                    gui.getRecipeArrow().draw(g,124,compactY(68)-7);
                    mana(g,50,"jei.omnira.assembly_progress");
                }
            },
            new Category<Writing>(WRITING,ModItems.CRYSTAL_PROCESSING_TABLE.get(),gui) {
                final int[][] points={{66,26},{108,26},{38,75},{66,124},{108,124},{136,75},{73,68},{101,68},{87,96}};
                @Override public void setRecipe(IRecipeLayoutBuilder b,Writing r,IFocusGroup focus) {
                    for(int i=0;i<8;i++) {
                        var slot=slot(b,RecipeIngredientRole.INPUT,points[i][0],points[i][1]);
                        if(i==6) slot.addItemStacks(List.of(new ItemStack(ModItems.SPELL_INK.get()),new ItemStack(Items.INK_SAC),new ItemStack(Items.GLOW_INK_SAC)))
                                .addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.omnira.ink")));
                        else if(!r.inputs().get(i).isEmpty()) slot.addItemStack(r.inputs().get(i));
                        if(i<2) {final int index=i;slot.addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.omnira."+ (index==0?"target":"shape"))));}
                    }
                    slot(b,RecipeIngredientRole.OUTPUT,points[8][0],points[8][1]).addItemStack(r.output());
                }
                @Override public void draw(Writing r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                    ring(g,new int[][]{points[0],points[1],points[5],points[4],points[3],points[2]});
                    if(com.mcmagic.omnira.spell.SpellPattern.composite(r.inputs().getFirst()))
                        com.mcmagic.omnira.client.screen.CompositeSlotGhost.render(g,r.inputs().getFirst(),points[1][0]-8,compactY(points[1][1])-8);
                    mana(g,SpellPayload.of(r.output()).baseCost(),"jei.omnira.writing_time");
                }
            }
        );
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level=Minecraft.getInstance().level;
        if(level==null) return;
        var recipes=level.getRecipeManager();
        registration.addRecipes(ADVANCED_FORGE,recipes.getAllRecipesFor(ModRecipes.ADVANCED_FORGE_TYPE.get()).stream().map(RecipeHolder::value).toList());
        var rituals=new ArrayList<RitualJei.Entry>();
        rituals.add(new RitualJei.Entry(new RitualJei.Dream()));
        rituals.add(new RitualJei.Entry(new RitualJei.Corridor()));
        rituals.add(new RitualJei.Entry(new RitualJei.Resonance()));
        rituals.add(new RitualJei.Entry(new RitualJei.Cleansing()));
        for(var affinity:List.of(com.mcmagic.omnira.mana.Affinity.LIGHT,com.mcmagic.omnira.mana.Affinity.DARK,
                com.mcmagic.omnira.mana.Affinity.ELEMENTAL))rituals.add(new RitualJei.Entry(new RitualJei.Affinity(affinity)));
        recipes.getAllRecipesFor(ModRecipes.SWORD_SHAPING_TYPE.get()).forEach(holder->rituals.add(new RitualJei.Entry(holder.value())));
        recipes.getAllRecipesFor(ModRecipes.VESSEL_CONVERSION_TYPE.get()).forEach(holder->rituals.add(new RitualJei.Entry(holder.value())));
        registration.addRecipes(RitualJei.TYPE,rituals);
        registration.addRecipes(ANALYSIS,recipes.getAllRecipesFor(ModRecipes.ANALYSIS_TYPE.get()).stream().map(RecipeHolder::value).toList());
        registration.addRecipes(CONDENSATION,recipes.getAllRecipesFor(ModRecipes.CONDENSATION_TYPE.get()).stream().map(RecipeHolder::value).filter(r->!r.advanced()).toList());
        registration.addRecipes(ADVANCED_CONDENSATION,recipes.getAllRecipesFor(ModRecipes.CONDENSATION_TYPE.get()).stream().map(RecipeHolder::value).filter(CondensationRecipe::advanced).toList());
        var assemblyRecipes=new ArrayList<AssemblyRecipe>();
        for(var holder:recipes.getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE.get())) {
            if(holder.value().operation().equals("staff")) assemblyRecipes.addAll(StaffAssemblyRecipes.previews());
            else assemblyRecipes.add(holder.value());
        }
        registration.addRecipes(ASSEMBLY,assemblyRecipes);
        registration.addRecipes(WRITING,writingRecipes());
    }
    private static List<List<ItemStack>> pairs(List<Item> items,boolean ordered) {
        var result=new ArrayList<List<ItemStack>>();
        result.add(List.of(ItemStack.EMPTY,ItemStack.EMPTY));
        for(int i=0;i<items.size();i++) {
            result.add(List.of(new ItemStack(items.get(i)),ItemStack.EMPTY));
            for(int j=ordered?0:i;j<items.size();j++) result.add(List.of(new ItemStack(items.get(i)),new ItemStack(items.get(j))));
        }
        return result;
    }
    private static List<Writing> writingRecipes() {
        var result=new ArrayList<Writing>();
        var cores=List.of(ModItems.EARTH_MICROCORE.get(),ModItems.WATER_MICROCORE.get(),ModItems.FIRE_MICROCORE.get(),ModItems.AIR_MICROCORE.get());
        var elements=pairs(List.of(ModItems.SHARP_BREATH.get(),ModItems.HEALING_DEW.get(),ModItems.DISSOCIATION_THREAD.get(),ModItems.CONSTRUCTION_MATRIX.get(),ModItems.DARK_MICROCORE.get()),true);
        var modifiers=pairs(List.of(Items.CLOCK,Items.IRON_BLOCK,ModItems.SPIRITUAL_CRYSTAL.get(),ModItems.LIGHT_MICROCORE.get()),false);
        for(var target:cores) for(var shape:cores) for(var element:elements) for(var modifier:modifiers) {
            var inputs=List.of(new ItemStack(target),new ItemStack(shape),modifier.get(0),modifier.get(1),element.get(0),element.get(1),
                    new ItemStack(ModItems.SPELL_INK.get()),new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
            var container=new SimpleContainer(9);
            for(int i=0;i<8;i++) container.setItem(i,inputs.get(i).copy());
            result.add(new Writing(inputs,CrystalProcessingTableMenu.previewCrystal(container)));
        }
        for(var target:List.of(ModItems.TIME_MICROCORE.get(),ModItems.SPACE_MICROCORE.get()))
            for(var element:elements) for(var modifier:modifiers) {
                var inputs=List.of(new ItemStack(target),ItemStack.EMPTY,modifier.get(0),modifier.get(1),element.get(0),element.get(1),
                        new ItemStack(ModItems.SPELL_INK.get()),new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
                var container=new SimpleContainer(9);
                for(int i=0;i<8;i++)container.setItem(i,inputs.get(i).copy());
                result.add(new Writing(inputs,CrystalProcessingTableMenu.previewCrystal(container)));
            }
        return result;
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.CRYSTAL_PEDESTAL.get()),RitualJei.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.RITUAL_ENERGY_CORE.get()),RitualJei.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ADVANCED_RITUAL_ENERGY_CORE.get()),RitualJei.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.PURE_VESSEL.get()),RitualJei.TYPE);
        if(net.neoforged.fml.ModList.get().isLoaded("create")) CreatePressingJei.catalyst(registration);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ANALYSIS_ARTISAN_TABLE.get()),ANALYSIS);
        registration.addRecipeCatalyst(new ItemStack(ModItems.SIMPLE_CONDENSATION_TABLE.get()),CONDENSATION);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ADVANCED_CONDENSATION_TABLE.get()),ADVANCED_CONDENSATION);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ARCANE_ASSEMBLY_TABLE.get()),ASSEMBLY);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ADVANCED_FORGE.get()),ADVANCED_FORGE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.CRYSTAL_PROCESSING_TABLE.get()),WRITING);
    }
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        if(net.neoforged.fml.ModList.get().isLoaded("create")) CreatePressingJei.transfer(registration);
        registration.addRecipeTransferHandler(CrystalProcessingTableMenu.class,ModMenuTypes.CRYSTAL_PROCESSING_TABLE.get(),WRITING,0,8,9,36);
        registration.addRecipeTransferHandler(ArcaneAssemblyTableMenu.class,ModMenuTypes.ARCANE_ASSEMBLY_TABLE.get(),ASSEMBLY,0,7,7,36);
        // Analysis consumes counted stacks, so a single-item automatic transfer would be misleading.
    }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(CrystalProcessingTableScreen.class,28,7,120,16,WRITING);
        registration.addRecipeClickArea(AnalysisArtisanTableScreen.class,28,7,120,16,ANALYSIS);
        registration.addRecipeClickArea(SimpleCondensationTableScreen.class,28,7,120,16,CONDENSATION,ADVANCED_CONDENSATION);
        registration.addRecipeClickArea(ArcaneAssemblyTableScreen.class,28,7,120,16,ASSEMBLY);
    }
}
