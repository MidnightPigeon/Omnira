package com.mcmagic.omnira.pulse;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PulseWireEntity extends BlockEntity {
    public int visualFlow;
    public PulseWireEntity(BlockPos p,BlockState s){super(PulseContent.WIRE_ENTITY.get(),p,s);}
    public void tick(){if(Math.floorMod(level.getGameTime()+worldPosition.hashCode(),20)==0){
        boolean active=PulseNetworks.wirePowered(this);int flow=PulseNetworks.visualFlow(this);boolean changed=flow!=visualFlow;visualFlow=flow;
        if(active!=getBlockState().getValue(PulseWireBlock.POWERED))level.setBlock(worldPosition,getBlockState().setValue(PulseWireBlock.POWERED,active),2);
        else if(changed)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){var tag=super.getUpdateTag(registries);tag.putInt("VisualFlow",visualFlow);return tag;}
    @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries){super.loadAdditional(tag,registries);visualFlow=tag.getInt("VisualFlow");}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
