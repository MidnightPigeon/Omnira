package com.mcmagic.omnira.space;

import com.google.gson.JsonParser;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

public final class FoldObservatoryPlan {
    public static final int WIDTH=31,DEPTH=21,HEIGHT=9;
    public static final BlockPos GATE=new BlockPos(9,1,10);
    private static final List<KirisameShopPlan.Cell> LOWER=load(false),UPPER=load(true);
    private FoldObservatoryPlan(){}
    public static List<KirisameShopPlan.Cell> cells(boolean upper){return upper?UPPER:LOWER;}
    private static List<KirisameShopPlan.Cell> load(boolean upper){
        try(var in=Objects.requireNonNull(FoldObservatoryPlan.class.getResourceAsStream("/data/omnira/structures/fold_observatory_"+(upper?"upper":"lower")+".json"));
            var reader=new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)){
            var out=new ArrayList<KirisameShopPlan.Cell>();
            for(var raw:JsonParser.parseReader(reader).getAsJsonArray()){
                var c=raw.getAsJsonObject();var props=new HashMap<String,String>();
                if(c.has("properties"))c.getAsJsonObject("properties").entrySet().forEach(e->props.put(e.getKey(),e.getValue().getAsString()));
                out.add(new KirisameShopPlan.Cell(new BlockPos(c.get("x").getAsInt(),c.get("y").getAsInt(),c.get("z").getAsInt()),c.get("name").getAsString(),props,c.has("loot")?c.get("loot").getAsString():"",List.of(),"",false));
            }
            out.sort(Comparator.comparingInt(c->c.pos().getY()));return List.copyOf(out);
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot load fold observatory",e);}
    }
    public static UUID identity(String dimension,BlockPos pos){return UUID.nameUUIDFromBytes((dimension+":"+pos.asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    public static CompoundTag gateData(String dimension,BlockPos pos,BlockPos peer,boolean white){
        var t=new CompoundTag();t.putString("id","omnira:fold_gate");t.putInt("x",pos.getX());t.putInt("y",pos.getY());t.putInt("z",pos.getZ());
        t.putUUID("Identity",identity(dimension,pos));t.putUUID("PeerIdentity",identity(dimension,peer));t.putString("PeerDimension",dimension);t.putLong("PeerPos",peer.asLong());t.putBoolean("White",white);return t;
    }
}
