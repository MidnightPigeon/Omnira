package com.mcmagic.omnira.space.anchor;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Shared, bounded one-minute activation for anchors and stabilized stellar orbs. */
public final class SpatialActivation {
    private final BlockEntity owner;
    private final int radius,side,volume;
    private final Map<BlockPos,Progress> progress=new LinkedHashMap<>();
    private record Progress(BlockState original,int ticks){}
    private int cursor;
    public SpatialActivation(BlockEntity owner,int radius){this.owner=owner;this.radius=radius;side=radius*2+1;volume=side*side*side;}
    public boolean begin(BlockPos pos){
        var level=owner.getLevel();
        if(level==null||progress.size()>=128||pos.distSqr(owner.getBlockPos())>radius*radius||!level.hasChunkAt(pos)
                ||StabilityAnchorEntity.result(level.getBlockState(pos))==null)return false;
        if(progress.putIfAbsent(pos.immutable(),new Progress(level.getBlockState(pos),0))!=null)return false;
        owner.setChanged();return true;
    }
    private static int offset(int n){return n%2==0?-n/2:(n+1)/2;}
    public void tick(ServerLevel level){
        long now=level.getGameTime();
        var it=progress.entrySet().iterator();
        while(it.hasNext()){
            var entry=it.next();var pos=entry.getKey();if(!level.hasChunkAt(pos))continue;
            var old=entry.getValue();if(!level.getBlockState(pos).equals(old.original())){it.remove();owner.setChanged();continue;}
            int ticks=old.ticks()+1;
            if(ticks>=1200){level.setBlockAndUpdate(pos,StabilityAnchorEntity.result(old.original()));it.remove();owner.setChanged();}
            else{entry.setValue(new Progress(old.original(),ticks));if(now%10==0)StabilityAnchorEntity.orbit(level,Vec3.atCenterOf(pos),.65,now+pos.asLong()%97);}
        }
        for(int n=0;n<2048&&progress.size()<128;n++){
            int index=cursor++;if(cursor==volume)cursor=0;
            begin(owner.getBlockPos().offset(offset(index%side),offset(index/(side*side)),offset(index/side%side)));
        }
        if(now%20==0&&!progress.isEmpty())owner.setChanged();
    }
    public void clear(){if(!progress.isEmpty()){progress.clear();owner.setChanged();}}
    public void save(CompoundTag tag){
        tag.putInt("Cursor",cursor);var list=new ListTag();
        for(var entry:progress.entrySet()){var item=new CompoundTag();item.putLong("Pos",entry.getKey().asLong());item.put("State",NbtUtils.writeBlockState(entry.getValue().original()));item.putInt("Ticks",entry.getValue().ticks());list.add(item);}tag.put("Conversions",list);
    }
    public void load(CompoundTag tag,HolderLookup.Provider provider){
        cursor=Math.floorMod(tag.getInt("Cursor"),volume);progress.clear();
        for(var raw:tag.getList("Conversions",10)){
            var item=(CompoundTag)raw;var pos=BlockPos.of(item.getLong("Pos"));
            var state=NbtUtils.readBlockState(provider.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),item.getCompound("State"));
            if(progress.size()<128&&pos.distSqr(owner.getBlockPos())<=radius*radius&&StabilityAnchorEntity.result(state)!=null)
                progress.put(pos,new Progress(state,Math.clamp(item.getInt("Ticks"),0,1199)));
        }
    }
}
