package com.mcmagic.omnira.space.phase;

import com.google.gson.JsonParser;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.space.FoldedTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import java.util.*;

public final class PhaseGreenhousePiece extends StructurePiece {
    public static final List<KirisameShopPlan.Cell> CELLS=load();
    private final BlockPos origin;
    public PhaseGreenhousePiece(BlockPos p){super(RuinsStructure.PHASE_GREENHOUSE_PIECE.get(),0,new BoundingBox(p.getX()+1,24,p.getZ()+1,p.getX()+43,p.getY()+9,p.getZ()+17));origin=p.immutable();}
    public PhaseGreenhousePiece(StructurePieceSerializationContext c,CompoundTag tag){super(RuinsStructure.PHASE_GREENHOUSE_PIECE.get(),tag);origin=BlockPos.of(tag.getLong("Origin"));}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){tag.putLong("Origin",origin.asLong());}
    @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        for(var cell:CELLS){
            var p=origin.offset(cell.pos());if(!clip.isInside(p))continue;
            if(cell.pos().getY()==0)for(int y=FoldedTerrain.floor(p.getX(),p.getZ())+1;y<p.getY();y++){
                var below=new BlockPos(p.getX(),y,p.getZ());if(clip.isInside(below))level.setBlock(below,DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState(),2);
            }
            level.setBlock(p,KirisameShopPlan.state(cell),2);
            var nbt=KirisameShopPlan.blockEntity(cell,p,random.nextLong());
            if(nbt!=null&&level.getBlockEntity(p)!=null)level.getBlockEntity(p).loadWithComponents(nbt,level.registryAccess());
        }
    }
    private static List<KirisameShopPlan.Cell> load(){
        try(var in=Objects.requireNonNull(PhaseGreenhousePiece.class.getResourceAsStream("/data/omnira/structures/phase_greenhouse.json"));var reader=new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)){
            var cells=new ArrayList<KirisameShopPlan.Cell>();
            for(var raw:JsonParser.parseReader(reader).getAsJsonArray()){
                var c=raw.getAsJsonObject();var props=new HashMap<String,String>();
                if(c.has("properties"))c.getAsJsonObject("properties").entrySet().forEach(e->props.put(e.getKey(),e.getValue().getAsString()));
                cells.add(new KirisameShopPlan.Cell(new BlockPos(c.get("x").getAsInt(),c.get("y").getAsInt(),c.get("z").getAsInt()),c.get("name").getAsString(),props,"",List.of(),c.has("nbt")?c.get("nbt").getAsString():"",false));
            }
            cells.sort(Comparator.comparingInt(c->c.pos().getY()));return List.copyOf(cells);
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot load phase greenhouse",e);}
    }
}
