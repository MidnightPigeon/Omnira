package com.mcmagic.omnira.energy;

import java.util.*;
import java.util.function.LongSupplier;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Visual-only receipt tracking; simulated probes and buffered energy never invent an inlet. */
public final class EnergyInputFaces {
    private final IEnergyStorage storage;
    private final LongSupplier clock;
    private final Map<Direction,Long> receipts=new EnumMap<>(Direction.class);
    private final Map<Direction,IEnergyStorage> ports=new EnumMap<>(Direction.class);
    public EnergyInputFaces(IEnergyStorage storage,LongSupplier clock){this.storage=storage;this.clock=clock;}
    public int mask(){
        int mask=0;long now=clock.getAsLong();
        for(var entry:receipts.entrySet())if(now>=entry.getValue()&&now-entry.getValue()<20)mask|=1<<entry.getKey().ordinal();
        return mask;
    }
    public IEnergyStorage port(Direction face){
        if(face==null)return null;
        return ports.computeIfAbsent(face,side->new IEnergyStorage(){
            public int receiveEnergy(int amount,boolean simulate){int n=storage.receiveEnergy(amount,simulate);if(!simulate&&n>0)receipts.put(side,clock.getAsLong());return n;}
            public int extractEnergy(int amount,boolean simulate){return storage.extractEnergy(amount,simulate);}
            public int getEnergyStored(){return storage.getEnergyStored();}
            public int getMaxEnergyStored(){return storage.getMaxEnergyStored();}
            public boolean canExtract(){return storage.canExtract();}
            public boolean canReceive(){return storage.canReceive();}
        });
    }
}
