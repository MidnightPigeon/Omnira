package com.mcmagic.omnira.space.phase;

/** Fixed-count decorative geometry, independent of particle lifetime and tick rate. */
public final class PhaseMoteLayout {
    public static final int CACHE_LIMIT=512, VISIBLE_LIMIT=128, RANGE=32;
    public static final float RADIUS=1F/32;
    public record Mote(float x,float y,float z,int color){}
    public static Mote sample(boolean cluster,float phase,double time,int index){
        double angle=time*.018+index*Math.PI*2/3;
        double radius=cluster?.49:.26;
        float x=.5F+(float)(Math.cos(angle)*radius),z=.5F+(float)(Math.sin(angle)*radius);
        float y=.5F+(float)Math.sin(angle*1.3+index)*.26F;
        phase=Math.clamp(phase,0F,1F);
        int r=Math.round(207+(110-207)*phase),g=Math.round(166+(41-166)*phase),b=Math.round(255+(194-255)*phase);
        return new Mote(x,y,z,r<<16|g<<8|b);
    }
    private PhaseMoteLayout(){}
}
