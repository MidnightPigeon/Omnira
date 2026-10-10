package com.mcmagic.omnira.space.phase;

import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.space.FoldedTerrain;
import com.mcmagic.omnira.spacetime.TimeBiomeLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class PhaseTerrain {
    private static final ThreadLocal<java.util.Map<Long,java.util.Optional<BlockPos>>> CENTERS=ThreadLocal.withInitial(()->new java.util.LinkedHashMap<>(128,.75F,true){
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<Long,java.util.Optional<BlockPos>> e){return size()>1024;}
    });
    public static boolean biome(int x,int z){return TimeBiomeLayout.sample(x+8192,z-4096).region()==3;}
    private static int hash(int x,int z){int n=x*73428767^z*1274126177;return (n^(n>>>16))*0x7feb352d;}
    public static BlockPos center(int cx,int cz){
        long key=((long)cx<<32)^(cz&0xffffffffL);
        return CENTERS.get().computeIfAbsent(key,k->java.util.Optional.ofNullable(find(cx,cz))).orElse(null);
    }
    private static BlockPos find(int cx,int cz){
        int seed=hash(cx,cz);if(Math.floorMod(seed,4)==0)return null;
        int x=cx*24+8+Math.floorMod(seed>>>4,8),z=cz*24+8+Math.floorMod(seed>>>12,8);
        for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++)if(!biome(x+dx,z+dz))return null;
        return new BlockPos(x,FoldedTerrain.floor(x,z)+1,z);
    }
    public static boolean large(int cx,int cz){return Math.floorMod(hash(cx,cz)>>>20,3)!=0;}
    public static BlockState block(int x,int y,int z){
        var p=center(Math.floorDiv(x,24),Math.floorDiv(z,24));
        if(p!=null){
            if(large(Math.floorDiv(x,24),Math.floorDiv(z,24))&&PhaseGrowth.form().contains(new BlockPos(x-p.getX(),y-p.getY(),z-p.getZ())))return PhaseContent.BLOCK.get().defaultBlockState();
            if(!large(Math.floorDiv(x,24),Math.floorDiv(z,24))&&p.getX()==x&&p.getY()==y&&p.getZ()==z)return PhaseContent.CLUSTER.get().defaultBlockState();
        }
        if(y<=FoldedTerrain.floor(x,z))return DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState();
        if(y>=FoldedTerrain.roof(x,z))return DreamContent.SPATIAL_CRYSTAL.get().defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }
    private PhaseTerrain(){}
}
