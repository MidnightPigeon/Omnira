package com.mcmagic.omnira.compat.create;

import com.mcmagic.omnira.workstation.*;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class KineticCrystalWorkstationEntity extends KineticBlockEntity implements WorkstationAccess {
    private final CrystalWorkstationEntity work;
    private float lastRpm=Float.NaN;
    public KineticCrystalWorkstationEntity(BlockPos pos,BlockState s){super(CrystalWorkstations.ENTITY.get(),pos,s);work=new CrystalWorkstationEntity(pos,s);work.host=this;}
    @Override public CrystalWorkstationEntity workstation(){return work;}
    @Override public void setLevel(Level l){super.setLevel(l);work.setLevel(l);}
    @Override public float calculateStressApplied(){float rpm=Math.abs(getTheoreticalSpeed());return lastStressApplied=rpm>=16?256F/rpm:0;}
    @Override public void tick(){
        super.tick();work.setBlockState(getBlockState());
        if(level==null||level.isClientSide)return;
        float rpm=Math.abs(getTheoreticalSpeed());
        if(rpm!=lastRpm){lastRpm=rpm;if(hasNetwork())getOrCreateNetwork().updateStressFor(this,calculateStressApplied());}
        work.tick(Math.abs(getSpeed())>=16&&!isOverStressed(),getSpeed());
    }
    @Override protected void write(CompoundTag tag,HolderLookup.Provider r,boolean packet){super.write(tag,r,packet);if(work!=null)work.writeWork(tag,r);}
    @Override protected void read(CompoundTag tag,HolderLookup.Provider r,boolean packet){super.read(tag,r,packet);if(work!=null)work.readWork(tag,r);}
}
