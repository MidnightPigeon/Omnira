package com.mcmagic.omnira.client.renderer;

/** Five wavefronts rise from the feet and settle evenly over the player's height. */
public final class PlayerPulseRingMotion {
    public static final int RINGS=5,CHARGE_TICKS=100;
    public record Ring(float height,float alpha){}
    public static Ring at(double elapsed,int index,boolean active){
        double delay=index*12,progress=Math.clamp((elapsed-delay)/(CHARGE_TICKS-delay),0,1);
        double smooth=progress*progress*(3-2*progress),target=(index+.5)/RINGS;
        float alpha=elapsed<delay?0:(float)(Math.min(1,(elapsed-delay+1)/8)*(.5+.3*smooth));
        if(active)alpha=(float)(.72+.1*Math.sin(elapsed*.08+index*.8));
        return new Ring((float)(.015+(target-.015)*smooth),alpha);
    }
    private PlayerPulseRingMotion(){}
}
