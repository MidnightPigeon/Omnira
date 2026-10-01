package com.mcmagic.omnira.archaeology;

import com.google.gson.*;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import java.util.*;

public final class DrillingPlatformPiece extends StructurePiece {
    public static final List<KirisameShopPlan.Cell> CELLS=load();
    private static List<KirisameShopPlan.Cell> load(){
        try(var in=new java.io.InputStreamReader(Objects.requireNonNull(DrillingPlatformPiece.class.getResourceAsStream("/data/omnira/structures/drilling_platform_plan.json")),java.nio.charset.StandardCharsets.UTF_8)){
            var result=new ArrayList<KirisameShopPlan.Cell>();
            for(var element:JsonParser.parseReader(in).getAsJsonArray()){
                var j=element.getAsJsonObject();Map<String,String> props=new HashMap<>();
                if(j.has("properties"))j.getAsJsonObject("properties").entrySet().forEach(p->props.put(p.getKey(),p.getValue().getAsString()));
                result.add(new KirisameShopPlan.Cell(new BlockPos(j.get("x").getAsInt(),j.get("y").getAsInt(),j.get("z").getAsInt()),j.get("name").getAsString(),props,j.has("loot")?j.get("loot").getAsString():"",List.of(),j.has("nbt")?j.get("nbt").getAsString():"",false));
            }return List.copyOf(result);
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    public DrillingPlatformPiece(BlockPos p){super(RuinsStructure.DRILL_PLATFORM_PIECE.get(),0,new BoundingBox(p.getX(),p.getY(),p.getZ(),p.getX()+16,p.getY()+14,p.getZ()+16));}
    public DrillingPlatformPiece(StructurePieceSerializationContext c,CompoundTag n){super(RuinsStructure.DRILL_PLATFORM_PIECE.get(),n);}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag n){}
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        var b=getBoundingBox();var origin=new BlockPos(b.minX(),b.minY(),b.minZ());
        for(var cell:CELLS){var p=cell.pos().offset(origin);if(!clip.isInside(p))continue;
            level.setBlock(p,KirisameShopPlan.state(cell),2);
            var tag=KirisameShopPlan.blockEntity(cell,p,level.getSeed());
            if(tag!=null&&level.getBlockEntity(p)!=null)level.getBlockEntity(p).loadWithComponents(tag,level.registryAccess());
        }
    }
}
