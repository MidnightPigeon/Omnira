package com.mcmagic.omnira.space;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SpacetimePortBlockEntity extends BlockEntity implements SpacetimePortHost {
    private final SpacetimePortState port=new SpacetimePortState(this);
    public SpacetimePortBlockEntity(BlockPos p,BlockState s){super(ModBlockEntityTypes.SPACETIME_PORT.get(),p,s);}
    public SpacetimePortState port(){return port;}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);port.write(t);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);port.read(t);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=new CompoundTag();port.write(t);return t;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
