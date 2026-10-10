package com.mcmagic.omnira.energy;

/** A shared, non-banking tick budget for every input face of one machine. */
public final class EnergyInputLimit {
    private final int perSecond;
    private long tick=Long.MIN_VALUE;
    private int used;

    public EnergyInputLimit(int perSecond){
        if(perSecond<=0)throw new IllegalArgumentException("Positive FE rate required");
        this.perSecond=perSecond;
    }
    public int receive(long now,int requested,int space,boolean simulate){
        int phase=(int)Math.floorMod(now,20);
        int budget=(int)(((long)(phase+1)*perSecond)/20-((long)phase*perSecond)/20);
        int accepted=Math.min(Math.max(0,requested),Math.min(Math.max(0,space),Math.max(0,budget-(tick==now?used:0))));
        if(!simulate&&accepted>0){
            if(tick!=now){tick=now;used=0;}
            used+=accepted;
        }
        return accepted;
    }
}
