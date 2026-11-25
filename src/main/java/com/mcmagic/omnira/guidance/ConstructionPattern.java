package com.mcmagic.omnira.guidance;

import com.mcmagic.omnira.aggregation.*;
import com.mcmagic.omnira.alchemy.ReactionPoolBlock;
import com.mcmagic.omnira.alchemy.ReactionPoolStructure;
import com.mcmagic.omnira.alchemy.catalysis.CatalysisStructure;
import com.mcmagic.omnira.engine.*;
import com.mcmagic.omnira.forging.ForgeLayout;
import com.mcmagic.omnira.registry.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/** Coordinates are relative to the middle of the bottom layer, including for the vertical ring. */
public record ConstructionPattern(List<Cell> cells) {
    public record Cell(BlockPos offset, BlockState primary, BlockState alternate) {
        public BlockState display(int ticks) { return ticks / 80 % 2 == 0 ? primary : alternate; }
        public boolean matches(BlockState state) { return state.is(primary.getBlock()) || state.is(alternate.getBlock()); }
    }
    public long missingMask(net.minecraft.world.level.Level level,BlockPos base){
        long mask=0;
        for(int i=0;i<cells.size();i++){
            var cell=cells.get(i);
            var pos=base.offset(cell.offset());
            if(!level.hasChunkAt(pos) || !cell.matches(level.getBlockState(pos)))mask |= 1L<<i;
        }
        return mask;
    }
    private static final Map<String,ConstructionPattern> CACHE = new HashMap<>();
    public static ConstructionPattern of(String scene) { return CACHE.computeIfAbsent(scene, ConstructionPattern::build); }

    private static ConstructionPattern build(String scene) {
        List<Cell> cells = new ArrayList<>();
        int count = switch (scene) {
            case "advanced_assembly_table", "large_alchemical_reaction_pool" -> 18;
            case "advanced_crystal_aggregation_ring" -> 9;
            case "spacetime_engine" -> 10;
            case "alchemical_catalysis_chamber" -> 3;
            default -> throw new IllegalArgumentException(scene);
        };
        for (int i = 0; i < count; i++) {
            BlockPos pos;
            BlockState state;
            Block alternate = null;
            switch (scene) {
                case "advanced_assembly_table" -> {
                    int x=i%3-1, y=i/9, z=i%9/3-1;
                    pos=ForgeLayout.offset(Direction.SOUTH,x,y,z);
                    state=ForgeLayout.required(x,y,z).defaultBlockState();
                    if (state.isAir()) continue;
                    if (state.hasProperty(HORIZONTAL_FACING)) state=state.setValue(HORIZONTAL_FACING,
                            y==0?Direction.SOUTH:x<0?Direction.WEST:x>0?Direction.EAST:z>0?Direction.SOUTH:Direction.NORTH);
                }
                case "large_alchemical_reaction_pool" -> {
                    pos=ReactionPoolBlock.offset(Direction.SOUTH,i);
                    state=ReactionPoolStructure.required(i).defaultBlockState();
                }
                case "spacetime_engine" -> {
                    pos=SpacetimeEngineBlock.offset(i);
                    state=SpacetimeEngineStructure.required(i).defaultBlockState();
                    int side=SpacetimeEngineBlock.side(i);
                    if(side>=0) state=state.setValue(HORIZONTAL_FACING,SpacetimeEngineBlock.OUTPUTS[side]);
                }
                case "alchemical_catalysis_chamber" -> {
                    pos=BlockPos.ZERO.above(i);
                    state=CatalysisStructure.required(i).defaultBlockState();
                }
                default -> {
                    pos=AggregationLayout.part(BlockPos.ZERO.above(),Direction.SOUTH,i);
                    Block block=ModBlocks.INFUSED_CRYSTAL_CASING.get();
                    if(i==4) block=ModBlocks.CRYSTAL_PROCESSING_TABLE.get();
                    if(i==1||i==7){block=DreamContent.EXCITED_SPATIAL_CRYSTAL.get();alternate=AggregationContent.SPATIAL_BLOCK.get();}
                    if(i==3||i==5){block=DreamContent.LIVING_TEMPORAL_SILT.get();alternate=TimeNatureContent.LIVING_SILT.get();}
                    state=block.defaultBlockState();
                }
            }
            cells.add(new Cell(pos,state,alternate==null?state:alternate.defaultBlockState()));
        }
        return new ConstructionPattern(List.copyOf(cells));
    }
}
