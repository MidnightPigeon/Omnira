package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.recipe.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;

/** Ritual diagrams represent world positions, not an inventory or crafting grid. */
final class RitualJei {
    record Entry(Object ritual) {}
    static final RecipeType<Entry> TYPE=RecipeType.create("omnira","ritual",Entry.class);
    record Dream() {}
    record Corridor() {}
    static final RecipeType<Corridor> CORRIDOR=RecipeType.create("omnira","corridor_ritual",Corridor.class);
    record Resonance() {}
    record Cleansing() {}
    record Affinity(com.mcmagic.omnira.mana.Affinity kind) {}
    static final RecipeType<SwordShapingRecipe> SWORD_SHAPING=RecipeType.create("omnira","sword_shaping",SwordShapingRecipe.class);
    static final RecipeType<VesselConversionRecipe> VESSEL_CONVERSION=RecipeType.create("omnira","vessel_conversion",VesselConversionRecipe.class);
    static final RecipeType<Affinity> AFFINITY=RecipeType.create("omnira","affinity_ritual",Affinity.class);
    static final RecipeType<Resonance> RESONANCE=RecipeType.create("omnira","resonance_ritual",Resonance.class);
    static final RecipeType<Cleansing> CLEANSING=RecipeType.create("omnira","cleansing_ritual",Cleansing.class);
    static final RecipeType<Dream> DREAM=RecipeType.create("omnira","dream_ritual",Dream.class);
    private abstract static class Page<T> implements IRecipeCategory<T> {
        private final RecipeType<T> type;
        private final IDrawable icon;
        private final String title;
        Page(RecipeType<T> type,Item item,String title,IGuiHelper gui) {
            this.type=type;this.title=title;icon=gui.createDrawableItemStack(new ItemStack(item));
        }
        public RecipeType<T> getRecipeType(){return type;}
        public Component getTitle(){return Component.translatable(title);}
        public IDrawable getIcon(){return icon;}
        public int getWidth(){return 220;}
        public int getHeight(){return 190;}
        void note(GuiGraphics g,String key){g.drawWordWrap(Minecraft.getInstance().font,Component.translatable(key),6,124,208,0x404040);}
    }
    private static mezz.jei.api.gui.builder.IRecipeSlotBuilder slot(IRecipeLayoutBuilder b,RecipeIngredientRole role,int x,int y,String tooltip) {
        return b.addSlot(role,x-8,y-8).setStandardSlotBackground()
                .addRichTooltipCallback((view,t)->t.add(Component.translatable(tooltip)));
    }
    private static void grid(GuiGraphics g,int size,int step,int center) {
        int start=center-size*step/2,end=start+size*step;
        for(int i=0;i<=size;i++) {
            int v=start+i*step;g.fill(start,v,end+1,v+1,0xFFB6C8CA);g.fill(v,start,v+1,end+1,0xFFB6C8CA);
        }
    }
    private static void arrow(GuiGraphics g,int x,int y,int end) {
        g.fill(Math.min(x,end),y,Math.max(x,end),y+1,0xFF758C95);
        int sign=end>=x?1:-1;
        for(int i=0;i<5;i++)g.fill(end-sign*i,y-i,end-sign*i+1,y+i+1,0xFF758C95);
    }
    static void register(IRecipeCategoryRegistration registration,IGuiHelper gui) {
        var pages=java.util.List.<IRecipeCategory<?>>of(new Page<Corridor>(CORRIDOR,ModItems.UNSTABLE_SPACETIME_AGGREGATE.get(),"jei.omnira.ritual.corridor_title",gui) {
            public int getHeight(){return 220;}
            public void setRecipe(IRecipeLayoutBuilder b,Corridor r,IFocusGroup focus) {
                int index=0;
                for(var a:com.mcmagic.omnira.world.dimension.DreamRitual.ANCHORS)
                    slot(b,RecipeIngredientRole.CATALYST,60+a[0]*16,60+a[1]*16,"jei.omnira.ritual.offering").addItemStack(new ItemStack(com.mcmagic.omnira.spacetime.CorridorRitual.offering(index++)));
                slot(b,RecipeIngredientRole.CATALYST,60,60,"jei.omnira.ritual.advanced_core").addItemStack(new ItemStack(ModItems.ADVANCED_RITUAL_ENERGY_CORE.get()));
                slot(b,RecipeIngredientRole.INPUT,162,28,"jei.omnira.ritual.hand").addItemStack(new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()));
                slot(b,RecipeIngredientRole.CATALYST,150,98,"jei.omnira.ritual.pedestals").addItemStack(new ItemStack(ModItems.CRYSTAL_PEDESTAL.get(),8));
                slot(b,RecipeIngredientRole.OUTPUT,193,76,"jei.omnira.ritual.portal_result").addItemStack(new ItemStack(ModItems.CORRIDOR_GATEWAY.get()));
            }
            public void draw(Corridor r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                grid(g,7,16,60);arrow(g,132,76,177);note(g,"jei.omnira.ritual.corridor_steps");
            }
        },new Page<Affinity>(AFFINITY,ModItems.ADVANCED_RITUAL_ENERGY_CORE.get(),"jei.omnira.ritual.affinity_title",gui) {
            public void setRecipe(IRecipeLayoutBuilder b,Affinity r,IFocusGroup focus) {
                int index=0;
                for(var a:com.mcmagic.omnira.world.dimension.DreamRitual.ANCHORS)
                    slot(b,RecipeIngredientRole.CATALYST,60+a[0]*16,60+a[1]*16,"jei.omnira.ritual.offering").addItemStack(new ItemStack(com.mcmagic.omnira.world.dimension.AffinityRitual.ingredients().get(index++)));
                slot(b,RecipeIngredientRole.CATALYST,60,60,"jei.omnira.ritual.advanced_core").addItemStack(new ItemStack(ModItems.ADVANCED_RITUAL_ENERGY_CORE.get()));
                slot(b,RecipeIngredientRole.INPUT,162,28,"jei.omnira.ritual.hand").addItemStack(r.kind().offering());
                slot(b,RecipeIngredientRole.CATALYST,162,76,"jei.omnira.ritual.pedestals").addItemStack(new ItemStack(ModItems.CRYSTAL_PEDESTAL.get(),8));
            }
            public void draw(Affinity r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                grid(g,7,16,60);arrow(g,148,28,122);note(g,"jei.omnira.ritual.affinity_steps");
                g.drawCenteredString(Minecraft.getInstance().font,Component.translatable(r.kind().key()),165,100,0xFF514333);
            }
        },new Page<SwordShapingRecipe>(SWORD_SHAPING,ModItems.PURE_VESSEL.get(),"guide.omnira.sword_shaping",gui) {
            public void setRecipe(IRecipeLayoutBuilder b,SwordShapingRecipe r,IFocusGroup focus) {
                var anchors=com.mcmagic.omnira.world.dimension.SwordShapingRitual.ANCHORS;
                for(int i=0;i<4;i++)slot(b,RecipeIngredientRole.INPUT,56+anchors[i][0]*20,56+anchors[i][1]*20,"jei.omnira.ritual.offering").addIngredients(r.offerings().get(i));
                slot(b,RecipeIngredientRole.INPUT,56,56,"jei.omnira.ritual.vessel").addItemStack(new ItemStack(ModItems.PURE_VESSEL.get()));
                slot(b,RecipeIngredientRole.INPUT,160,24,"jei.omnira.ritual.weapon").addIngredients(Ingredient.of(com.mcmagic.omnira.block.entity.PureVesselBlockEntity.WEAPONS));
                slot(b,RecipeIngredientRole.CATALYST,130,96,"jei.omnira.ritual.pedestals").addItemStack(new ItemStack(ModItems.CRYSTAL_PEDESTAL.get(),4));
                slot(b,RecipeIngredientRole.OUTPUT,190,72,"jei.omnira.ritual.drop").addItemStack(r.result());
            }
            public void draw(SwordShapingRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                grid(g,5,20,56);arrow(g,146,24,114);arrow(g,116,72,176);note(g,"jei.omnira.ritual.sword_steps");
            }
        },new Page<VesselConversionRecipe>(VESSEL_CONVERSION,ModItems.PURE_VESSEL.get(),"jei.omnira.vessel_conversion",gui) {
            public void setRecipe(IRecipeLayoutBuilder b,VesselConversionRecipe r,IFocusGroup focus) {
                slot(b,RecipeIngredientRole.INPUT,38,32,"jei.omnira.ritual.hand").addIngredients(r.ingredient());
                slot(b,RecipeIngredientRole.INPUT,92,70,"jei.omnira.ritual.vessel").addItemStack(new ItemStack(ModItems.PURE_VESSEL.get()));
                slot(b,RecipeIngredientRole.OUTPUT,182,70,"jei.omnira.ritual.drop").addItemStack(r.result());
            }
            public void draw(VesselConversionRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                arrow(g,50,32,92);arrow(g,106,70,168);note(g,"jei.omnira.ritual.wood_steps");
            }
        },new Page<Resonance>(RESONANCE,ModItems.RESONANCE_TERMINAL.get(),"jei.omnira.ritual.resonance_title",gui) {
            public void setRecipe(IRecipeLayoutBuilder b,Resonance r,IFocusGroup focus) {
                for(var a:com.mcmagic.omnira.world.dimension.DreamRitual.ANCHORS)
                    slot(b,RecipeIngredientRole.CATALYST,60+a[0]*16,60+a[1]*16,"jei.omnira.ritual.offering").addItemStack(new ItemStack(ModItems.RESONANCE_CORE.get()));
                slot(b,RecipeIngredientRole.CATALYST,60,60,"jei.omnira.ritual.either_core").addIngredients(Ingredient.of(ModItems.RITUAL_ENERGY_CORE.get(),ModItems.ADVANCED_RITUAL_ENERGY_CORE.get()));
                slot(b,RecipeIngredientRole.INPUT,162,28,"jei.omnira.ritual.hand").addItemStack(new ItemStack(ModItems.SPACETIME_KNOT.get()));
                slot(b,RecipeIngredientRole.CATALYST,150,98,"jei.omnira.ritual.pedestals").addItemStack(new ItemStack(ModItems.CRYSTAL_PEDESTAL.get(),8));
                slot(b,RecipeIngredientRole.OUTPUT,193,76,"jei.omnira.ritual.drop").addItemStack(new ItemStack(ModItems.RESONANCE_TERMINAL.get()));
            }
            public void draw(Resonance r,IRecipeSlotsView slots,GuiGraphics g,double x,double y){grid(g,7,16,60);arrow(g,132,76,177);note(g,"jei.omnira.ritual.resonance_steps");}
        },new Page<Cleansing>(CLEANSING,ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),"jei.omnira.ritual.cleansing_title",gui) {
            public void setRecipe(IRecipeLayoutBuilder b,Cleansing r,IFocusGroup focus) {
                for(var a:com.mcmagic.omnira.world.dimension.DreamRitual.ANCHORS)
                    slot(b,RecipeIngredientRole.CATALYST,60+a[0]*16,60+a[1]*16,"jei.omnira.ritual.offering")
                            .addItemStack(new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
                slot(b,RecipeIngredientRole.CATALYST,60,60,"jei.omnira.ritual.either_core")
                        .addIngredients(Ingredient.of(ModItems.RITUAL_ENERGY_CORE.get(),ModItems.ADVANCED_RITUAL_ENERGY_CORE.get()));
                slot(b,RecipeIngredientRole.INPUT,162,28,"jei.omnira.ritual.hand")
                        .addItemStack(new ItemStack(ModItems.PARADOX_DUST.get()));
                slot(b,RecipeIngredientRole.CATALYST,150,98,"jei.omnira.ritual.pedestals")
                        .addItemStack(new ItemStack(ModItems.CRYSTAL_PEDESTAL.get(),8));
            }
            public void draw(Cleansing r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                grid(g,7,16,60);note(g,"jei.omnira.ritual.cleansing_steps");
            }
        },new Page<Dream>(DREAM,ModItems.RITUAL_ENERGY_CORE.get(),"jei.omnira.ritual.dream_title",gui) {
            public void setRecipe(IRecipeLayoutBuilder b,Dream r,IFocusGroup focus) {
                for(var a:com.mcmagic.omnira.world.dimension.DreamRitual.ANCHORS)
                    slot(b,RecipeIngredientRole.CATALYST,60+a[0]*16,60+a[1]*16,"jei.omnira.ritual.dream_crystal").addItemStack(new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
                slot(b,RecipeIngredientRole.CATALYST,60,60,"jei.omnira.ritual.either_core").addIngredients(Ingredient.of(ModItems.RITUAL_ENERGY_CORE.get(),ModItems.ADVANCED_RITUAL_ENERGY_CORE.get()));
                slot(b,RecipeIngredientRole.INPUT,162,28,"jei.omnira.ritual.hand").addIngredients(Ingredient.of(net.minecraft.tags.ItemTags.BEDS));
                slot(b,RecipeIngredientRole.CATALYST,150,98,"jei.omnira.ritual.pedestals").addItemStack(new ItemStack(ModItems.CRYSTAL_PEDESTAL.get(),8));
                slot(b,RecipeIngredientRole.OUTPUT,193,76,"jei.omnira.ritual.portal_result").addItemStack(new ItemStack(ModItems.DREAM_PORTAL.get()));
            }
            public void draw(Dream r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {grid(g,7,16,60);arrow(g,132,76,177);note(g,"jei.omnira.ritual.dream_steps");}
        });
        registration.addRecipeCategories(new IRecipeCategory<Entry>() {
            private final IDrawable icon=gui.createDrawableItemStack(new ItemStack(ModItems.RITUAL_ENERGY_CORE.get()));
            public RecipeType<Entry> getRecipeType(){return TYPE;}
            public Component getTitle(){return Component.translatable("jei.omnira.ritual.title");}
            public IDrawable getIcon(){return icon;}
            public int getWidth(){return 220;}
            public int getHeight(){return 220;}
            @SuppressWarnings({"rawtypes","unchecked"})
            public void setRecipe(IRecipeLayoutBuilder b,Entry entry,IFocusGroup focus) {
                ((IRecipeCategory)page(entry.ritual())).setRecipe(b,entry.ritual(),focus);
            }
            @SuppressWarnings({"rawtypes","unchecked"})
            public void draw(Entry entry,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
                var page=page(entry.ritual());
                ((IRecipeCategory)page).draw(entry.ritual(),slots,g,x,y);
                g.drawCenteredString(Minecraft.getInstance().font,page.getTitle(),110,202,0xFF514333);
            }
            private IRecipeCategory<?> page(Object ritual) {
                if(ritual instanceof Corridor)return pages.get(0);
                if(ritual instanceof Affinity)return pages.get(1);
                if(ritual instanceof SwordShapingRecipe)return pages.get(2);
                if(ritual instanceof VesselConversionRecipe)return pages.get(3);
                if(ritual instanceof Resonance)return pages.get(4);
                if(ritual instanceof Cleansing)return pages.get(5);
                return pages.get(6);
            }
        });
    }
}
