package com.mcmagic.omnira.space.abyss;

import com.google.gson.JsonParser;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import com.mcmagic.omnira.space.FoldedTerrain;
import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import java.util.*;

public final class SkyreachPiece extends StructurePiece {
    private final BlockPos origin;
    private final int height;
    public SkyreachPiece(BlockPos pos,int height){super(RuinsStructure.SKYREACH_PIECE.get(),0,new BoundingBox(pos.getX(),24,pos.getZ(),pos.getX()+20,102,pos.getZ()+20));origin=pos.immutable();this.height=height;}
    public SkyreachPiece(StructurePieceSerializationContext context,CompoundTag tag){super(RuinsStructure.SKYREACH_PIECE.get(),tag);origin=BlockPos.of(tag.getLong("Origin"));height=tag.getInt("Height");}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag){tag.putLong("Origin",origin.asLong());tag.putInt("Height",height);}
    public static List<KirisameShopPlan.Cell> cells(int height){
        // The initial design is parametric. A later player capture supersedes it, not vice versa.
        var resource=SkyreachPiece.class.getResourceAsStream("/data/omnira/structures/skyreach_tower.json");
        if(resource==null)return SkyreachPlan.build(height);
        try(var reader=new java.io.InputStreamReader(resource,java.nio.charset.StandardCharsets.UTF_8)){
            var result=new ArrayList<KirisameShopPlan.Cell>();
            for(var raw:JsonParser.parseReader(reader).getAsJsonArray()){
                var c=raw.getAsJsonObject();int y=c.get("y").getAsInt();
                // Stretch only the empty central crossing; preserve authored steps and galleries.
                if(y>SkyreachPlan.REFERENCE_HEIGHT/2)y+=height-SkyreachPlan.REFERENCE_HEIGHT;
                else if(y==SkyreachPlan.REFERENCE_HEIGHT/2)y=height/2;
                var props=new HashMap<String,String>();if(c.has("properties"))c.getAsJsonObject("properties").entrySet().forEach(e->props.put(e.getKey(),e.getValue().getAsString()));
                result.add(new KirisameShopPlan.Cell(new BlockPos(c.get("x").getAsInt(),y,c.get("z").getAsInt()),c.get("name").getAsString(),props,"",List.of(),c.has("nbt")?c.get("nbt").getAsString():"",false));
            }
            return List.copyOf(result);
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot read skyreach tower",e);}
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        for(var cell:cells(height)){
            var p=origin.offset(cell.pos());if(!clip.isInside(p))continue;
            level.setBlock(p,KirisameShopPlan.state(cell),2);
            var nbt=KirisameShopPlan.blockEntity(cell,p,random.nextLong());
            if(nbt!=null&&level.getBlockEntity(p)!=null)level.getBlockEntity(p).loadWithComponents(nbt,level.registryAccess());
            if(cell.pos().getY()==0||cell.pos().getY()==height){
                int surface=cell.pos().getY()==0?FoldedTerrain.floor(p.getX(),p.getZ()):FoldedTerrain.roof(p.getX(),p.getZ());
                for(int y=Math.min(surface,p.getY());y<=Math.max(surface,p.getY());y++){
                    var fill=new BlockPos(p.getX(),y,p.getZ());if(clip.isInside(fill))level.setBlock(fill,DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState(),2);
                }
            }
        }
    }
}
