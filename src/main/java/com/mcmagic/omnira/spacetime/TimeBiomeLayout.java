package com.mcmagic.omnira.spacetime;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/** Shared horizontal layout for biome selection, terrain and structure eligibility. */
public final class TimeBiomeLayout {
    public static final int SCALE=256;
    private static final SimplexNoise WARP_X=new SimplexNoise(RandomSource.create(73109));
    private static final SimplexNoise WARP_Z=new SimplexNoise(RandomSource.create(91837));
    public record RegionSample(int region,double edgeDistance){}
    private record Cached(int x,int z,RegionSample sample){}
    private static final ThreadLocal<Cached> CACHE=new ThreadLocal<>();
    public static RegionSample sample(int x,int z){
        var cached=CACHE.get();
        if(cached!=null&&cached.x()==x&&cached.z()==z)return cached.sample();
        double wx=x+95*WARP_X.getValue(x/290.0,z/290.0)+22*WARP_Z.getValue(x/75.0,z/75.0);
        double wz=z+95*WARP_Z.getValue(x/290.0,z/290.0)+22*WARP_X.getValue(x/75.0,z/75.0);
        int cx=(int)Math.floor(wx/SCALE),cz=(int)Math.floor(wz/SCALE),region=0;
        double first=Double.POSITIVE_INFINITY,second=first;
        // Jittered centers and multi-scale domain warping remove straight grid boundaries.
        for(int a=cx-2;a<=cx+2;a++)for(int b=cz-2;b<=cz+2;b++){
            int h=a*73428767^b*9123671;
            h=(h^(h>>>16))*1274126177;
            double px=(a+.28+.44*((h>>>8)&65535)/65535.0)*SCALE;
            double pz=(b+.28+.44*((h>>>16)&65535)/65535.0)*SCALE;
            double d=(wx-px)*(wx-px)+(wz-pz)*(wz-pz);
            if(d<first){second=first;first=d;region=Math.floorMod(a+2*b+2*Math.floorDiv(b,4),6);}
            else if(d<second)second=d;
        }
        var result=new RegionSample(switch(region){case 0->0;case 1->3;case 2->1;case 3->5;case 4->2;default->4;},(Math.sqrt(second)-Math.sqrt(first))*.5);
        CACHE.set(new Cached(x,z,result));
        return result;
    }
}
