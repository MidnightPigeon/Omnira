package com.mcmagic.omnira.archaeology;

import com.mcmagic.omnira.mire.MireContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Small sealed aquifers within individual eras, leaving the separators intact. */
public final class EpochalAquifers {
    private record Key(int x,int z,int era){}
    private static final java.util.Map<Key,java.util.Optional<BlockPos>> CENTERS=new java.util.LinkedHashMap<>(256,.75F,true){
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<Key,java.util.Optional<BlockPos>> entry){return size()>2048;}
    };
    public static synchronized BlockPos center(int cellX,int cellZ,int era){
        return CENTERS.computeIfAbsent(new Key(cellX,cellZ,era),k->java.util.Optional.ofNullable(findCenter(k.x,k.z,k.era))).orElse(null);
    }
    private static BlockPos findCenter(int cellX,int cellZ,int era){
        int h=EpochalTerrain.hash(cellX,era,cellZ);
        if(Math.floorMod(h,3)!=0)return null;
        int x=cellX*32+8+Math.floorMod(h>>>5,16),z=cellZ*32+8+Math.floorMod(h>>>13,16);
        var column=EpochalTerrain.column(x,z);
        int min=256,max=0;
        for(int y=EpochalTerrain.BASE+1;y<=column.top();y++)if(EpochalTerrain.era(y,column)==era){min=Math.min(min,y);max=y;}
        int y=(min+max)/2;
        if(max-min<3||y+2>column.surface())return null;
        for(int dx:new int[]{-6,0,6})for(int dz:new int[]{-6,0,6}){
            var edge=EpochalTerrain.column(x+dx,z+dz);
            if(y+2>edge.surface()||EpochalTerrain.era(y-1,edge)!=era||EpochalTerrain.era(y+2,edge)!=era)return null;
        }
        return new BlockPos(x,y,z);
    }
    public static BlockState block(int x,int y,int z,int era){
        var center=center(Math.floorDiv(x,32),Math.floorDiv(z,32),era);
        if(center==null)return null;
        int dx=x-center.getX(),dz=z-center.getZ(),dy=y-center.getY();
        if(dy< -1||dy>1||dx*dx+dz*dz>20-(dy==1?7:0))return null;
        return dy<=0?MireContent.LIQUID.get().defaultBlockState():Blocks.AIR.defaultBlockState();
    }
    private EpochalAquifers(){}
}
