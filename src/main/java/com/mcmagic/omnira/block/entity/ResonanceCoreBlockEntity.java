package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class ResonanceCoreBlockEntity extends BlockEntity {
    public java.util.UUID identity=java.util.UUID.randomUUID(),owner,terminal;
    public ResonanceCoreBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.RESONANCE_CORE.get(),pos,state);}
    public IItemHandler items() {
        if(level==null || !level.hasChunkAt(worldPosition.below()))return null;
        if(level.getBlockEntity(worldPosition.below()) instanceof LiquidCrystalBallBlockEntity)return null;
        var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,worldPosition.below(),Direction.UP);
        if(handler!=null)return handler;
        return level.getBlockEntity(worldPosition.below()) instanceof net.minecraft.world.Container c?new net.neoforged.neoforge.items.wrapper.InvWrapper(c):null;
    }
    public IFluidHandler fluids() {
        if(level==null || !level.hasChunkAt(worldPosition.below()))return null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK,worldPosition.below(),Direction.UP);
    }
    public int kind(){return level!=null && level.getBlockEntity(worldPosition.below()) instanceof com.mcmagic.omnira.shop.MarisaOrbBlockEntity?4:fluids()!=null?3:items()!=null?2:0;}
    public boolean permits(net.minecraft.world.entity.player.Player player) {
        return level!=null && player.getUUID().equals(owner) && level.mayInteract(player,worldPosition)
                && level.mayInteract(player,worldPosition.below())
                && (!(level.getBlockEntity(worldPosition.below()) instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity container) || container.canOpen(player));
    }
    public void refresh() {
        if(level==null || level.isClientSide)return;
        int kind=kind(),mode=kind==0?0:terminal==null?1:kind;
        if(getBlockState().getValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE)!=mode)
            level.setBlockAndUpdate(worldPosition,getBlockState().setValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE,mode));
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);tag.putUUID("Identity",identity);
        if(owner!=null)tag.putUUID("Owner",owner);if(terminal!=null)tag.putUUID("Terminal",terminal);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):java.util.UUID.randomUUID();
        owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;terminal=tag.hasUUID("Terminal")?tag.getUUID("Terminal"):null;
    }
}
