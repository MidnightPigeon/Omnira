package com.mcmagic.omnira.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.spacetime.CorridorLayout;
import com.mcmagic.omnira.registry.DreamContent;

public final class FrozenTerraPalacePiece extends StructurePiece {
    private static final ResourceKey<LootTable> TOWER_LOOT=ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("omnira","chests/golden_throne_tower"));
    public FrozenTerraPalacePiece(BlockPos origin){
        super(RuinsStructure.TERRA_PALACE_PIECE.get(),0,new BoundingBox(origin.getX(),origin.getY(),origin.getZ(),
                origin.getX()+FrozenTerraPalacePlan.WIDTH-1,origin.getY()+FrozenTerraPalacePlan.HEIGHT-1,
                origin.getZ()+FrozenTerraPalacePlan.DEPTH-1));
    }
    public FrozenTerraPalacePiece(StructurePieceSerializationContext context,CompoundTag tag){
        super(RuinsStructure.TERRA_PALACE_PIECE.get(),tag);
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag){}
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,
                                      RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        var box=getBoundingBox();
        for(int x=box.minX();x<=box.maxX();x++)for(int z=box.minZ();z<=box.maxZ();z++){
            int ground=CorridorLayout.timeSurface(x,z);
            for(int y=ground+1;y<box.minY();y++){
                BlockPos fill=new BlockPos(x,y,z);
                if(clip.isInside(fill))level.setBlock(fill,DreamContent.TEMPORAL_SILT.get().defaultBlockState(),2);
            }
        }
        FrozenTerraPalacePlan.create().entrySet().stream()
                .sorted(java.util.Comparator.comparingInt(entry->entry.getKey().getY()))
                .forEach(entry->{
            var local=entry.getKey();var cell=entry.getValue();
            BlockPos world=new BlockPos(box.minX()+local.getX(),box.minY()+local.getY(),box.minZ()+local.getZ());
            if(!clip.isInside(world))return;
            var state=BuiltInRegistries.BLOCK.get(ResourceLocation.parse(cell.name())).defaultBlockState();
            if(!cell.half().isEmpty())state=state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                    DoubleBlockHalf.valueOf(cell.half().toUpperCase(java.util.Locale.ROOT)));
            if(!cell.facing().isEmpty())state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    Direction.byName(cell.facing()));
            else if(cell.name().equals("omnira:golden_throne"))
                state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH);
            level.setBlock(world,state,2);
            if(cell.name().equals("omnira:crystal_ball") && level.getBlockEntity(world) instanceof CrystalBallBlockEntity ball){
                ball.setLootTable(TOWER_LOOT);
                ball.setLootTableSeed(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(world.asLong()));
                AuthoredStructureConnections.defer(ball,new BlockPos(box.minX(),box.minY(),box.minZ()),"palace");
            }
        });
        TerraPalaceArmorStands.place(level,clip,box);
    }
}
