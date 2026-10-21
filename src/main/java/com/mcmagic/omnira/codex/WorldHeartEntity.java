package com.mcmagic.omnira.codex;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WorldHeartEntity extends BlockEntity {
    public HeartLink link;
    public int diameter=7,fade;
    public WorldHeartEntity(BlockPos p,BlockState s){super(MyriadCodexContent.HEART_ENTITY.get(),p,s);}
    public void tick(){
        if(link==null){level.removeBlock(worldPosition,false);return;}
        var codex=link.find(level.getServer());
        if(codex==null||!worldPosition.equals(codex.heartPos)){level.removeBlock(worldPosition,false);return;}
        if(diameter!=codex.diameter()||fade!=codex.fade){diameter=codex.diameter();fade=codex.fade;setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);if(link!=null)tag.put("Link",link.save());tag.putInt("Diameter",diameter);tag.putInt("Fade",fade);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);link=HeartLink.load(tag.getCompound("Link"));diameter=tag.getInt("Diameter");fade=tag.getInt("Fade");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=new CompoundTag();saveAdditional(t,r);return t;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
