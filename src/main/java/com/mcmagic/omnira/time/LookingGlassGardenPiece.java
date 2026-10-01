package com.mcmagic.omnira.time;

import com.mcmagic.omnira.shop.KirisameShopPlan;
import com.mcmagic.omnira.world.structure.RuinsStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class LookingGlassGardenPiece extends StructurePiece {
    public LookingGlassGardenPiece(BlockPos origin){super(RuinsStructure.GARDEN_PIECE.get(),0,
            new BoundingBox(origin.getX(),origin.getY(),origin.getZ(),origin.getX()+LookingGlassGardenPlan.WIDTH-1,
                    origin.getY()+LookingGlassGardenPlan.HEIGHT-1,origin.getZ()+LookingGlassGardenPlan.DEPTH-1));}
    public LookingGlassGardenPiece(StructurePieceSerializationContext context,CompoundTag tag){super(RuinsStructure.GARDEN_PIECE.get(),tag);}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag){}

    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,
            RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        var box=getBoundingBox();var origin=new BlockPos(box.minX(),box.minY(),box.minZ());
        for(var cell:LookingGlassGardenPlan.cells()){
            var pos=cell.pos().offset(origin);
            if(!clip.isInside(pos))continue;
            level.setBlock(pos,KirisameShopPlan.state(cell),2);
            var tag=KirisameShopPlan.blockEntity(cell,pos,level.getSeed());
            if(tag!=null&&level.getBlockEntity(pos)!=null)
                level.getBlockEntity(pos).loadWithComponents(tag,level.registryAccess());
        }
    }
}
