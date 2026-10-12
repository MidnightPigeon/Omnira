package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class CrystalPedestalBlockEntity extends CrystalProcessingTableBlockEntity {
    public CrystalPedestalBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.CRYSTAL_PEDESTAL.get(),pos,state,1);}
    @Override public int getContainerSize() {return 1;}
    @Override public int getMaxStackSize() {return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {return slot==0;}
    @Override public void setChanged(){
        super.setChanged();
        validateRituals();
    }
    public void validateRituals(){
        if(level==null || level.isClientSide)return;
        for(var offset:com.mcmagic.omnira.world.dimension.DreamRitual.ANCHORS){
            var core=worldPosition.offset(-offset[0],0,-offset[1]);
            if(level.hasChunkAt(core) && level.getBlockEntity(core) instanceof RitualEnergyCoreBlockEntity ritual)ritual.validateGenesis();
        }
    }
}
