package com.mcmagic.omnira.myriad;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import com.mcmagic.omnira.myriad.MiniatureWorldBlockEntity.Stage;

/** Ordered alternatives belong to one evolution, shared by matching, hints and JEI. */
public record WorldDevelopmentSeries(ResourceLocation id,List<WorldDevelopmentRecipe> recipes) {
    private static final ResourceLocation CODEX=ResourceLocation.parse("omnira:myriad_codex");
    public static final int PANEL_COUNT=3;
    public static final double PANEL_WIDTH=4.3, PANEL_HEIGHT=1.1;
    public WorldDevelopmentSeries {recipes=List.copyOf(recipes);}
    public record Step(Stage from,Stage to,List<WorldDevelopmentRecipe> alternatives){
        public List<WorldDevelopmentRecipe.Material> materials(int alternative){return alternatives.isEmpty()?List.of():alternatives.get(alternative).materials();}
    }
    public List<Step> steps(){
        var steps=new ArrayList<Step>();
        for(var from:Stage.values()){
            var byTarget=new LinkedHashMap<Stage,List<WorldDevelopmentRecipe>>();
            for(var recipe:at(from))byTarget.computeIfAbsent(recipe.to()==Stage.RIFT?Stage.END_EMPTY:recipe.to(),k->new ArrayList<>()).add(recipe);
            byTarget.forEach((to,alternatives)->steps.add(new Step(from,to,List.copyOf(alternatives))));
            if(from.manualNext()!=null&&recipes.stream().anyMatch(r->r.to()==from))steps.add(new Step(from,from.manualNext(),List.of()));
        }
        return List.copyOf(steps);
    }
    public List<WorldDevelopmentRecipe> at(Stage stage){return recipes.stream().filter(r->r.from()==stage).toList();}
    public record Selection(int step,int alternative){}
    public Selection selectInputs(List<net.minecraft.world.item.ItemStack> inputs){
        var ordered=steps();
        for(int step=0;step<ordered.size();step++)for(int alternative=0;alternative<ordered.get(step).alternatives().size();alternative++){
            for(var material:ordered.get(step).materials(alternative))if(inputs.stream().anyMatch(material::test))return new Selection(step,alternative);
        }
        return new Selection(0,0);
    }
    public String key(){return "miniature."+id.getNamespace()+".series."+id.getPath().replace('/','.');}
    public static List<WorldDevelopmentSeries> collect(List<RecipeHolder<WorldDevelopmentRecipe>> holders){
        // Keep the two established hint panels in place; the codex fills the third panel.
        var grouped=new TreeMap<ResourceLocation,List<WorldDevelopmentRecipe>>(
                Comparator.comparing((ResourceLocation id)->id.equals(CODEX)).thenComparing(Comparator.naturalOrder()));
        holders.stream().sorted(Comparator.comparing(h->h.id().toString())).forEach(h->grouped.computeIfAbsent(h.value().series(),k->new ArrayList<>()).add(h.value()));
        return grouped.entrySet().stream().map(e->new WorldDevelopmentSeries(e.getKey(),e.getValue())).toList();
    }
    public static double panelY(int index){return 3.85-index*1.35;}
    /** Coordinates are relative to the controller, in the same face frame as the renderer. */
    public static int panelAt(Direction face,Vec3 hit){
        if(face.getAxis().isVertical())return -1;
        double x=switch(face){case NORTH->2.5-hit.x;case SOUTH->hit.x-2.5;case EAST->2.5-hit.z;case WEST->hit.z-2.5;default->0;};
        if(Math.abs(x)>PANEL_WIDTH/2)return -1;
        for(int i=0;i<PANEL_COUNT;i++)if(Math.abs(hit.y-panelY(i))<=PANEL_HEIGHT/2)return i;
        return -1;
    }
}
