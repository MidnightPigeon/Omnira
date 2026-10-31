package com.mcmagic.omnira.fate;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SuitcaseEntity extends BlockEntity {
    private ItemStack display=ItemStack.EMPTY;
    private CompoundTag musicBag=new CompoundTag();
    private int track=-1,remaining,effectAge;
    private long performance;
    public SuitcaseEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.SUITCASE.get(),pos,state);}
    public ItemStack displayed(){return display;}
    public boolean musical(){return display.is(ModItems.VOYAGER_VIOLIN.get());}
    public int track(){return track;}
    public long performance(){return performance;}
    public void tickPerformance(){
        if(!musical())return;
        if(track<0||--remaining<=0){
            track=com.mcmagic.omnira.music.ViolinTracks.draw(musicBag,level.random);
            remaining=com.mcmagic.omnira.music.ViolinTracks.ticks(track);performance++;changed();
        }
        if(effectAge++%25==0)com.mcmagic.omnira.music.VoyagerPerformance.affectAround(level,worldPosition.getCenter(),null);
    }
    public boolean insert(ItemStack source,boolean creative){
        if(!display.isEmpty()||source.isEmpty())return false;
        display=source.copyWithCount(1);if(!creative)source.shrink(1);changed();return true;
    }
    public ItemStack removeDisplay(){var result=display;display=ItemStack.EMPTY;track=-1;remaining=effectAge=0;if(!result.isEmpty())changed();return result;}
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);if(!display.isEmpty())tag.put("Display",display.save(registries));tag.put("MusicBag",musicBag);tag.putLong("Performance",performance);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);display=ItemStack.parseOptional(registries,tag.getCompound("Display"));if(!display.isEmpty())display.setCount(1);musicBag=tag.getCompound("MusicBag");performance=tag.getLong("Performance");track=tag.contains("PlayingTrack")?tag.getInt("PlayingTrack"):-1;remaining=effectAge=0;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=new CompoundTag();saveAdditional(tag,registries);tag.putInt("PlayingTrack",track);return tag;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
