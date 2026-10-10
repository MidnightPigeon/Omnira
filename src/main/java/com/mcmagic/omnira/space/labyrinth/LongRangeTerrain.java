package com.mcmagic.omnira.space.labyrinth;

import com.mcmagic.omnira.archaeology.*;
import com.mcmagic.omnira.spacetime.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Sparse shafts cut into existing deep Epochal Cliffs, never through the solidified seal. */
public final class LongRangeTerrain {
    private static final int CELL=32,RADIUS=4;
    private static final ThreadLocal<Map<Long,Optional<BlockPos>>> CACHE=ThreadLocal.withInitial(()->new LinkedHashMap<>(128,.75F,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Long,Optional<BlockPos>> e){return size()>256;}
    });
    public static BlockPos portal(int cx,int cz){
        long key=((long)cx<<32)^(cz&0xffffffffL);
        return CACHE.get().computeIfAbsent(key,k->Optional.ofNullable(find(cx,cz))).orElse(null);
    }
    private static BlockPos find(int cx,int cz){
        BlockPos best=null;int floor=Integer.MAX_VALUE;
        for(int x=RADIUS+1;x<CELL-RADIUS-1;x++)for(int z=RADIUS+1;z<CELL-RADIUS-1;z++){
            int px=cx*CELL+x,pz=cz*CELL+z;
            if(!StrataBiomeSource.epochalCliffs(px,pz))continue;
            var c=EpochalTerrain.column(px,pz);
            if(c.surface()>CorridorLayout.TIME_BASE+14||c.surface()>=floor)continue;
            boolean inside=true;
            for(var d:Direction.Plane.HORIZONTAL)if(!StrataBiomeSource.epochalCliffs(px+d.getStepX()*5,pz+d.getStepZ()*5)){inside=false;break;}
            if(inside){best=new BlockPos(px,CorridorLayout.TIME_BASE+1,pz);floor=c.surface();}
        }
        return best;
    }
    public static int swirlFloor(BlockPos portal,int x,int z){
        int dx=x-portal.getX(),dz=z-portal.getZ();double radius=Math.sqrt(dx*dx+dz*dz);
        if(radius<1)return portal.getY();
        double angle=Math.atan2(dz,dx)+radius*.8;
        int spiral=Math.cos(angle*3)>.45?1:0;
        return Math.min(EpochalTerrain.column(x,z).surface(),portal.getY()+(int)Math.round(radius*2)+spiral);
    }
    public static BlockState block(int x,int y,int z){
        if(y<=CorridorLayout.TIME_BASE||!StrataBiomeSource.epochalCliffs(x,z))return null;
        var portal=portal(Math.floorDiv(x,CELL),Math.floorDiv(z,CELL));if(portal==null)return null;
        int dx=x-portal.getX(),dz=z-portal.getZ();if(dx*dx+dz*dz>RADIUS*RADIUS)return null;
        if(x==portal.getX()&&z==portal.getZ()&&y==portal.getY())return LabyrinthContent.LONG_RANGE_PORTAL.get().defaultBlockState().setValue(VoidPortalBlock.FACING,Direction.DOWN);
        int floor=swirlFloor(portal,x,z);
        if(y>floor)return Blocks.AIR.defaultBlockState();
        return y==floor?ArchaeologyContent.SAND.get().defaultBlockState():null;
    }
    private LongRangeTerrain(){}
}
