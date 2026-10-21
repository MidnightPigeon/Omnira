package com.mcmagic.omnira.weaver;

/** Shared timing for the authoritative exit sequence and its render-only fragments. */
public final class WeaverDeparture {
    public static final int NONE=0,DISSOLVE=1,DEFEATED=2;
    public static final int REWARD_RETURNED=32,EXPERIENCE=1024;
    public static final int CLOUD_TICK=12,FIRST_SEED_TICK=32,SEED_INTERVAL=16,DISSOLVE_TICKS=120,DEATH_TICKS=140;
    public static final float DEATH_RISE=DEATH_TICKS*.018F+2;
    public static final float BLAST_MIN_RADIUS=.55F,BLAST_MAX_RADIUS=1.1F;
    public static float deathRise(float age){return Math.clamp(age/DEATH_TICKS,0,1)*DEATH_RISE;}
    public static float coreScale(int mode,float age){return mode==DISSOLVE?Math.clamp((DISSOLVE_TICKS-age)/20F,0,1):1;}
    public static float loosen(int group,float age){return group==0||group==2||group==7?0:Math.clamp((age-18-group*4)/30F,0,1);}
    public static boolean showFace(int mode,float age,int group,float x,float y,float z){
        if(mode==NONE)return true;
        if(group==2||group==7)return mode==DISSOLVE?age<DISSOLVE_TICKS:age<DEATH_TICKS-8;
        if(mode==DISSOLVE){
            int part=switch(group){case 1->0;case 3->1;case 4->2;case 5->3;default->-1;};
            if(part>=0)return age<FIRST_SEED_TICK+part*SEED_INTERVAL+8;
        }
        int hash=Math.floorMod((int)Math.floor(x/4)*73+(int)Math.floor(y/4)*37+(int)Math.floor(z/4)*19,64);
        return age<(mode==DISSOLVE?24+hash:40+hash*1.35F);
    }
    private WeaverDeparture(){}
}
