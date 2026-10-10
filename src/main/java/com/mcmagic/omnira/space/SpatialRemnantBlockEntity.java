package com.mcmagic.omnira.space;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Each remnant pools movement from all entities; only earned distance survives unloading. */
public final class SpatialRemnantBlockEntity extends BlockEntity {
    public static final double REQUIRED_DISTANCE=100;
    private final Map<UUID,Vec3> previous=new HashMap<>();
    private long lastTick=Long.MIN_VALUE;
    private double distance;

    public SpatialRemnantBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.SPATIAL_REMNANT.get(),pos,state);}
    public double distance(){return distance;}
    public void restoreDistance(double value){
        distance=Double.isFinite(value)?Math.clamp(value,0,REQUIRED_DISTANCE):0;
        previous.clear();lastTick=Long.MIN_VALUE;setChanged();
    }
    public void tick(){
        if(!(level instanceof ServerLevel server)||!server.getBlockState(worldPosition).is(ModBlocks.SPATIAL_REMNANT.get()))return;
        long now=server.getGameTime();
        if(lastTick==now)return;
        if(lastTick!=now-1)previous.clear();
        lastTick=now;
        var current=new HashMap<UUID,Vec3>();
        Vec3 center=Vec3.atCenterOf(worldPosition);
        double moved=0;
        for(var entity:server.getEntities((Entity)null,new AABB(worldPosition).inflate(5),e->e.isAlive()&&!e.isSpectator())){
            if(!SpaceWarpBlockEntity.touches(entity.getBoundingBox(),center))continue;
            Vec3 point=entity.position(),old=previous.get(entity.getUUID());
            if(old!=null)moved+=point.distanceTo(old);
            current.put(entity.getUUID(),point);
        }
        previous.clear();previous.putAll(current);
        if(moved>0){distance=Math.min(REQUIRED_DISTANCE,distance+moved);setChanged();}
        if(distance>=REQUIRED_DISTANCE){
            server.setBlockAndUpdate(worldPosition,ModBlocks.SPACE_WARP_POINT.get().defaultBlockState());
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putDouble("MovementDistance",distance);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);restoreDistance(tag.getDouble("MovementDistance"));
    }
}
