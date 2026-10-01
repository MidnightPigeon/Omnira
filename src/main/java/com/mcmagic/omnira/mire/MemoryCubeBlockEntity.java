package com.mcmagic.omnira.mire;

import com.mcmagic.omnira.block.MemoryCubeBlock;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps the placed state stable outside the mire's reversal window. */
public final class MemoryCubeBlockEntity extends BlockEntity {
    private boolean basePeaceful;
    public MemoryCubeBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.MEMORY_CUBE.get(),pos,state);basePeaceful=state.getValue(MemoryCubeBlock.PEACEFUL);}
    public void tick(){
        if(!(level instanceof ServerLevel server)||server.getGameTime()%10!=0)return;
        boolean reversing=server.getBiome(worldPosition).is(MireCycle.BIOME)&&MireCycle.reversing(server.getGameTime());
        boolean expected=basePeaceful!=reversing;
        if(getBlockState().getValue(MemoryCubeBlock.PEACEFUL)!=expected)
            server.setBlock(worldPosition,getBlockState().setValue(MemoryCubeBlock.PEACEFUL,expected),3);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putBoolean("BasePeaceful",basePeaceful);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);
        if(tag.contains("BasePeaceful"))basePeaceful=tag.getBoolean("BasePeaceful");
    }
}
