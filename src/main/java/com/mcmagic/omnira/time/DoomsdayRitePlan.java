package com.mcmagic.omnira.time;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class DoomsdayRitePlan {
    public static final int WIDTH=33,DEPTH=33,HEIGHT=2;
    public record Cell(BlockPos pos,ResourceLocation block){}
    private static final List<Cell> CELLS=load();
    public static List<Cell> cells(){return CELLS;}
    public static BlockState state(Cell cell){return BuiltInRegistries.BLOCK.get(cell.block()).defaultBlockState();}
    public static net.minecraft.nbt.CompoundTag treasure(Cell cell,long seed){
        var tag=new net.minecraft.nbt.CompoundTag();
        if(cell.block().equals(ResourceLocation.parse("omnira:liquid_crystal_ball"))){
            tag.putBoolean("FluidTreasure",true);
        }else if(cell.block().equals(ResourceLocation.parse("omnira:crystal_ball"))){
            tag.putString("LootTable","omnira:chests/doomsday_rite");
            tag.putLong("LootTableSeed",seed);tag.putBoolean("LootContainer",true);
        }
        return tag;
    }
    private static List<Cell> load(){
        try(var reader=new InputStreamReader(Objects.requireNonNull(DoomsdayRitePlan.class.getResourceAsStream(
                "/data/omnira/structures/doomsday_rite_plan.json")),StandardCharsets.UTF_8)){
            var out=new ArrayList<Cell>();
            for(var value:JsonParser.parseReader(reader).getAsJsonArray()){
                var cell=value.getAsJsonObject();
                out.add(new Cell(new BlockPos(cell.get("x").getAsInt(),cell.get("y").getAsInt(),cell.get("z").getAsInt()),
                        ResourceLocation.parse(cell.get("name").getAsString())));
            }
            return List.copyOf(out);
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot load doomsday rite",error);}
    }
    private DoomsdayRitePlan(){}
}
