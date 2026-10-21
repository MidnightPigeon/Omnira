package com.mcmagic.omnira.compat.jei;

import java.util.*;
import com.mcmagic.omnira.myriad.*;
import com.mcmagic.omnira.myriad.MiniatureWorldBlockEntity.Stage;
import com.mcmagic.omnira.myriad.WorldDevelopmentSeries.Step;
import com.mcmagic.omnira.client.MiniatureWorldClient;
import com.mojang.math.Axis;
import mezz.jei.api.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.gui.widgets.*;
import mezz.jei.api.gui.inputs.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.*;
import net.minecraft.client.renderer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class MiniatureWorldJei implements IModPlugin {
    public record Display(WorldDevelopmentSeries series,List<Step> steps){}
    private static final RecipeType<Display> TYPE=RecipeType.create("omnira","world_development",Display.class);
    public ResourceLocation getPluginUid(){return ResourceLocation.parse("omnira:world_development");}
    public static Display display(WorldDevelopmentSeries series){
        return new Display(series,series.steps());
    }
    public void registerCategories(IRecipeCategoryRegistration r){
        var icon=r.getJeiHelpers().getGuiHelper().createDrawableItemStack(new ItemStack(MiniatureWorldContent.ITEM.get()));
        r.addRecipeCategories(new mezz.jei.api.recipe.category.IRecipeCategory<Display>(){
            private final Map<mezz.jei.api.gui.ingredient.IRecipeSlotDrawable,java.lang.ref.WeakReference<EvolutionView>> views=new WeakHashMap<>();
            public RecipeType<Display> getRecipeType(){return TYPE;}
            public Component getTitle(){return Component.translatable("jei.omnira.world_development");}
            public mezz.jei.api.gui.drawable.IDrawable getIcon(){return icon;}
            public int getWidth(){return EvolutionNavigation.WIDTH;}
            public int getHeight(){return EvolutionNavigation.HEIGHT;}
            public ResourceLocation getRegistryName(Display d){return d.series().id();}
            public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder b,Display d,IFocusGroup focus){
                // Index the entire evolution, including alternative intermediate inputs.
                for(var recipe:d.series().recipes())for(var material:recipe.materials())b.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(material.examples());
                int slots=d.series().recipes().stream().mapToInt(v->v.materials().size()).max().orElse(0);
                for(int i=0;i<slots;i++){
                    var examples=new ArrayList<ItemStack>();
                    for(var recipe:d.series().recipes())if(i<recipe.materials().size())examples.addAll(recipe.materials().get(i).examples());
                    b.addSlot(RecipeIngredientRole.INPUT,0,116).setSlotName("material"+i).setStandardSlotBackground().addItemStacks(examples);
                }
                var products=d.series().recipes().stream().map(v->v.getResultItem(null)).filter(s->!s.isEmpty()).toList();
                if(!products.isEmpty())b.addSlot(RecipeIngredientRole.OUTPUT,148,64).setStandardSlotBackground().addItemStacks(products);
            }
            public void createRecipeExtras(IRecipeExtrasBuilder b,Display d,IFocusGroup focus){
                var view=new EvolutionView(d,b.getRecipeSlots(),focus);b.addWidget(view);b.addInputHandler(view);
                b.getRecipeSlots().findSlotByName("material0").ifPresent(slot->views.put(slot,new java.lang.ref.WeakReference<>(view)));
            }
            @Override public void onDisplayedIngredientsUpdate(Display d,List<mezz.jei.api.gui.ingredient.IRecipeSlotDrawable> slots,IFocusGroup focus){
                // JEI clears overrides on refresh; restore this layout's current stage, not the series-wide index.
                for(var slot:slots){
                    var reference=views.get(slot);var view=reference==null?null:reference.get();
                    if(view!=null){view.updateSlots();return;}
                }
            }
        });
    }
    /** Navigation is scoped to one JEI layout, never shared between open recipes. */
    private static final class EvolutionView implements IRecipeWidget,IJeiInputHandler {
        private final Display display;
        private final mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView slots;
        private final List<net.minecraft.client.gui.components.Button> buttons=new ArrayList<>();
        private int stage,alternative;
        EvolutionView(Display display,mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView slots,IFocusGroup focus){
            this.display=display;this.slots=slots;
            var selected=display.series().selectInputs(focus.getItemStackFocuses(RecipeIngredientRole.INPUT).map(f->f.getTypedValue().getIngredient()).toList());
            stage=selected.step();alternative=selected.alternative();
            for(boolean stages:new boolean[]{true,false})for(int direction:new int[]{-1,1}){
                var area=EvolutionNavigation.button(stages,direction);
                buttons.add(net.minecraft.client.gui.components.Button.builder(Component.literal(direction<0?"<":">"),button->navigate(stages,direction))
                        .bounds(area.x(),area.y(),area.width(),area.height()).build());
            }
            updateSlots();
        }
        public ScreenPosition getPosition(){return new ScreenPosition(0,0);}
        public ScreenRectangle getArea(){return new ScreenRectangle(0,0,EvolutionNavigation.WIDTH,EvolutionNavigation.HEIGHT);}
        private Step step(){return display.steps().get(stage);}
        private void updateSlots(){
            var inputs=step().materials(alternative);
            int count=display.series().recipes().stream().mapToInt(r->r.materials().size()).max().orElse(0);
            for(int i=0;i<count;i++){
                var slot=slots.findSlotByName("material"+i).orElseThrow();
                slot.clearDisplayOverrides();
                slot.createDisplayOverrides().addItemStacks(i<inputs.size()?inputs.get(i).examples():List.of());
                slot.setPosition(i<inputs.size()?(EvolutionNavigation.WIDTH-inputs.size()*19)/2+i*19:-1000,116);
            }
            for(int i=0;i<buttons.size();i++)buttons.get(i).active=i<2?display.steps().size()>1:step().alternatives().size()>1;
        }
        private void navigate(boolean stages,int direction){
            if(stages){stage=Math.floorMod(stage+direction,display.steps().size());alternative=0;}
            else alternative=Math.floorMod(alternative+direction,step().alternatives().size());
            updateSlots();
        }
        public boolean handleInput(double x,double y,IJeiUserInput input){
            if(input.getKey().getType()!=com.mojang.blaze3d.platform.InputConstants.Type.MOUSE||input.getKey().getValue()!=0)return false;
            for(var button:buttons)if(button.active&&button.isMouseOver(x,y)){
                if(!input.isSimulate())button.mouseClicked(x,y,0);
                return true;
            }
            return false;
        }
        public void drawWidget(net.minecraft.client.gui.GuiGraphics g,double mx,double my){
            var mc=Minecraft.getInstance();var pose=g.pose();var buffers=mc.renderBuffers().bufferSource();
            double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
            var step=step();
            centered(g,Component.translatable(display.series().key()),0);
            centered(g,Component.translatable(step.from().key()).append(" -> ").append(Component.translatable(step.to().key())),13);
            g.flush();com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();pose.pushPose();
            pose.translate(87,66,160);pose.scale(12,-12,12);pose.mulPose(Axis.XP.rotationDegrees(25));pose.mulPose(Axis.YP.rotationDegrees(-35+(float)Math.sin(time*.008)*18));pose.translate(-2.5,-2.5,-2.5);
            MiniatureWorldClient.draw(null,step.to().completion()!=null?step.to().completion():step.to(),time,pose,buffers,LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            buffers.endBatch();pose.popPose();com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            navigation(g,(stage+1)+"/"+display.steps().size(),100);
            navigation(g,step.alternatives().isEmpty()?Component.translatable("miniature.omnira.enter").getString():(alternative+1)+"/"+step.alternatives().size(),139);
            buttons.forEach(button->button.render(g,(int)mx,(int)my,0));
        }
        @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip,double x,double y){
            for(int i=0;i<buttons.size();i++)if(buttons.get(i).active&&buttons.get(i).isMouseOver(x,y)){
                tooltip.add(Component.translatable("jei.omnira.evolution."+(i<2?"stage":"variant")+"_"+(i%2==0?"previous":"next")));return;
            }
        }
        private void navigation(net.minecraft.client.gui.GuiGraphics g,String label,int y){
            var font=Minecraft.getInstance().font;g.drawCenteredString(font,label,87,y,0xFF506577);
        }
        private void centered(net.minecraft.client.gui.GuiGraphics g,Component label,int y){
            var font=Minecraft.getInstance().font;float scale=Math.min(1,170F/Math.max(1,font.width(label)));
            g.pose().pushPose();g.pose().translate(87,y,0);g.pose().scale(scale,scale,scale);g.drawCenteredString(font,label,0,0,0xFF506577);g.pose().popPose();
        }
    }
    public void registerRecipes(IRecipeRegistration r){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        r.addRecipes(TYPE,WorldDevelopmentSeries.collect(mc.level.getRecipeManager().getAllRecipesFor(MiniatureWorldContent.TYPE.get())).stream().map(MiniatureWorldJei::display).toList());
    }
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r){r.addRecipeCatalyst(new ItemStack(MiniatureWorldContent.ITEM.get()),TYPE);}
}
