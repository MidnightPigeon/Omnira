package com.mcmagic.omnira.client.renderer;

/** Fixed-size travelling wavefront; age changes position and opacity, never its radius. */
public final class PulseRingMotion {
    public static final float SIZE=.5F;
    public record Frame(float offset,float alpha){}
    public static Frame frame(double time,int ring,boolean outward){
        double phase=time/30+ring*.5;phase-=Math.floor(phase);
        return new Frame((float)(.35+(outward?phase:1-phase)*.6),(float)((1-phase)*.85));
    }
    private PulseRingMotion(){}
}
