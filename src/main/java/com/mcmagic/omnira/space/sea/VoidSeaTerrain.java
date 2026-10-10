package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.space.FoldedTerrain;
import com.mcmagic.omnira.spacetime.TimeBiomeLayout;
import java.util.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Coordinate-only terrain, shared by generation and height/column queries. */
public final class VoidSeaTerrain {
    public static final int SEA_LEVEL=40;
    private record Star(int x,int y,int z,int core,int radius,boolean loot){}
    private record Column(int x,int z,int floor,int roof,int island,int fissureY,List<Star> stars){}
    private static final ThreadLocal<Column> CACHE=new ThreadLocal<>();
    public static boolean biome(int x,int z){return TimeBiomeLayout.sample(x+8192,z-4096).region()==5;}
    public static int hash(int x,int y,int z){int h=x*73428767^y*9123671^z*1274126177;h=(h^(h>>>16))*0x7feb352d;return h^(h>>>15);}
    public static int floor(int x,int z){
        return com.mcmagic.omnira.space.CaveSurface.seaFloor(x,z);
    }
    private static Column column(int x,int z){
        Column old=CACHE.get();if(old!=null&&old.x==x&&old.z==z)return old;
        int floor=floor(x,z),island=floor;
        int cx=Math.floorDiv(x,48),cz=Math.floorDiv(z,48);
        for(int gx=cx-1;gx<=cx+1;gx++)for(int gz=cz-1;gz<=cz+1;gz++){
            int h=hash(gx,41,gz);if(Math.floorMod(h,5)!=0)continue;
            int ix=gx*48+12+Math.floorMod(h>>>4,24),iz=gz*48+12+Math.floorMod(h>>>12,24);
            if(!biome(ix,iz)||com.mcmagic.omnira.space.CaveSurface.coastDistance(ix,iz)<16)continue;
            double dx=x-ix,dz=z-iz,q=dx*dx/144+dz*dz/100;
            q+=.10*Math.sin((x+z)/3.0)+.09*Math.sin(x/2.0-z/5.0);
            if(q<1)island=Math.max(island,(int)Math.round(SEA_LEVEL+5-q*27));
        }
        var stars=new ArrayList<Star>();cx=Math.floorDiv(x,32);cz=Math.floorDiv(z,32);
        for(int gx=cx-1;gx<=cx+1;gx++)for(int gz=cz-1;gz<=cz+1;gz++){
            int h=hash(gx,971,gz);if(Math.floorMod(h,3)!=0)continue;
            int sx=gx*32+8+Math.floorMod(h>>>4,16),sz=gz*32+8+Math.floorMod(h>>>12,16);
            int roll=Math.floorMod(h>>>20,20),core=roll==0?3:roll<6?2:1,r=core==3?8:core==2?5:3;
            if(!biome(sx,sz)||com.mcmagic.omnira.space.CaveSurface.coastDistance(sx,sz)<r+15||Math.abs(x-sx)>r||Math.abs(z-sz)>r)continue;
            int starY=Math.min(floor(sx,sz)+Math.max(1,r/3),SEA_LEVEL-r-1);
            stars.add(new Star(sx,starY,sz,core,r,(h&512)==0));
        }
        int fx=Math.floorDiv(x,12),fz=Math.floorDiv(z,12),h=hash(fx,723,fz);
        int fy=Math.floorMod(h,3)==0&&Math.floorMod(x,12)==2+Math.floorMod(h>>>3,8)&&Math.floorMod(z,12)==2+Math.floorMod(h>>>10,8)
                ?floor+2+Math.floorMod(h>>>18,Math.max(1,SEA_LEVEL+1-floor)):-1;
        Column result=new Column(x,z,floor,FoldedTerrain.roof(x,z),island,fy,List.copyOf(stars));CACHE.set(result);return result;
    }
    public static BlockState block(int x,int y,int z){
        Column c=column(x,z);
        if(y>=c.roof)return DreamContent.SPATIAL_CRYSTAL.get().defaultBlockState();
        if(y<=c.island&&c.island>c.floor)return DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState();
        for(Star s:c.stars){
            double o=s.core%2==0?.5:0,dx=x-s.x+o,dy=y-s.y+o,dz=z-s.z+o,d=dx*dx+dy*dy+dz*dz;
            if(d>s.radius*s.radius)continue;
            if(s.core==3&&s.loot&&x==s.x&&y==s.y&&z==s.z)return ModBlocks.CRYSTAL_BALL.get().defaultBlockState();
            int min=-s.core/2,max=min+s.core;
            if(x-s.x>=min&&x-s.x<max&&y-s.y>=min&&y-s.y<max&&z-s.z>=min&&z-s.z<max)return DreamContent.LIGHT_CRYSTAL_CORE.get().defaultBlockState();
            if(d>=Math.pow(s.radius-(s.radius>=5?2.5:1.5),2))return DreamContent.LIGHT_SOURCE_CRYSTAL.get().defaultBlockState();
            return VoidSeaContent.LIQUID.get().defaultBlockState();
        }
        if(y<=c.floor)return (Math.floorMod(hash(x,y,z),13)==0?DreamContent.EXCITED_SPATIAL_CRYSTAL:DreamContent.SPATIAL_CRYSTAL).get().defaultBlockState();
        if(y==c.fissureY&&y<=SEA_LEVEL+2)return VoidSeaContent.FISSURE.get().defaultBlockState().setValue(VoidFissureBlock.FLUID_LEVEL,y<=SEA_LEVEL?0:16);
        return y<=SEA_LEVEL?VoidSeaContent.LIQUID.get().defaultBlockState():Blocks.AIR.defaultBlockState();
    }
    private VoidSeaTerrain(){}
}
