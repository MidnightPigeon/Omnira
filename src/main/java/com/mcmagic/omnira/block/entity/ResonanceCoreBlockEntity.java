package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class ResonanceCoreBlockEntity extends BlockEntity {
    public java.util.UUID identity=java.util.UUID.randomUUID(),owner,terminal;
    public BlockPos peer;
    public java.util.UUID peerIdentity;
    public boolean output;
    public long selectedUntil;
    public ResonanceCoreBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.RESONANCE_CORE.get(),pos,state);}
    public IItemHandler items() {
        if(level==null || !level.hasChunkAt(worldPosition.below()))return null;
        var chamber=com.mcmagic.omnira.alchemy.catalysis.CatalysisBlock.controller(level,worldPosition.below());
        if(chamber!=null)return chamber.complete()?chamber.nodeItems:null;
        if(level.getBlockEntity(worldPosition.below()) instanceof LiquidCrystalBallBlockEntity)return null;
        var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,worldPosition.below(),Direction.UP);
        if(handler!=null)return handler;
        return level.getBlockEntity(worldPosition.below()) instanceof net.minecraft.world.Container c?new net.neoforged.neoforge.items.wrapper.InvWrapper(c):null;
    }
    public IFluidHandler fluids() {
        if(level==null || !level.hasChunkAt(worldPosition.below()))return null;
        var chamber=com.mcmagic.omnira.alchemy.catalysis.CatalysisBlock.controller(level,worldPosition.below());
        if(chamber!=null)return chamber.complete()?chamber.nodeFluids:null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK,worldPosition.below(),Direction.UP);
    }
    public int kind(){return level!=null && level.getBlockEntity(worldPosition.below()) instanceof VoidCrystalBallBlockEntity?5:level!=null && level.getBlockEntity(worldPosition.below()) instanceof com.mcmagic.omnira.shop.MarisaOrbBlockEntity?4:fluids()!=null?3:items()!=null?2:0;}
    public boolean permits(net.minecraft.world.entity.player.Player player) {
        return level!=null && player.getUUID().equals(owner) && level.mayInteract(player,worldPosition)
                && level.mayInteract(player,worldPosition.below())
                && (!(level.getBlockEntity(worldPosition.below()) instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity container) || container.canOpen(player));
    }
    public void refresh() {
        if(level==null || level.isClientSide)return;
        if(level instanceof net.minecraft.server.level.ServerLevel server
                &&com.mcmagic.omnira.storage.TuningOutputs.get(server).consumeRemovedLink(identity,peerIdentity)){
            peer=null;peerIdentity=null;selectedUntil=0;setChanged();
        }
        if(level instanceof net.minecraft.server.level.ServerLevel server
                &&com.mcmagic.omnira.item.ResonanceLinks.get(server).consumeRemovedBinding(identity,terminal)){
            terminal=null;setChanged();
        }
        if(peer!=null&&level.hasChunkAt(peer)&&(!(level.getBlockEntity(peer) instanceof ResonanceCoreBlockEntity other)||!identity.equals(other.peerIdentity))){
            peer=null;peerIdentity=null;setChanged();
        }
        int kind=kind(),mode=kind==0?0:terminal==null?1:kind;
        if(getBlockState().getValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE)!=mode)
            level.setBlockAndUpdate(worldPosition,getBlockState().setValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE,mode));
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);tag.putUUID("Identity",identity);
        if(owner!=null)tag.putUUID("Owner",owner);if(terminal!=null)tag.putUUID("Terminal",terminal);
        if(peer!=null&&peerIdentity!=null){tag.putLong("Peer",peer.asLong());tag.putUUID("PeerIdentity",peerIdentity);tag.putBoolean("Output",output);}
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        selectedUntil=tag.getLong("SelectedUntil");
        super.loadAdditional(tag,registries);identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):java.util.UUID.randomUUID();
        owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;terminal=tag.hasUUID("Terminal")?tag.getUUID("Terminal"):null;
        peer=tag.hasUUID("PeerIdentity")?BlockPos.of(tag.getLong("Peer")):null;
        peerIdentity=tag.hasUUID("PeerIdentity")?tag.getUUID("PeerIdentity"):null;output=tag.getBoolean("Output");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=saveWithoutMetadata(registries);tag.putLong("SelectedUntil",selectedUntil);return tag;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public void setChanged(){super.setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
}
