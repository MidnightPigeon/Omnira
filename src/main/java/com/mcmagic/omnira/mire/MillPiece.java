package com.mcmagic.omnira.mire;

import com.mcmagic.omnira.shop.*;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class MillPiece extends StructurePiece {
    public MillPiece(BlockPos p){super(RuinsStructure.MILL_PIECE.get(),0,new BoundingBox(p.getX(),p.getY()+MillPlan.MIN_Y,p.getZ(),p.getX()+MillPlan.WIDTH-1,p.getY()+MillPlan.MAX_Y,p.getZ()+MillPlan.DEPTH-1));}
    public MillPiece(StructurePieceSerializationContext c,CompoundTag n){super(RuinsStructure.MILL_PIECE.get(),n);}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag n){}
    @Override public void postProcess(WorldGenLevel l,StructureManager m,ChunkGenerator g,RandomSource r,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        var b=getBoundingBox();var origin=new BlockPos(b.minX(),b.minY()-MillPlan.MIN_Y,b.minZ());
        for(var c:MillPlan.CELLS){
            var p=c.pos().offset(origin);if(!clip.isInside(p))continue;
            l.setBlock(p,KirisameShopPlan.state(c),2);var tag=KirisameShopPlan.blockEntity(c,p,l.getSeed());
            if(tag!=null&&l.getBlockEntity(p)!=null){
                if(!c.loot().isEmpty()){var custom=tag.getCompound("NeoForgeData");custom.remove(MillTreasures.TOKEN);custom.putLong(MillPlan.PENDING,origin.asLong());tag.put("NeoForgeData",custom);}
                l.getBlockEntity(p).loadWithComponents(tag,l.registryAccess());
            }
        }
    }
}
