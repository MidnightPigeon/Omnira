package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** Loaded only by the optional Create factory. */
public final class ManaEngineBlockEntity extends GeneratingKineticBlockEntity implements ManaEngineAccess {
    private final ManaEngineState engine=new ManaEngineState(this,true);
    public ManaEngineBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.MANA_ENGINE.get(),pos,state);}
    @Override public ManaEngineState engineState() {return engine;}
    @Override public float getGeneratedSpeed() {return engine==null?0:engine.generatedSpeed();}
    @Override public float calculateAddedStressCapacity() {
        // Create stores SU per rpm; our core defines total SU at the fixed speed.
        return lastCapacityProvided=getGeneratedSpeed()==0?0:engine.ratedCapacity()/(float)ManaEngineState.RPM;
    }
    @Override public float calculateStressApplied() {return lastStressApplied=0;}
    @Override public void tick() {
        super.tick();
        if(level==null) return;
        if(!level.isClientSide && engine.pollChanges()) {updateGeneratedRotation();setChanged();}
        engine.tickEnergy();engine.animate();
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket) {
        super.write(tag,registries,clientPacket);engine.write(tag,registries);
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket) {
        super.read(tag,registries,clientPacket);engine.read(tag,registries);
    }
}
