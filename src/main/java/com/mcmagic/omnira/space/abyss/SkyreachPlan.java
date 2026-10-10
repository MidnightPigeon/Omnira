package com.mcmagic.omnira.space.abyss;

import com.mcmagic.omnira.shop.KirisameShopPlan.Cell;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Open circular loggias, mirrored about the gravity crossing, with two helical flights. */
public final class SkyreachPlan {
    public static final int WIDTH=21,REFERENCE_HEIGHT=56;
    private static final String STONE="omnira:mirror_rock",TRIM="omnira:engraved_mirror_rock";
    private static final String CRYSTAL="omnira:excited_rough_spatial_crystal_block";
    public static List<Cell> build(int height){
        Map<BlockPos,Cell> cells=new LinkedHashMap<>();
        for(int y=0;y<=height;y++)for(int x=0;x<WIDTH;x++)for(int z=0;z<WIDTH;z++)
            put(cells,x,y,z,"minecraft:air",Map.of(),"");
        int half=(height-5)/2;
        for(int side=0;side<2;side++){
            for(int d=0;d<=half;d++){
                int y=side==0?d:height-d;
                int tier=d/6,phase=d%6;
                double radius=d<=2?9.7:d>=half-2?8.7:9.1;
                for(int x=0;x<WIDTH;x++)for(int z=0;z<WIDTH;z++){
                    double r=Math.hypot(x-10,z-10);
                    if(d==0&&r<=9.7)put(cells,x,y,z,STONE,Map.of(),"");
                    else if(d<=2&&r>=7.7&&r<=9.7)put(cells,x,y,z,STONE,Map.of(),"");
                    else if((phase==0||d==half)&&r>=7.5&&r<=radius)
                        put(cells,x,y,z,TRIM,Map.of(),"");
                    else if(phase==5&&r>=8.4&&r<=radius)
                        put(cells,x,y,z,STONE,Map.of(),"");
                }
                // Each storey's columns shift around the ring, spiralling toward the middle.
                if(d>=3&&phase!=0)for(int i=0;i<16;i++){
                    double angle=i*Math.PI/8+tier*Math.PI/16;
                    int x=10+(int)Math.round(Math.cos(angle)*8.5),z=10+(int)Math.round(Math.sin(angle)*8.5);
                    put(cells,x,y,z,phase==1||phase==4?TRIM:CRYSTAL,Map.of(),"");
                    if(phase==4)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                        if(Math.abs(dx)+Math.abs(dz)==1&&Math.hypot(x+dx-10,z+dz-10)>=7.7)
                            put(cells,x+dx,y,z+dz,STONE,Map.of(),"");
                }
            }
            for(int d=1;d<=4;d++)for(int x=8;x<=12;x++)for(int z=17;z<=20;z++)
                put(cells,x,side==0?d:height-d,z,"minecraft:air",Map.of(),"");
            for(int x:new int[]{7,13})for(int d=1;d<=4;d++)
                put(cells,x,side==0?d:height-d,18,CRYSTAL,Map.of(),"");
            for(int x=7;x<=13;x++)put(cells,x,side==0?5:height-5,18,TRIM,Map.of(),"");
        }
        galleries(cells,height);
        stairs(cells,height,half);
        put(cells,10,height/2,10,"omnira:dense_void_mass",Map.of(),"");
        return List.copyOf(cells.values());
    }
    private static void galleries(Map<BlockPos,Cell> cells,int height){
        for(int side=0;side<2;side++){
            int floor=side==0?8:height-8,sign=side==0?1:-1;
            for(int x=3;x<=17;x++)for(int z=3;z<=17;z++){
                double r=Math.hypot(x-10,z-10);
                if(r>=3.5&&r<=7.5)put(cells,x,floor,z,TRIM,Map.of(),"");
            }
            for(int x:new int[]{6,14})put(cells,x,floor+sign,10,"omnira:crystal_ball",
                    Map.of("facing",x<10?"east":"west","mount",side==0?"up":"down"),
                    "{id:\"omnira:crystal_ball\",LootTable:\"omnira:chests/skyreach_supplies\"}");
            put(cells,10,floor+sign,6,"omnira:liquid_crystal_ball",Map.of("facing","south","mount",side==0?"up":"down"),
                    "{id:\"omnira:liquid_crystal_ball\",FluidTreasure:1b}");
        }
    }
    private static void stairs(Map<BlockPos,Cell> cells,int height,int half){
        var path=new ArrayList<BlockPos>();
        int[][] corners={{-3,6},{3,6},{6,3},{6,-3},{3,-6},{-3,-6},{-6,-3},{-6,3}};
        for(int i=0;i<corners.length;i++){
            var a=corners[i];var b=corners[(i+1)%corners.length];int steps=Math.max(Math.abs(b[0]-a[0]),Math.abs(b[1]-a[1]));
            for(int s=0;s<steps;s++)path.add(new BlockPos(10+a[0]+Integer.signum(b[0]-a[0])*s,0,10+a[1]+Integer.signum(b[1]-a[1])*s));
        }
        for(int d=0;d<=half;d++)for(int step=0;step<2;step++){
            var p=path.get((d*2+step)%path.size());
            int ix=Integer.signum(10-p.getX()),iz=Integer.signum(10-p.getZ());
            for(var at:new LinkedHashSet<>(List.of(p,p.offset(ix,0,0),p.offset(0,0,iz),p.offset(ix,0,iz)))){
                put(cells,at.getX(),d,at.getZ(),"omnira:engraved_mirror_rock_slab",Map.of("type",step==0?"bottom":"double","waterlogged","false"),"");
                put(cells,at.getX(),height-d,at.getZ(),"omnira:engraved_mirror_rock_slab",Map.of("type",step==0?"top":"double","waterlogged","false"),"");
                for(int air=1;air<=3;air++){
                    put(cells,at.getX(),d+air,at.getZ(),"minecraft:air",Map.of(),"");
                    put(cells,at.getX(),height-d-air,at.getZ(),"minecraft:air",Map.of(),"");
                }
            }
        }
    }
    private static void put(Map<BlockPos,Cell> cells,int x,int y,int z,String id,Map<String,String> properties,String nbt){
        var pos=new BlockPos(x,y,z);cells.put(pos,new Cell(pos,id,properties,"",List.of(),nbt,false));
    }
    private SkyreachPlan(){}
}
