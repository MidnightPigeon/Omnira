package com.mcmagic.omnira.space;

import com.mcmagic.omnira.shop.KirisameShopPlan;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class FoldObservatoryPiece extends StructurePiece {
    private final BlockPos peer;private final boolean upper;
    public FoldObservatoryPiece(BlockPos o,BlockPos peer,boolean upper){super(RuinsStructure.FOLD_OBSERVATORY_PIECE.get(),0,new BoundingBox(o.getX(),o.getY(),o.getZ(),o.getX()+30,upper?103:o.getY()+8,o.getZ()+20));this.peer=peer;this.upper=upper;}
    public FoldObservatoryPiece(StructurePieceSerializationContext c,CompoundTag t){super(RuinsStructure.FOLD_OBSERVATORY_PIECE.get(),t);peer=BlockPos.of(t.getLong("Peer"));upper=t.getBoolean("Upper");}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){t.putLong("Peer",peer.asLong());t.putBoolean("Upper",upper);}
    @Override public void postProcess(WorldGenLevel l,StructureManager m,ChunkGenerator g,RandomSource r,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        var origin=new BlockPos(boundingBox.minX(),boundingBox.minY(),boundingBox.minZ());
        if(upper)for(int x:new int[]{3,15})for(int z:new int[]{4,16}){
            int roof=FoldedTerrain.roof(origin.getX()+x,origin.getZ()+z);
            for(int y=origin.getY()+9;y<roof;y++){
                var p=new BlockPos(origin.getX()+x,y,origin.getZ()+z);
                if(clip.isInside(p))l.setBlock(p,net.minecraft.world.level.block.Blocks.CHAIN.defaultBlockState(),2);
            }
        }
        if(!upper)for(int x:new int[]{3,15,22,28})for(int z:new int[]{6,14}){
            for(int y=origin.getY()-1;y>=FoldedTerrain.floor(origin.getX()+x,origin.getZ()+z);y--){
                var p=new BlockPos(origin.getX()+x,y,origin.getZ()+z);
                if(clip.isInside(p))l.setBlock(p,com.mcmagic.omnira.registry.DreamContent.ENGRAVED_MIRROR_ROCK.get().defaultBlockState(),2);
            }
        }
        for(var cell:FoldObservatoryPlan.cells(upper)){
            var p=origin.offset(cell.pos());if(!clip.isInside(p))continue;
            l.setBlock(p,KirisameShopPlan.state(cell),2);
            var tag=cell.pos().equals(FoldObservatoryPlan.GATE)?FoldObservatoryPlan.gateData(l.getLevel().dimension().location().toString(),p,peer,!upper):KirisameShopPlan.blockEntity(cell,p,l.getSeed());
            if(tag!=null&&l.getBlockEntity(p)!=null)l.getBlockEntity(p).loadWithComponents(tag,l.registryAccess());
        }
    }
}
