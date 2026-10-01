package com.mcmagic.omnira.time;

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

public final class DoomsdayRitePiece extends StructurePiece {
    public DoomsdayRitePiece(BlockPos origin){super(RuinsStructure.DOOMSDAY_PIECE.get(),0,
            new BoundingBox(origin.getX(),origin.getY(),origin.getZ(),origin.getX()+DoomsdayRitePlan.WIDTH-1,
                    origin.getY()+DoomsdayRitePlan.HEIGHT-1,origin.getZ()+DoomsdayRitePlan.DEPTH-1));}
    public DoomsdayRitePiece(StructurePieceSerializationContext context,CompoundTag tag){super(RuinsStructure.DOOMSDAY_PIECE.get(),tag);}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag){}
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,
            RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        var box=getBoundingBox();var origin=new BlockPos(box.minX(),box.minY(),box.minZ());
        for(var cell:DoomsdayRitePlan.cells()){
            var pos=cell.pos().offset(origin);
            if(clip.isInside(pos)){
                level.setBlock(pos,DoomsdayRitePlan.state(cell),2);
                var tag=DoomsdayRitePlan.treasure(cell,random.nextLong());
                if(!tag.isEmpty()&&level.getBlockEntity(pos) instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball){
                    ball.loadWithComponents(tag,level.registryAccess());ball.setChanged();
                }
            }
        }
    }
}
