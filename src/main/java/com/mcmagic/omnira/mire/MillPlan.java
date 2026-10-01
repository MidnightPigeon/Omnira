package com.mcmagic.omnira.mire;

import com.google.gson.*;
import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Uses the captured player's block-state/NBT blueprint, never the initial layout generator. */
public final class MillPlan {
    public static final int WIDTH=49,DEPTH=45,MIN_Y=-2,MAX_Y=24;
    public static final String PENDING="OmniraMillOrigin";
    public static final JsonObject PLAN=read("rusty_lake_mill_plan").getAsJsonObject();
    public static final JsonArray SITES=read("rusty_lake_mill_treasures").getAsJsonArray();
    public static final List<KirisameShopPlan.Cell> CELLS=load();
    public static final int BUILDING_MIN_X=CELLS.stream().mapToInt(c->c.pos().getX()).min().orElseThrow();
    public static final int BUILDING_MAX_X=CELLS.stream().mapToInt(c->c.pos().getX()).max().orElseThrow();
    public static final int BUILDING_MIN_Z=CELLS.stream().mapToInt(c->c.pos().getZ()).min().orElseThrow();
    public static final int BUILDING_MAX_Z=CELLS.stream().mapToInt(c->c.pos().getZ()).max().orElseThrow();
    private static JsonElement read(String name){
        try(var r=new InputStreamReader(Objects.requireNonNull(MillPlan.class.getResourceAsStream("/data/omnira/structures/"+name+".json")),StandardCharsets.UTF_8)){return JsonParser.parseReader(r);}
        catch(IOException e){throw new IllegalStateException(e);}
    }
    private static List<KirisameShopPlan.Cell> load(){
        var cells=new ArrayList<KirisameShopPlan.Cell>();
        for(var raw:PLAN.getAsJsonArray("cells")){
            var j=raw.getAsJsonObject();
            if(!j.has("part")||!j.get("part").getAsString().equals("building"))continue;
            var props=new HashMap<String,String>();
            if(j.has("properties"))j.getAsJsonObject("properties").entrySet().forEach(e->props.put(e.getKey(),e.getValue().getAsString()));
            cells.add(new KirisameShopPlan.Cell(new BlockPos(j.get("x").getAsInt(),j.get("y").getAsInt(),j.get("z").getAsInt()),j.get("name").getAsString(),Map.copyOf(props),j.has("loot")?j.get("loot").getAsString():"",List.of(),j.has("nbt")?j.get("nbt").getAsString():"",false));
        }
        cells.sort(Comparator.comparingInt(c->c.pos().getY()));return List.copyOf(cells);
    }
    public static List<MillTreasures.Site> sites(BlockPos origin){
        var out=new ArrayList<MillTreasures.Site>();for(var raw:SITES){var j=raw.getAsJsonObject();out.add(new MillTreasures.Site(origin.offset(j.get("x").getAsInt(),j.get("y").getAsInt(),j.get("z").getAsInt()),j.get("reversed").getAsBoolean(),j.get("loot").getAsString()));}return out;
    }
    public static void initialize(CrystalBallBlockEntity ball){
        var data=ball.getPersistentData();if(!data.contains(PENDING)||!(ball.getLevel() instanceof ServerLevel level))return;
        var origin=BlockPos.of(data.getLong(PENDING));
        if(sites(origin).stream().noneMatch(site->site.pos().equals(ball.getBlockPos())))return;
        for(int x=origin.getX()>>4;x<=(origin.getX()+WIDTH-1)>>4;x++)for(int z=origin.getZ()>>4;z<=(origin.getZ()+DEPTH-1)>>4;z++)if(!level.hasChunk(x,z))return;
        if(MillTreasures.get(level).register(level,origin,sites(origin))){
            // Chunk-wise placement can overwrite connections resolved by an earlier neighbour.
            // Reconcile existing blocks only after all structure chunks are fully available.
            for(var cell:CELLS){
                var p=cell.pos().offset(origin);var state=level.getBlockState(p);var block=state.getBlock();
                if(block instanceof net.minecraft.world.level.block.StairBlock||block instanceof net.minecraft.world.level.block.FenceBlock
                        ||block instanceof net.minecraft.world.level.block.WallBlock||block instanceof net.minecraft.world.level.block.IronBarsBlock){
                    var connected=net.minecraft.world.level.block.Block.updateFromNeighbourShapes(state,level,p);
                    if(connected!=state)level.setBlock(p,connected,2);
                }
            }
        }
        // A token proves registration, including reloads; a failed first attempt remains retryable.
        if(data.hasUUID(MillTreasures.TOKEN)){data.remove(PENDING);ball.setChanged();}
    }
}
