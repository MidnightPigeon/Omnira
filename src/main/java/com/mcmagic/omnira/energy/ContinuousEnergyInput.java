package com.mcmagic.omnira.energy;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** FE is spent on arrival, including idle operation; receipts measure power, not stored fuel. */
public final class ContinuousEnergyInput implements IEnergyStorage {
    private final int base,maximum;
    private final LongSupplier clock;
    private final BooleanSupplier enabled;
    private final EnergyInputLimit limit;
    private final long[] stamps=new long[20];
    private final int[] receipts=new int[20];
    public ContinuousEnergyInput(int base,int maximum,LongSupplier clock,BooleanSupplier enabled){
        if(base<=0||maximum<base)throw new IllegalArgumentException("Invalid continuous power range");
        this.base=base;this.maximum=maximum;this.clock=clock;this.enabled=enabled;
        limit=new EnergyInputLimit(maximum);Arrays.fill(stamps,Long.MIN_VALUE);
    }
    @Override public int receiveEnergy(int requested,boolean simulate){
        if(!enabled.getAsBoolean())return 0;
        long now=clock.getAsLong();int slot=Math.floorMod(now,20);
        int amount=limit.receive(now,requested,Integer.MAX_VALUE,simulate);
        if(!simulate&&amount>0){
            if(stamps[slot]!=now){stamps[slot]=now;receipts[slot]=0;}
            receipts[slot]+=amount;
        }
        return amount;
    }
    public double factor(){return rate()/base;}
    public double rate(){
        long end=clock.getAsLong()-1,start=end;long sum=0;int count=0;
        // Read completed ticks so input/output block-entity order cannot change the result.
        for(int age=0;age<20;age++){
            long tick=end-age;int slot=Math.floorMod(tick,20);
            if(stamps[slot]!=tick||receipts[slot]<=0)break;
            sum+=receipts[slot];count++;start=tick;
        }
        if(count==0||sum<scheduled(base,start,end))return 0;
        if(sum==scheduled(base,start,end))return base;
        if(sum>=scheduled(maximum,start,end))return maximum;
        return Math.min(maximum,Math.max(base,sum*20.0/count));
    }
    private static long scheduled(int rate,long start,long end){
        long total=0;for(long tick=start;tick<=end;tick++){
            int phase=Math.floorMod(tick,20);total+=((long)(phase+1)*rate)/20-((long)phase*rate)/20;
        }return total;
    }
    public void reset(){Arrays.fill(stamps,Long.MIN_VALUE);Arrays.fill(receipts,0);}
    @Override public int getEnergyStored(){return 0;}
    @Override public int getMaxEnergyStored(){return (maximum+19)/20;}
    @Override public int extractEnergy(int amount,boolean simulate){return 0;}
    @Override public boolean canExtract(){return false;}
    @Override public boolean canReceive(){return enabled.getAsBoolean();}
}
