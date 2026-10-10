package com.mcmagic.omnira.space;

import com.mcmagic.omnira.spacetime.TimeBiomeLayout;
import java.util.*;

/** Shared shore profile, measured against actual biome columns rather than Voronoi estimates. */
public final class CaveSurface {
    private record Tile(int x,int z,int region){}
    private static final int TILE=32,HALO=33,SIZE=TILE+2*HALO,LIMIT=32;
    private static final ThreadLocal<Map<Tile,int[]>> CACHE=ThreadLocal.withInitial(()->new LinkedHashMap<>(128,.75F,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Tile,int[]> entry){return size()>128;}
    });
    public static int coastDistance(int x,int z){
        return flatDistance(x,z,5);
    }
    public static int mazeDistance(int x,int z){return flatDistance(x,z,4);}
    private static int flatDistance(int x,int z,int region){
        int tx=Math.floorDiv(x,TILE),tz=Math.floorDiv(z,TILE);var key=new Tile(tx,tz,region);
        int[] tile=CACHE.get().computeIfAbsent(key,k->distances(tx*TILE,tz*TILE,region));
        return tile[Math.floorMod(z,TILE)*TILE+Math.floorMod(x,TILE)];
    }
    private static int[] distances(int ox,int oz,int region){
        boolean[] sea=new boolean[SIZE*SIZE];int[] distance=new int[sea.length],queue=new int[sea.length];
        Arrays.fill(distance,LIMIT);
        for(int z=0;z<SIZE;z++)for(int x=0;x<SIZE;x++)
            sea[z*SIZE+x]=TimeBiomeLayout.sample(ox+x-HALO+8192,oz+z-HALO-4096).region()==region;
        int head=0,tail=0;
        for(int z=1;z<SIZE-1;z++)for(int x=1;x<SIZE-1;x++){
            int i=z*SIZE+x;
            if(sea[i]!=sea[i-1]||sea[i]!=sea[i+1]||sea[i]!=sea[i-SIZE]||sea[i]!=sea[i+SIZE]){
                distance[i]=0;queue[tail++]=i;
            }
        }
        while(head<tail){
            int i=queue[head++],x=i%SIZE,z=i/SIZE,next=distance[i]+1;
            if(next>=LIMIT)continue;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
                int nx=x+dx,nz=z+dz;if(nx<0||nz<0||nx>=SIZE||nz>=SIZE)continue;
                int n=nz*SIZE+nx;
                if(sea[n]==sea[i]&&distance[n]>next){distance[n]=next;queue[tail++]=n;}
            }
        }
        int[] result=new int[TILE*TILE];
        for(int z=0;z<TILE;z++)for(int x=0;x<TILE;x++)result[z*TILE+x]=distance[(z+HALO)*SIZE+x+HALO];
        return result;
    }
    private static double smooth(double t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
    public static int landFloor(int x,int z){
        double relief=5*Math.sin(x/47.0)*Math.cos(z/61.0)+2*Math.sin((x+z)/19.0);
        return 40+(int)Math.round(relief*smooth(Math.min(coastDistance(x,z),mazeDistance(x,z))/24.0));
    }
    public static int seaFloor(int x,int z){
        double depth=28+3*Math.sin(x/37.0)*Math.cos(z/51.0)+Math.sin((x-z)/23.0);
        return Math.max(24,(int)Math.round(40+(depth-40)*smooth((coastDistance(x,z)-4)/28.0)));
    }
    private CaveSurface(){}
}
