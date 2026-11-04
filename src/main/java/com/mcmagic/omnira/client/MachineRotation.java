package com.mcmagic.omnira.client;

/** Client-owned phases; speed changes never replace the accumulated angle. */
public final class MachineRotation {
    private final java.util.Map<Object,Phase> phases=new java.util.WeakHashMap<>();
    private final double initial,settling;
    public MachineRotation(double initial,double settling){this.initial=initial;this.settling=settling;}
    public double angle(Object owner,double time,double target){
        if(owner==null)return initial;
        return phases.computeIfAbsent(owner,key->new Phase(initial)).advance(time,target,settling);
    }
    public static final class Phase {
        private double angle,speed,last=Double.NaN;
        public Phase(double angle){this.angle=angle;}
        public double advance(double time,double target,double settling){
            if(Double.isNaN(last)){last=time;speed=target;return angle;}
            double elapsed=time-last;last=time;
            // No catch-up burst after unloading, pausing or skipping distant render frames.
            if(elapsed<0||elapsed>5){speed=target;return angle;}
            if(settling<=0){speed=target;angle+=target*elapsed;}
            else {
                double decay=Math.exp(-elapsed/settling);
                angle+=target*elapsed+(speed-target)*settling*(1-decay);
                speed=target+(speed-target)*decay;
            }
            angle=((angle%360)+360)%360;return angle;
        }
    }
}
