package com.mcmagic.omnira.shop;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class KirisameShopPlan {
    public static final int WIDTH=31,DEPTH=29,HEIGHT=22;
    public record Cell(BlockPos pos,String name,Map<String,String> properties,String loot,List<String> sign,String nbt,boolean terrain){}
    private static final List<Cell> CELLS=load();
    private KirisameShopPlan(){}
    private static List<Cell> load(){
        try(var stream=Objects.requireNonNull(KirisameShopPlan.class.getResourceAsStream("/data/omnira/structures/kirisame_shop_plan.json"));
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            List<Cell> result=new ArrayList<>();
            for(var e:JsonParser.parseReader(reader).getAsJsonArray()){
                var j=e.getAsJsonObject();Map<String,String> props=new HashMap<>();
                if(j.has("properties"))j.getAsJsonObject("properties").entrySet().forEach(p->props.put(p.getKey(),p.getValue().getAsString()));
                List<String> sign=new ArrayList<>();if(j.has("sign"))j.getAsJsonArray("sign").forEach(line->sign.add(line.getAsString()));
                result.add(new Cell(new BlockPos(j.get("x").getAsInt(),j.get("y").getAsInt(),j.get("z").getAsInt()),
                        j.get("name").getAsString(),Map.copyOf(props),j.has("loot")?j.get("loot").getAsString():"",List.copyOf(sign),
                        j.has("nbt")?j.get("nbt").getAsString():"",j.has("terrain")&&j.get("terrain").getAsBoolean()));
            }
            result.sort(Comparator.comparingInt(c->c.pos().getY()));return List.copyOf(result);
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot read Kirisame shop",e);}
    }
    public static List<Cell> cells(){return CELLS;}
    public static BlockState state(Cell cell){
        var block=BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(cell.name)).orElseThrow();
        BlockState state=block.defaultBlockState();
        for(var prop:cell.properties.entrySet())state=set(state,block.getStateDefinition().getProperty(prop.getKey()),prop.getValue());
        return state;
    }
    private static <T extends Comparable<T>> BlockState set(BlockState state,Property<T> property,String value){
        if(property==null)throw new IllegalArgumentException("Missing property on "+state);
        return state.setValue(property,property.getValue(value).orElseThrow());
    }
    public static CompoundTag blockEntity(Cell cell,BlockPos pos,long seed){
        var tag=new CompoundTag();
        if(!cell.nbt.isEmpty()) {
            try {tag=net.minecraft.nbt.TagParser.parseTag(cell.nbt);}
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException("Invalid shop block entity",e);}
        }
        if(!cell.loot.isEmpty()){
            tag.putString("id","omnira:crystal_ball");tag.putString("LootTable",cell.loot);tag.putLong("LootTableSeed",seed^pos.asLong());
        }else if(!cell.sign.isEmpty()){
            tag.putString("id","minecraft:sign");var front=new CompoundTag();var messages=new net.minecraft.nbt.ListTag();
            for(String line:cell.sign)messages.add(net.minecraft.nbt.StringTag.valueOf(new Gson().toJson(Map.of("text",line))));
            front.put("messages",messages);front.putString("color","white");front.putBoolean("has_glowing_text",true);tag.put("front_text",front);
        }else if(cell.name.equals("omnira:marisa_crystal_ball"))tag.putString("id",cell.name);
        else if(tag.isEmpty())return null;
        tag.putInt("x",pos.getX());tag.putInt("y",pos.getY());tag.putInt("z",pos.getZ());return tag;
    }
}
