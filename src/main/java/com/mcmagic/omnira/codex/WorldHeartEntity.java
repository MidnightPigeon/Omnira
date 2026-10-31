package com.mcmagic.omnira.codex;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WorldHeartEntity extends BlockEntity {
    public HeartLink link;
    public int diameter=7,fade,shape,axis=1;
    public WorldHeartEntity(BlockPos p,BlockState s){super(MyriadCodexContent.HEART_ENTITY.get(),p,s);}
    public void tick(){
        if(link==null){level.removeBlock(worldPosition,false);return;}
        var codex=link.find(level.getServer());
        if(codex==null||!worldPosition.equals(codex.heartPos)){level.removeBlock(worldPosition,false);return;}
        int previewShape=codex.mode==1?codex.shape:0;
        if(diameter!=codex.diameter()||fade!=codex.fade||shape!=previewShape||axis!=codex.axis){
            diameter=codex.diameter();fade=codex.fade;shape=previewShape;axis=codex.axis;
            setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);if(link!=null)tag.put("Link",link.save());tag.putInt("Diameter",diameter);tag.putInt("Fade",fade);tag.putInt("Shape",shape);tag.putInt("Axis",axis);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);link=HeartLink.load(tag.getCompound("Link"));diameter=tag.getInt("Diameter");fade=tag.getInt("Fade");shape=Math.clamp(tag.getInt("Shape"),0,2);axis=tag.contains("Axis")?Math.clamp(tag.getInt("Axis"),0,2):1;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=new CompoundTag();saveAdditional(t,r);return t;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
