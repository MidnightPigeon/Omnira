package com.mcmagic.omnira.space;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Per-dimension point positions, including player-relocated and unloaded points. */
public final class SpaceWarpIndex extends SavedData {
    private final Set<BlockPos> points=new HashSet<>();
    public static SpaceWarpIndex get(ServerLevel level){return level.getDataStorage().computeIfAbsent(new Factory<>(SpaceWarpIndex::new,SpaceWarpIndex::load,null),"omnira_space_warp_points");}
    public void add(BlockPos p){if(points.add(p.immutable()))setDirty();}
    public void remove(BlockPos p){if(points.remove(p))setDirty();}
    public List<BlockPos> nearby(ServerLevel level,BlockPos from){
        var all=new HashSet<>(points);
        if(level.dimension()==com.mcmagic.omnira.world.dimension.ModDimensions.SPACETIME_CORRIDOR)
            for(int x=Math.floorDiv(from.getX()-128,32);x<=Math.floorDiv(from.getX()+128,32);x++)
                for(int z=Math.floorDiv(from.getZ()-128,32);z<=Math.floorDiv(from.getZ()+128,32);z++){
                    var p=FoldedTerrain.point(x,z);if(p!=null)all.add(p);
                }
        return all.stream().filter(p->!p.equals(from)&&Math.pow((double)p.getX()-from.getX(),2)+Math.pow((double)p.getZ()-from.getZ(),2)<=128*128)
                .sorted(Comparator.comparingLong(BlockPos::asLong)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    public static SpaceWarpIndex load(CompoundTag tag,HolderLookup.Provider registries){var data=new SpaceWarpIndex();for(long p:tag.getLongArray("Points"))data.points.add(BlockPos.of(p));return data;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){tag.putLongArray("Points",points.stream().mapToLong(BlockPos::asLong).toArray());return tag;}
}
