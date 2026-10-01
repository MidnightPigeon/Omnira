package com.mcmagic.omnira.client.screen;

/** Pure animation timing shared by the preview and regression tests. */
public final class EngineGuideTimeline {
    public static final int STAGES=5, DOCK_START=35, DOCK_END=85, POWER_ON=100, FIRST_STRIKE=110, COMPLETE=300;
    private EngineGuideTimeline() {}
    public static int duration(int stage) {return stage==4?350:200;}
    public static double ease(double time,double start,double end) {
        double t=Math.clamp((time-start)/(end-start),0,1);return t*t*(3-2*t);
    }
    public static float yaw(int stage,double time) {
        if(stage==4)return (float)(110+35*ease(time,0,90));
        return (float)(-35+180*ease(time,25,130));
    }
    public static double engineZ(int stage,double time) {return stage==4?.5*(1-ease(time,DOCK_START,DOCK_END)):0;}
    public static double tableZ(double time) {return -1-1.8*(1-ease(time,DOCK_START,DOCK_END));}
    public static int grids(int stage,double time) {return stage==0?0:stage==1?Math.clamp((int)Math.floor((time-20)/40)+1,0,3):3;}
    public static boolean powered(int stage,double time) {
        return stage==4?time>=POWER_ON:stage==2?(int)time%100>=50:time>=20;
    }
    public static double strikeAge(double time) {return time<FIRST_STRIKE || time>=COMPLETE+6?-1:(time-FIRST_STRIKE)%10;}
    public static int progress(double time) {return time<FIRST_STRIKE?0:Math.min(100,((int)(time-FIRST_STRIKE)/10+1)*5);}
}
