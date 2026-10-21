package com.mcmagic.omnira.client;

/** All visible nodes share this trajectory; render-thread cache, with unchanged phases. */
public final class ResonanceNodeMotion {
    private static double last=Double.NaN;
    private static final float[] RING=new float[14*3];
    public static float[] ring(double time){
        if(time!=last){
            for(int i=0;i<14;i++){
                double angle=time*.045+i*Math.PI*2/14;
                RING[i*3]=.5F+(float)Math.cos(angle)*.37F;
                RING[i*3+1]=.44F+(float)Math.sin(angle*2+time*.025)*.045F;
                RING[i*3+2]=.5F+(float)Math.sin(angle)*.37F;
            }
            last=time;
        }
        return RING;
    }
    private ResonanceNodeMotion(){}
}
