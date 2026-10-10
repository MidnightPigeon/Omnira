package com.mcmagic.omnira.space;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spacetime.TimeBiomeLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Coordinate-stable cave relief and small shelves surrounding upper fold points. */
public final class FoldedTerrain {
    public static boolean biome(int x,int z){return TimeBiomeLayout.sample(x+8192,z-4096).region()==0;}
    public static int floor(int x,int z){return CaveSurface.landFloor(x,z);}
    public static int roof(int x,int z){return 92+(int)Math.round(4*Math.cos(x/57.0)*Math.sin(z/43.0)+2*Math.cos((x-z)/23.0));}
    private static int hash(int x,int z){int n=x*73428767^z*1274126177;return (n^(n>>>16))*0x7feb352d;}
    public static BlockPos point(int cx,int cz){
        int h=hash(cx,cz);if(Math.floorMod(h,3)==0)return null;
        int x=cx*32+12+Math.floorMod(h>>>4,8),z=cz*32+12+Math.floorMod(h>>>12,8);
        if(!biome(x,z))return null;
        return new BlockPos(x,(h&1)==0?floor(x,z)+2:roof(x,z)-9,z);
    }
    public static BlockState decoration(int x,int y,int z){
        if(!biome(x,z))return null;
        var p=point(Math.floorDiv(x,32),Math.floorDiv(z,32));if(p==null)return null;
        int dx=x-p.getX(),dz=z-p.getZ(),r2=dx*dx+dz*dz;
        if(p.getY()>65){
            int dy=y-(p.getY()-2);
            if(dy<=0&&dy>=-3&&r2<=Math.pow(8+dy,2))return DreamContent.SPATIAL_CRYSTAL.get().defaultBlockState();
            if(r2>=49&&r2<=64&&Math.abs(dx)<=1&&y>=p.getY()-2&&y<=roof(x,z))return DreamContent.SPATIAL_CRYSTAL.get().defaultBlockState();
        }
        if(r2==0&&y==p.getY())return ModBlocks.SPACE_WARP_POINT.get().defaultBlockState();
        // Keep the point clear of the existing crystal cones.
        if(r2<=64&&y>=p.getY()-1&&y<=p.getY()+3)return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return null;
    }
    private FoldedTerrain(){}
}
