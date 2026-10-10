package com.mcmagic.omnira.space.geodes;

import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.spacetime.TimeBiomeLayout;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shared cave relief, paired crystal cones and budding matrices at their exposed tips. */
public final class DislocatedTerrain {
    public static final int FLOOR=40,ROOF=96;
    public static int floor(int x,int z){return com.mcmagic.omnira.space.FoldedTerrain.floor(x,z);}
    public static int roof(int x,int z){return com.mcmagic.omnira.space.FoldedTerrain.roof(x,z);}
    public static boolean biome(int x,int z){return TimeBiomeLayout.sample(x+8192,z-4096).region()==1;}
    private static int hash(int x,int z){int n=x*73428767^z*1274126177;return (n^(n>>>16))*0x7feb352d;}
    public static BlockState block(int x,int y,int z){
        if(y<=floor(x,z)||y>=roof(x,z))return material(x,y,z);
        BlockState decoration=null;boolean solid=false,matrix=false;
        for(int cx=Math.floorDiv(x,16)-1;cx<=Math.floorDiv(x,16)+1;cx++)
            for(int cz=Math.floorDiv(z,16)-1;cz<=Math.floorDiv(z,16)+1;cz++){
                int seed=hash(cx,cz);if(Math.floorMod(seed,3)!=0)continue;
                int px=cx*16+4+Math.floorMod(seed>>>4,8),pz=cz*16+4+Math.floorMod(seed>>>12,8);
                if(!biome(px,pz))continue;
                int height=7+Math.floorMod(seed>>>20,9),radius=2+Math.floorMod(seed>>>8,3);
                int dx=x-px,dz=z-pz;
                for(boolean upper:new boolean[]{false,true}){
                    int distance=upper?roof(px,pz)-y:y-floor(px,pz);
                    if(distance>=0&&distance<=height){
                        int width=radius*(height-distance)/height;
                        if(dx*dx+dz*dz<=width*width){solid=true;if(distance==height&&dx==0&&dz==0)matrix=true;}
                    }
                    if(distance==height+1&&dx==0&&dz==0)
                        decoration=DeflectionContent.stages()[Math.floorMod(seed>>>24,4)].defaultBlockState()
                                .setValue(AmethystClusterBlock.FACING,upper?Direction.DOWN:Direction.UP);
                }
            }
        if(matrix)return DeflectionContent.MATRIX.get().defaultBlockState();
        if(solid)return material(x,y,z);
        return decoration==null?Blocks.AIR.defaultBlockState():decoration;
    }
    private static BlockState material(int x,int y,int z){
        if(Math.floorMod(hash(Math.floorDiv(x,4)+Math.floorDiv(y,4)*17,Math.floorDiv(z,4)),4)!=0)
            return DeflectionContent.BLOCK.get().defaultBlockState();
        return (Math.floorMod(hash(x+y*31,z-y*17),4)==0
                ?DreamContent.EXCITED_SPATIAL_CRYSTAL:DreamContent.SPATIAL_CRYSTAL).get().defaultBlockState();
    }
    private DislocatedTerrain(){}
}
