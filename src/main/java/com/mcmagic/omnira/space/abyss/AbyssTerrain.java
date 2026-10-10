package com.mcmagic.omnira.space.abyss;

import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.space.FoldedTerrain;
import com.mcmagic.omnira.spacetime.TimeBiomeLayout;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public final class AbyssTerrain {
    private record RiftColumn(int floor,int roof){}
    private static final ThreadLocal<java.util.Map<Long,RiftColumn>> RIFTS=ThreadLocal.withInitial(()->new java.util.LinkedHashMap<>(1024,.75F,true){
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<Long,RiftColumn> e){return size()>8192;}
    });
    private static final ThreadLocal<java.util.Map<Long,java.util.Optional<BlockPos>>> CLUSTERS=ThreadLocal.withInitial(()->new java.util.LinkedHashMap<>(128,.75F,true){
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<Long,java.util.Optional<BlockPos>> e){return size()>512;}
    });
    public static boolean biome(int x,int z){return TimeBiomeLayout.sample(x+8192,z-4096).region()==2;}
    public static double middle(int x,int z){return (FoldedTerrain.floor(x,z)+1+FoldedTerrain.roof(x,z))/2.0;}
    private static int hash(int x,int z,int salt){int h=x*73428767^z*1274126177^salt;return (h^(h>>>16))*0x7feb352d;}
    private static double fissure(int x,int z,int salt){
        if(!biome(x,z))return 0;
        double strength=0;
        int cx=Math.floorDiv(x,192),cz=Math.floorDiv(z,192);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            int gx=cx+dx,gz=cz+dz,h=hash(gx,gz,salt);
            if(Math.floorMod(h,3)==0)continue;
            double centerX=gx*192+48+Math.floorMod(h>>>4,96),centerZ=gz*192+48+Math.floorMod(h>>>12,96);
            double angle=Math.floorMod(h>>>20,180)*Math.PI/180;
            double px=x-centerX,pz=z-centerZ;
            double along=px*Math.cos(angle)+pz*Math.sin(angle),across=-px*Math.sin(angle)+pz*Math.cos(angle);
            double halfLength=56+Math.floorMod(h>>>8,25);
            if(Math.abs(along)>=halfLength)continue;
            double curve=5*Math.sin(along/24.0+(h&15));
            double width=(8+Math.floorMod(h>>>16,5))*Math.sqrt(Math.max(0,1-Math.pow(along/halfLength,2)));
            strength=Math.max(strength,Math.clamp((width-Math.abs(across-curve))/4,0,1));
        }
        if(strength==0)return 0;
        // Narrow back to zero before an actual biome boundary, not into the neighbouring cave.
        for(int distance:new int[]{4,8,12})for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
            if((dx!=0||dz!=0)&&!biome(x+dx*distance,z+dz*distance))strength=Math.min(strength,(distance-4)/8.0);
        return strength;
    }
    private static RiftColumn rifts(int x,int z){
        long key=((long)x<<32)^(z&0xffffffffL);
        return RIFTS.get().computeIfAbsent(key,k->{
            int floor=FoldedTerrain.floor(x,z),roof=FoldedTerrain.roof(x,z);
            return new RiftColumn(floor-(int)Math.round((floor-23)*fissure(x,z,0x27a49)),
                    roof+(int)Math.round((103-roof)*fissure(x,z,0x59c13)));
        });
    }
    public static int riftFloor(int x,int z){return rifts(x,z).floor;}
    public static int riftRoof(int x,int z){return rifts(x,z).roof;}
    public static BlockPos cluster(int cellX,int cellZ){
        long key=((long)cellX<<32)^(cellZ&0xffffffffL);
        return CLUSTERS.get().computeIfAbsent(key,k->java.util.Optional.ofNullable(calculateCluster(cellX,cellZ))).orElse(null);
    }
    private static BlockPos calculateCluster(int cellX,int cellZ){
        int h=cellX*73428767^cellZ*1274126177;h=(h^(h>>>16))*0x7feb352d;
        if(Math.floorMod(h,3)==0)return null;
        int x=cellX*24+7+Math.floorMod(h>>>4,10),z=cellZ*24+7+Math.floorMod(h>>>12,10);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(!biome(x+dx,z+dz))return null;
        int y=(int)Math.floor(middle(x,z));
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
            if(AbyssGravity.band(y-.5,middle(x+dx,z+dz))!=0||AbyssGravity.band(y+1.5,middle(x+dx,z+dz))!=0)return null;
        return new BlockPos(x,y,z);
    }
    public static BlockState block(int x,int y,int z){
        var cluster=cluster(Math.floorDiv(x,24),Math.floorDiv(z,24));
        if(cluster!=null){
            int distance=Math.abs(x-cluster.getX())+Math.abs(y-cluster.getY())+Math.abs(z-cluster.getZ());
            if(distance<=1)return (distance==0?DreamContent.EXCITED_SPATIAL_CRYSTAL:DreamContent.SPATIAL_CRYSTAL).get().defaultBlockState();
        }
        var surfaces=rifts(x,z);
        if(y>surfaces.floor&&y<surfaces.roof)return Blocks.AIR.defaultBlockState();
        int h=x*73428767^y*9123671^z*1274126177;h=(h^(h>>>16))*0x7feb352d;
        int n=Math.floorMod(h,20);
        return (n==0?DreamContent.VOID_MIRROR_ROCK:n<4?DreamContent.SPATIAL_CRYSTAL:DreamContent.EXCITED_SPATIAL_CRYSTAL).get().defaultBlockState();
    }
    private AbyssTerrain(){}
}
