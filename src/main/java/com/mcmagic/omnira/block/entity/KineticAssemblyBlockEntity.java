package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Optional kinetic host; the same assembly inventory and recipe logic serve both modes. */
public final class KineticAssemblyBlockEntity extends KineticBlockEntity implements AssemblyAccess {
    private final ArcaneAssemblyTableBlockEntity work;
    private float lastRpm=Float.NaN;
    public KineticAssemblyBlockEntity(BlockPos pos,BlockState state) {
        super(ModBlockEntityTypes.ARCANE_ASSEMBLY_TABLE.get(),pos,state);
        work=new ArcaneAssemblyTableBlockEntity(pos,state);
        work.setHost(this);
    }
    @Override public ArcaneAssemblyTableBlockEntity assembly() {return work;}
    @Override public void setLevel(Level level) {
        super.setLevel(level);
        work.setLevel(level);
    }
    @Override public float calculateStressApplied() {
        float rpm=Math.abs(getTheoreticalSpeed());
        return lastStressApplied=rpm>=16?256F/rpm:0;
    }
    @Override public void tick() {
        super.tick();
        work.setBlockState(getBlockState());
        if(level==null || level.isClientSide) return;
        float rpm=Math.abs(getTheoreticalSpeed());
        if(rpm!=lastRpm) {
            lastRpm=rpm;
            if(hasNetwork()) getOrCreateNetwork().updateStressFor(this,calculateStressApplied());
        }
        work.setMechanicalPowered(Math.abs(getSpeed())>=16 && !isOverStressed());
        if(work.mechanicalPowered()) work.automaticStrike();
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket) {
        super.write(tag,registries,clientPacket);
        if(work!=null) work.writeWork(tag,registries);
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket) {
        super.read(tag,registries,clientPacket);
        if(work!=null) work.readWork(tag,registries);
    }
}
