package com.mcmagic.omnira.world.structure;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** One persisted layout; every chunk uses the same rooms, openings and optional furniture. */
public final class ShadowLibraryPiece extends StructurePiece {
    private final BlockPos origin;
    private final int surface;
    private final long seed;
    public ShadowLibraryPiece(BlockPos origin,int surface,long seed) {
        super(RuinsStructure.LIBRARY_PIECE.get(),0,new ShadowLibraryLayout(seed).bounds(origin,surface));
        this.origin=origin;this.surface=surface;this.seed=seed;
    }
    public ShadowLibraryPiece(StructurePieceSerializationContext context,CompoundTag tag) {
        super(RuinsStructure.LIBRARY_PIECE.get(),tag);
        origin=BlockPos.of(tag.getLong("Origin"));surface=tag.getInt("Surface");seed=tag.getLong("LayoutSeed");
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        tag.putLong("Origin",origin.asLong());tag.putInt("Surface",surface);tag.putLong("LayoutSeed",seed);
    }
    public ShadowLibraryLayout layout() {return new ShadowLibraryLayout(seed);}
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,
                                     BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        var layout=layout();var brick=DreamContent.SHADOW_ROCK_BRICKS.get().defaultBlockState();
        for(int index=0;index<layout.rooms.size();index++) {
            var room=layout.rooms.get(index);var pos=origin.offset(room.origin());
            var template=level.getLevel().getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("omnira","authored/shadow_library/"+room.kind()));
            var settings=new StructurePlaceSettings().setBoundingBox(clip).setIgnoreEntities(true)
                    .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).setRandom(RandomSource.create(seed+index));
            template.placeInWorld(level,pos,pos,settings,RandomSource.create(seed+index),2);
            // Separate balls retain independent loot even when the room straddles chunks.
            for(var cell:template.filterBlocks(pos,settings,ModBlocks.CRYSTAL_BALL.get()))
                if(clip.isInside(cell.pos()) && level.getBlockEntity(cell.pos()) instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball)
                    ball.setLootTableSeed(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed^cell.pos().asLong()));
            if(room.kind().equals("stacks") && index!=0) {
                for(int y=1;y<=3;y++)put(level,clip,pos.offset(1,y,3),Blocks.AIR.defaultBlockState());
                put(level,clip,pos.offset(1,4,3),brick);
            }
            if(room.kind().equals("research")) {
                if(!layout.analysisTable)put(level,clip,pos.offset(3,1,5),Blocks.AIR.defaultBlockState());
                if(!layout.enchantingTable)put(level,clip,pos.offset(7,1,5),Blocks.AIR.defaultBlockState());
            }
            if(room.kind().equals("sealed") && layout.waymark) {
                var mark=ModBlocks.WAYMARK.get().defaultBlockState();
                put(level,clip,pos.offset(2,1,4),mark);
                put(level,clip,pos.offset(2,2,4),mark.setValue(com.mcmagic.omnira.block.WaymarkBlock.HALF,DoubleBlockHalf.UPPER));
            }
        }
        // Cut doors through both the wall and the inner bookshelf row, after all templates.
        for(var room:layout.rooms) {
            if(room.parent()<0)continue;
            var parent=layout.rooms.get(room.parent());
            int dx=Integer.signum(room.x()-parent.x()),dz=Integer.signum(room.z()-parent.z());
            for(int t=parent.half()-1;t<=16-room.half()+1;t++)for(int side=-2;side<=2;side++)for(int y=0;y<=4;y++) {
                var pos=origin.offset(parent.x()*16+dx*t+dz*side,y,parent.z()*16+dz*t+dx*side);
                boolean outside=t>parent.half() && t<16-room.half();
                if(outside || Math.abs(side)<2 && y>0 && y<4)
                    put(level,clip,pos,y==0 || y==4 || Math.abs(side)==2?brick:Blocks.AIR.defaultBlockState());
            }
            var lamp=origin.offset(parent.x()*16+dx*8,3,parent.z()*16+dz*8);
            put(level,clip,lamp,DreamContent.SHADOW_LANTERN.get().defaultBlockState().setValue(LanternBlock.HANGING,true));
        }
        ShadowLibraryEntrance.plan(origin.offset(-3,4,-1),surface).forEach((p,state)->put(level,clip,p,state));
    }
    private static void put(WorldGenLevel level,BoundingBox clip,BlockPos pos,BlockState state) {
        if(clip.isInside(pos))level.setBlock(pos,state,2);
    }
}
