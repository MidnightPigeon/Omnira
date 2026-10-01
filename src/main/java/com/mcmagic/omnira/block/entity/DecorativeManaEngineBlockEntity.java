package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DecorativeManaEngineBlockEntity extends BlockEntity implements ManaEngineAccess {
    private final ManaEngineState engine=new ManaEngineState(this,false);
    public DecorativeManaEngineBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.MANA_ENGINE.get(),pos,state);}
    @Override public ManaEngineState engineState() {return engine;}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {super.saveAdditional(tag,registries);engine.write(tag,registries);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {super.loadAdditional(tag,registries);engine.read(tag,registries);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {return ClientboundBlockEntityDataPacket.create(this);}
    public void tick() {
        engine.tickEnergy();engine.animate();
        if(level!=null && !level.isClientSide && engine.pollChanges())
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
}
