package com.mcmagic.omnira.time;

import com.mcmagic.omnira.spacetime.StrataBiomeSource;

/** Sparse, irregular shallow pools wholly inside the garden region. */
public final class RecurrencePonds {
    public static final int WATER_Y=224;
    public static final int FLOOR_Y=WATER_Y-2;
    private static final int CELL=48;
    private record Cached(int x,int z,boolean pond){}
    private static final ThreadLocal<Cached> CACHE=new ThreadLocal<>();
    private RecurrencePonds(){}

    public static boolean contains(int x,int z){
        var cached=CACHE.get();
        if(cached!=null&&cached.x()==x&&cached.z()==z)return cached.pond();
        boolean pond=calculate(x,z);
        CACHE.set(new Cached(x,z,pond));
        return pond;
    }

    private static boolean calculate(int x,int z){
        if(!StrataBiomeSource.recurrenceGarden(x,z)||StrataBiomeSource.sample(x,z).edgeDistance()<9)return false;
        int cx=Math.floorDiv(x,CELL),cz=Math.floorDiv(z,CELL);
        for(int i=cx-1;i<=cx+1;i++)for(int j=cz-1;j<=cz+1;j++){
            int seed=hash(i,j);
            if((seed&1)!=0)continue;
            int px=i*CELL+12+Math.floorMod(seed>>>5,CELL-24);
            int pz=j*CELL+12+Math.floorMod(seed>>>13,CELL-24);
            double dx=x-px,dz=z-pz;
            double ripple=.38*Math.sin(x*.83+z*.31)+.27*Math.cos(z*.72-x*.22);
            if(dx*dx+dz*dz<Math.pow(4.4+Math.floorMod(seed>>>21,3)+ripple,2))return true;
        }
        return false;
    }

    private static int hash(int x,int z){
        int n=x*73428767^z*1274126177;
        n=(n^(n>>>16))*0x7feb352d;
        return n^(n>>>15);
    }
}
