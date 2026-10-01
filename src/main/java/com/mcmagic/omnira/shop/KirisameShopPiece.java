package com.mcmagic.omnira.shop;

import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mcmagic.omnira.spacetime.CorridorLayout;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class KirisameShopPiece extends StructurePiece {
    public KirisameShopPiece(BlockPos pos){super(RuinsStructure.KIRISAME_PIECE.get(),0,new BoundingBox(pos.getX(),pos.getY(),pos.getZ(),
            pos.getX()+KirisameShopPlan.WIDTH-1,pos.getY()+KirisameShopPlan.HEIGHT-1,pos.getZ()+KirisameShopPlan.DEPTH-1));}
    public KirisameShopPiece(StructurePieceSerializationContext context,CompoundTag tag){super(RuinsStructure.KIRISAME_PIECE.get(),tag);}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){}
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        var b=getBoundingBox();
        for(int x=b.minX();x<=b.maxX();x++)for(int z=b.minZ();z<=b.maxZ();z++){
            int ground=CorridorLayout.timeSurface(x,z);
            for(int y=Math.min(ground,b.minY());y<=b.maxY();y++){
                var pos=new BlockPos(x,y,z);if(!clip.isInside(pos))continue;
                level.setBlock(pos,y<=b.minY()?soil(pos):Blocks.AIR.defaultBlockState(),2);
            }
        }
        for(var cell:KirisameShopPlan.cells()){
            var pos=cell.pos().offset(b.minX(),b.minY(),b.minZ());if(!clip.isInside(pos))continue;
            level.setBlock(pos,cell.terrain()?soil(pos):KirisameShopPlan.state(cell),2);
            var tag=KirisameShopPlan.blockEntity(cell,pos,level.getSeed());
            if(tag!=null&&level.getBlockEntity(pos)!=null)level.getBlockEntity(pos).loadWithComponents(tag,level.registryAccess());
            if(cell.name().equals("omnira:marisa_crystal_ball")&&level.getBlockEntity(pos)!=null)
                com.mcmagic.omnira.world.structure.AuthoredStructureConnections.defer(level.getBlockEntity(pos),new BlockPos(b.minX(),b.minY(),b.minZ()),"kirisame");
        }
    }
    public static net.minecraft.world.level.block.state.BlockState soil(BlockPos pos){
        return (Math.floorMod(pos.asLong() ^ (pos.asLong() >>> 17),11)==0?TimeNatureContent.LIVING_SILT:TimeNatureContent.SILT).get().defaultBlockState();
    }
}
