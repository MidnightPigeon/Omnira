package com.mcmagic.omnira.time;

import com.google.gson.JsonParser;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/** Only authored paths and fixtures; biome terrain and vegetation remain natural. */
public final class LookingGlassGardenPlan {
    public static final int WIDTH=41,DEPTH=41,HEIGHT=5;
    private static final List<KirisameShopPlan.Cell> CELLS=load();
    private LookingGlassGardenPlan(){}

    public static List<KirisameShopPlan.Cell> cells(){return CELLS;}

    private static List<KirisameShopPlan.Cell> load(){
        try(var stream=Objects.requireNonNull(LookingGlassGardenPlan.class.getResourceAsStream(
                "/data/omnira/structures/looking_glass_garden_plan.json"));
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            var cells=new ArrayList<KirisameShopPlan.Cell>();
            for(var entry:JsonParser.parseReader(reader).getAsJsonArray()){
                var cell=entry.getAsJsonObject();var properties=new HashMap<String,String>();
                if(cell.has("properties"))cell.getAsJsonObject("properties").entrySet().forEach(
                        property->properties.put(property.getKey(),property.getValue().getAsString()));
                cells.add(new KirisameShopPlan.Cell(new BlockPos(cell.get("x").getAsInt(),cell.get("y").getAsInt(),
                        cell.get("z").getAsInt()),cell.get("name").getAsString(),Map.copyOf(properties),
                        cell.has("loot")?cell.get("loot").getAsString():"",List.of(),"",false));
            }
            cells.sort(Comparator.comparingInt(c->c.pos().getY()));
            return List.copyOf(cells);
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot read looking-glass garden",e);}
    }
}
