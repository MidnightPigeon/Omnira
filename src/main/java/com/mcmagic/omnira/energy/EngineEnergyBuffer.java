package com.mcmagic.omnira.energy;

/** Per-tick output budget is shared by push and pull, including simulation requests. */
public final class EngineEnergyBuffer {
    private long tick=Long.MIN_VALUE;
    private int stored,remainder,budget,capacity;
    public void beginTick(long now,int perSecond,boolean running) {
        capacity=Math.max(0,perSecond);
        stored=Math.min(stored,capacity);
        if(!running) {budget=0;return;}
        if(tick==now) {budget=Math.min(budget,(int)(((long)capacity+19)/20));return;}
        tick=now;
        long accumulated=(long)capacity+remainder;
        budget=(int)(accumulated/20);remainder=(int)(accumulated%20);
        stored=(int)Math.min(capacity,(long)stored+budget);
    }
    public int extract(int requested,boolean simulate) {
        int amount=Math.min(Math.max(0,requested),Math.min(stored,budget));
        if(!simulate) {stored-=amount;budget-=amount;}
        return amount;
    }
    public int stored() {return stored;}
    public int capacity() {return capacity;}
    public int remainder() {return remainder;}
    public void restore(int stored,int remainder) {
        this.stored=Math.max(0,stored);this.remainder=Math.clamp(remainder,0,19);
        budget=0;tick=Long.MIN_VALUE;
    }
}
