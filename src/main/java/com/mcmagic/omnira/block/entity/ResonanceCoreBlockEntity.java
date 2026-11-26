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
    public net.neoforged.neoforge.fluids.FluidStack pendingDelivery=net.neoforged.neoforge.fluids.FluidStack.EMPTY;
    public void retainDelivery(net.neoforged.neoforge.fluids.FluidStack fluid){pendingDelivery=fluid;setChanged();}
    public long selectedUntil;
    private boolean relocated;
    public ResonanceCoreBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.RESONANCE_CORE.get(),pos,state);}
    public IItemHandler items() {
        if(level==null || !level.hasChunkAt(worldPosition.below()) || com.mcmagic.omnira.compat.LootrOrbs.blocksAutomation(level.getBlockEntity(worldPosition.below())))return null;
        var chamber=com.mcmagic.omnira.alchemy.catalysis.CatalysisBlock.controller(level,worldPosition.below());
        if(chamber!=null)return chamber.complete()?chamber.nodeItems:null;
        if(level.getBlockEntity(worldPosition.below()) instanceof LiquidCrystalBallBlockEntity)return null;
        var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,worldPosition.below(),Direction.UP);
        if(handler!=null)return handler;
        return level.getBlockEntity(worldPosition.below()) instanceof net.minecraft.world.Container c?new net.neoforged.neoforge.items.wrapper.InvWrapper(c):null;
    }
    public IFluidHandler fluids() {
        if(level==null || !level.hasChunkAt(worldPosition.below()) || com.mcmagic.omnira.compat.LootrOrbs.blocksAutomation(level.getBlockEntity(worldPosition.below())))return null;
        var chamber=com.mcmagic.omnira.alchemy.catalysis.CatalysisBlock.controller(level,worldPosition.below());
        if(chamber!=null)return chamber.complete()?chamber.nodeFluids:null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK,worldPosition.below(),Direction.UP);
    }
    public static final int MIXED=6;
    private static final Direction[] FACES={Direction.UP,null,Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    public BlockPos menuPosition(){
        return com.mcmagic.omnira.storage.StorageContainerIdentity.menuPosition(level,worldPosition.below());
    }
    public int kind(){
        if(level==null || !level.hasChunkAt(worldPosition.below()))return 0;
        var pos=menuPosition();
        var target=level.getBlockEntity(pos);
        if(target==null || com.mcmagic.omnira.compat.LootrOrbs.blocksAutomation(target))return 0;
        if(target instanceof VoidCrystalBallBlockEntity)return 5;
        if(target instanceof com.mcmagic.omnira.shop.MarisaOrbBlockEntity)return 4;
        if(target instanceof LiquidCrystalBallBlockEntity)return 3;
        com.mcmagic.omnira.compat.SophisticatedBackpackMenus.prepare(target);
        boolean item=hasItemCapacity(items());
        if(!item && target instanceof net.minecraft.world.Container container)item=hasInventorySpace(container);
        if(!item && target instanceof AssemblyAccess assembly)item=hasInventorySpace(assembly.assembly());
        if(!item && target instanceof ManaEngineAccess engine)item=hasInventorySpace(engine.engineState().inventory);
        boolean fluid=hasFluidCapacity(fluids());
        if(target instanceof com.mcmagic.omnira.alchemy.ReactionPoolBlockEntity pool){
            item|=hasItemCapacity(pool.nodeItems);
            fluid|=hasFluidCapacity(pool.nodeFluids);
        }
        boolean energy=false;
        var state=target.getBlockState();
        for(var face:FACES){
            if(item && fluid)return MIXED;
            if(!item)item=hasItemCapacity(Capabilities.ItemHandler.BLOCK.getCapability(level,pos,state,target,face));
            if(!fluid)fluid=hasFluidCapacity(Capabilities.FluidHandler.BLOCK.getCapability(level,pos,state,target,face));
            if(!energy){
                var handler=Capabilities.EnergyStorage.BLOCK.getCapability(level,pos,state,target,face);
                energy=handler!=null && handler.getMaxEnergyStored()>0;
            }
            if((item && fluid) || (energy && (item || fluid)))return MIXED;
        }
        if(fluid)return 3;
        if(item)return 2;
        return energy || com.mcmagic.omnira.storage.RemoteContainerAccess.provider(level,pos)!=null?7:0;
    }
    private static boolean hasInventorySpace(net.minecraft.world.Container inventory){
        return inventory!=null && inventory.getContainerSize()>0 && inventory.getMaxStackSize()>0;
    }
    private static boolean hasItemCapacity(IItemHandler handler){
        if(handler==null)return false;
        for(int slot=0,slots=handler.getSlots();slot<slots;slot++){
            if(handler.getSlotLimit(slot)>0)return true;
        }
        return false;
    }
    private static boolean hasFluidCapacity(IFluidHandler handler){
        if(handler==null)return false;
        for(int tank=0,tanks=handler.getTanks();tank<tanks;tank++){
            if(handler.getTankCapacity(tank)>0)return true;
        }
        return false;
    }
    public boolean permits(net.minecraft.world.entity.player.Player player) {
        if(level==null)return false;
        var pos=menuPosition();var target=level.getBlockEntity(pos);
        return !com.mcmagic.omnira.compat.LootrOrbs.blocksAutomation(target) && player.getUUID().equals(owner) && level.mayInteract(player,worldPosition)
                && level.mayInteract(player,worldPosition.below())
                && (pos.equals(worldPosition.below())||level.mayInteract(player,pos))
                && com.mcmagic.omnira.compat.MekanismRemoteMenus.permits(player,target)
                && (!(target instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity container) || container.canOpen(player));
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
        if(peer!=null&&(relocated||level.hasChunkAt(peer))&&(!level.hasChunkAt(peer)||!(level.getBlockEntity(peer) instanceof ResonanceCoreBlockEntity other)
                ||!peerIdentity.equals(other.identity)||!identity.equals(other.peerIdentity)||!worldPosition.equals(other.peer))){
            peer=null;peerIdentity=null;setChanged();
        }
        relocated=false;
        int kind=kind(),mode=kind==0?0:terminal==null?1:kind;
        if(getBlockState().getValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE)!=mode)
            level.setBlockAndUpdate(worldPosition,getBlockState().setValue(com.mcmagic.omnira.block.ResonanceCoreBlock.MODE,mode));
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);tag.putUUID("Identity",identity);tag.putLong("NodeAnchor",worldPosition.asLong());
        com.mcmagic.omnira.storage.FluidDelivery.write(tag,registries,pendingDelivery);
        if(owner!=null)tag.putUUID("Owner",owner);if(terminal!=null)tag.putUUID("Terminal",terminal);
        if(peer!=null&&peerIdentity!=null){tag.putLong("Peer",peer.asLong());tag.putUUID("PeerIdentity",peerIdentity);tag.putBoolean("Output",output);}
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        selectedUntil=tag.getLong("SelectedUntil");
        pendingDelivery=com.mcmagic.omnira.storage.FluidDelivery.read(tag,registries);
        super.loadAdditional(tag,registries);identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):java.util.UUID.randomUUID();
        owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;terminal=tag.hasUUID("Terminal")?tag.getUUID("Terminal"):null;
        peer=tag.hasUUID("PeerIdentity")?BlockPos.of(tag.getLong("Peer")):null;
        peerIdentity=tag.hasUUID("PeerIdentity")?tag.getUUID("PeerIdentity"):null;output=tag.getBoolean("Output");
        relocated=tag.contains("NodeAnchor")&&!worldPosition.equals(BlockPos.of(tag.getLong("NodeAnchor")));
        if(relocated){
            if(peer!=null)peer=peer.offset(worldPosition.subtract(BlockPos.of(tag.getLong("NodeAnchor"))));
            terminal=null;selectedUntil=0;
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=saveWithoutMetadata(registries);tag.putLong("SelectedUntil",selectedUntil);return tag;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public void setChanged(){super.setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
}
