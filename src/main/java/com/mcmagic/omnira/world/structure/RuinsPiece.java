package com.mcmagic.omnira.world.structure;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import java.util.*;

public final class RuinsPiece extends StructurePiece {
    private final int variant;
    private final long seed;
    public record Cell(BlockState state,String loot,CompoundTag data) {
        public Cell(BlockState state,String loot) {this(state,loot,null);}
    }
    public static int width(int variant) {return variant==2?13:variant==1?19:17;}
    public static int depth(int variant) {return variant==2?13:17;}
    public static int height(int variant) {return variant==2?24:17;}
    public RuinsPiece(int variant,int x,int y,int z,Direction facing,long seed) {
        super(RuinsStructure.PIECE.get(),0,BoundingBox.orientBox(x,y,z,0,0,0,width(variant),height(variant),depth(variant),facing));
        this.variant=variant;this.seed=seed;setOrientation(facing);
    }
    public RuinsPiece(StructurePieceSerializationContext context,CompoundTag tag) {
        super(RuinsStructure.PIECE.get(),tag);variant=Math.clamp(tag.getInt("Variant"),0,2);seed=tag.getLong("LayoutSeed");
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        tag.putInt("Variant",variant);tag.putLong("LayoutSeed",seed);
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,
                                      BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        var plan=blueprint(variant,seed);
        Map<BlockPos,Cell> worldPlan=new LinkedHashMap<>();
        plan.forEach((p,c)->worldPlan.put(getWorldPos(p.getX(),p.getY(),p.getZ()).immutable(),new Cell(orient(c.state()),c.loot(),c.data())));
        RuinStairShapes.resolve(worldPlan);
        for(var entry:plan.entrySet()) {
            var p=entry.getKey();var cell=entry.getValue();
            var world=getWorldPos(p.getX(),p.getY(),p.getZ());
            if(!clip.isInside(world))continue;
            level.setBlock(world,worldPlan.get(world).state(),2);
            var fluid=level.getFluidState(world);
            if(!fluid.isEmpty())level.scheduleTick(world,fluid.getType(),0);
            var be=level.getBlockEntity(world);
            if(be!=null && (cell.data()!=null||cell.loot()!=null)) {
                var data=cell.data()==null?new CompoundTag():cell.data().copy();
                data.putInt("x",world.getX());data.putInt("y",world.getY());data.putInt("z",world.getZ());
                if(cell.loot()!=null) {data.putString("LootTable","omnira:chests/"+cell.loot());data.putLong("LootTableSeed",seed^p.asLong());}
                be.loadWithComponents(data,level.registryAccess());be.setChanged();
            }
            if(p.getY()==0 && !cell.state().isAir()) fillColumnDown(level,Blocks.COBBLESTONE.defaultBlockState(),p.getX(),-1,p.getZ(),clip);
        }
    }
    private BlockState orient(BlockState state) {
        return switch(getOrientation()) {
            case NORTH -> state.mirror(Mirror.LEFT_RIGHT);
            case WEST -> state.rotate(Rotation.CLOCKWISE_90);
            case EAST -> state.mirror(Mirror.LEFT_RIGHT).rotate(Rotation.CLOCKWISE_90);
            default -> state;
        };
    }
    /** Stable local blueprint: chunk processing order cannot reroll decay or the attic workstation. */
    public static Map<BlockPos,Cell> blueprint(int variant,long seed) {
        var b=new Plan(seed);
        AuthoredRuins.build(b,variant);
        RuinStairShapes.resolve(b.cells);
        return b.cells;
    }
    static final class Plan {
        final Map<BlockPos,Cell> cells=new LinkedHashMap<>();
        final RandomSource random;
        Plan(long seed) {random=RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed));}
        void put(int x,int y,int z,BlockState state) {cells.put(new BlockPos(x,y,z),new Cell(state,null));}
        void put(int x,int y,int z,Block block) {put(x,y,z,block.defaultBlockState());}
        void box(int x,int y,int z,int xx,int yy,int zz,Block block) {
            for(int a=x;a<=xx;a++) for(int c=z;c<=zz;c++) for(int h=y;h<=yy;h++) put(a,h,c,block);
        }
        void chest(int x,int y,int z,String loot) {cells.put(new BlockPos(x,y,z),new Cell(Blocks.CHEST.defaultBlockState(),loot));}
        void machine(int x,int y,int z,Block block) {
            var state=block.defaultBlockState();
            if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH);
            put(x,y,z,state);
        }
        Block stone() {return random.nextInt(5)==0?Blocks.MOSSY_STONE_BRICKS:random.nextInt(5)==0?Blocks.CRACKED_STONE_BRICKS:Blocks.STONE_BRICKS;}
    }
}
