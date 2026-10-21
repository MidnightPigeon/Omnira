package com.mcmagic.omnira.space;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.UUID;

/** A live view of the other attached face, never a second inventory or transfer buffer. */
public final class SpacetimePortState {
    public final BlockEntity host;
    public UUID identity=UUID.randomUUID(),peerIdentity;
    public BlockPos peer;
    public boolean white;
    public long selectedUntil;
    public SpacetimePortState(BlockEntity host){this.host=host;}
    public Direction face(){return host.getBlockState().getValue(SpacetimePortBlock.FACING);}
    public BlockPos attached(){return host.getBlockPos().relative(face().getOpposite());}
    public static SpacetimePortState at(Level l,BlockPos p){return l!=null&&l.hasChunkAt(p)&&l.getBlockEntity(p) instanceof SpacetimePortHost h?h.port():null;}
    public SpacetimePortState other(){
        var l=host.getLevel();if(l==null||peer==null||host.isRemoved())return null;
        var other=at(l,peer);
        return other!=null&&!other.host.isRemoved()&&peerIdentity!=null&&peerIdentity.equals(other.identity)
                &&host.getBlockPos().equals(other.peer)&&identity.equals(other.peerIdentity)
                &&white!=other.white&&host.getBlockPos().distSqr(peer)<=32*32?other:null;
    }
    public boolean accessible(Direction side){return side==face().getOpposite()&&other()!=null;}
    private <T> T remote(BlockCapability<T,Direction> cap){
        var other=other();if(other==null)return null;
        var l=host.getLevel();if(!l.hasChunkAt(other.attached()))return null;var state=l.getBlockState(other.attached());
        if(state.getBlock() instanceof SpacetimePortBlock||state.isAir())return null;
        return l.getCapability(cap,other.attached(),other.face());
    }
    public static boolean pair(SpacetimePortState a,SpacetimePortState b){
        if(a==b||a.host.getLevel()==null||a.host.getLevel()!=b.host.getLevel()||a.peer!=null||b.peer!=null
                ||a.host.getBlockPos().distSqr(b.host.getBlockPos())>32*32||a.attached().equals(b.attached()))return false;
        ((SpacetimePortHost)a.host).detachPortKinetics();((SpacetimePortHost)b.host).detachPortKinetics();
        a.peer=b.host.getBlockPos();a.peerIdentity=b.identity;a.white=true;a.selectedUntil=0;
        b.peer=a.host.getBlockPos();b.peerIdentity=a.identity;b.white=false;b.selectedUntil=0;
        a.sync();b.sync();((SpacetimePortHost)a.host).attachPortKinetics();((SpacetimePortHost)b.host).attachPortKinetics();return true;
    }
    public void unlink(){
        if(host.getLevel()==null||host.getLevel().isClientSide)return;
        var other=other();((SpacetimePortHost)host).detachPortKinetics();
        if(other!=null)((SpacetimePortHost)other.host).detachPortKinetics();
        clear();sync();if(other!=null){other.clear();other.sync();((SpacetimePortHost)other.host).attachPortKinetics();}
        ((SpacetimePortHost)host).attachPortKinetics();
    }
    private void clear(){peer=null;peerIdentity=null;white=false;selectedUntil=0;}
    public void sync(){
        host.setChanged();var l=host.getLevel();if(l==null)return;
        com.mcmagic.omnira.pulse.PulseNetworks.changed(l,host.getBlockPos());
        l.invalidateCapabilities(host.getBlockPos());l.invalidateCapabilities(attached());
        l.sendBlockUpdated(host.getBlockPos(),host.getBlockState(),host.getBlockState(),3);
        l.updateNeighborsAt(host.getBlockPos(),host.getBlockState().getBlock());
        if(net.neoforged.fml.ModList.get().isLoaded("create"))com.mcmagic.omnira.compat.PortFluidUpdates.changed(l,attached());
    }
    public void tick(){
        var l=host.getLevel();if(l==null||l.isClientSide)return;
        if(peer!=null&&l.hasChunkAt(peer)&&other()==null){unlink();return;}
    }
    public void write(CompoundTag tag){
        tag.putLong("PortAnchor",host.getBlockPos().asLong());
        tag.putUUID("PortIdentity",identity);tag.putBoolean("PortWhite",white);tag.putLong("PortSelected",selectedUntil);
        if(peer!=null&&peerIdentity!=null){tag.putLong("PortPeer",peer.asLong());tag.putUUID("PortPeerIdentity",peerIdentity);}
    }
    public void read(CompoundTag tag){
        if(tag.hasUUID("PortIdentity"))identity=tag.getUUID("PortIdentity");
        white=tag.getBoolean("PortWhite");selectedUntil=tag.getLong("PortSelected");
        peer=tag.contains("PortPeer")?BlockPos.of(tag.getLong("PortPeer")):null;
        if(peer!=null&&tag.contains("PortAnchor"))
            peer=peer.offset(host.getBlockPos().subtract(BlockPos.of(tag.getLong("PortAnchor"))));
        peerIdentity=tag.hasUUID("PortPeerIdentity")?tag.getUUID("PortPeerIdentity"):null;
    }
    private final IItemHandler items=new IItemHandler(){
        private IItemHandler target(){return remote(Capabilities.ItemHandler.BLOCK);}
        public int getSlots(){var t=target();return t==null?0:t.getSlots();}
        public ItemStack getStackInSlot(int s){var t=target();return t==null||s<0||s>=t.getSlots()?ItemStack.EMPTY:t.getStackInSlot(s);}
        public ItemStack insertItem(int s,ItemStack stack,boolean simulate){var t=target();var result=t==null||s<0||s>=t.getSlots()?stack:t.insertItem(s,stack,simulate);if(!simulate)transferred(stack.getCount()-result.getCount());return result;}
        public ItemStack extractItem(int s,int n,boolean simulate){var t=target();var result=t==null||s<0||s>=t.getSlots()?ItemStack.EMPTY:t.extractItem(s,n,simulate);if(!simulate)transferred(result.getCount());return result;}
        public int getSlotLimit(int s){var t=target();return t==null||s<0||s>=t.getSlots()?0:t.getSlotLimit(s);}
        public boolean isItemValid(int s,ItemStack stack){var t=target();return t!=null&&s>=0&&s<t.getSlots()&&t.isItemValid(s,stack);}
    };
    private final IFluidHandler fluids=new IFluidHandler(){
        private IFluidHandler target(){return remote(Capabilities.FluidHandler.BLOCK);}
        public int getTanks(){var t=target();return t==null?0:t.getTanks();}
        public FluidStack getFluidInTank(int i){var t=target();return t==null||i<0||i>=t.getTanks()?FluidStack.EMPTY:t.getFluidInTank(i);}
        public int getTankCapacity(int i){var t=target();return t==null||i<0||i>=t.getTanks()?0:t.getTankCapacity(i);}
        public boolean isFluidValid(int i,FluidStack f){var t=target();return t!=null&&i>=0&&i<t.getTanks()&&t.isFluidValid(i,f);}
        public int fill(FluidStack f,FluidAction a){var t=target();int result=t==null?0:t.fill(f,a);if(a.execute())transferred(result);return result;}
        public FluidStack drain(FluidStack f,FluidAction a){var t=target();var result=t==null?FluidStack.EMPTY:t.drain(f,a);if(a.execute())transferred(result.getAmount());return result;}
        public FluidStack drain(int n,FluidAction a){var t=target();var result=t==null?FluidStack.EMPTY:t.drain(n,a);if(a.execute())transferred(result.getAmount());return result;}
    };
    private final IEnergyStorage energy=new IEnergyStorage(){
        private IEnergyStorage target(){return remote(Capabilities.EnergyStorage.BLOCK);}
        public int receiveEnergy(int n,boolean sim){var t=target();int result=t!=null?t.receiveEnergy(n,sim):0;if(!sim)transferred(result);return result;}
        public int extractEnergy(int n,boolean sim){var t=target();int result=t!=null?t.extractEnergy(n,sim):0;if(!sim)transferred(result);return result;}
        public int getEnergyStored(){var t=target();return t==null?0:t.getEnergyStored();}
        public int getMaxEnergyStored(){var t=target();return t==null?0:t.getMaxEnergyStored();}
        public boolean canExtract(){var t=target();return t!=null&&t.canExtract();}
        public boolean canReceive(){var t=target();return t!=null&&t.canReceive();}
    };
    public IItemHandler itemPort(Direction side){return accessible(side)?items:null;}
    private void transferred(int amount){
        com.mcmagic.omnira.progression.BranchProgress.port(host,amount);var other=other();
        if(other!=null)com.mcmagic.omnira.progression.BranchProgress.port(other.host,amount);
    }
    public IFluidHandler fluidPort(Direction side){return accessible(side)?fluids:null;}
    public IEnergyStorage energyPort(Direction side){return accessible(side)?energy:null;}
    public static com.mcmagic.omnira.block.entity.ManaEngineAccess engineAt(Level l,BlockPos p,Direction towardsMachine){
        if(!l.hasChunkAt(p))return null;
        var port=at(l,p);BlockPos enginePos=p;Direction output=towardsMachine;
        if(port!=null){var peer=port.other();if(peer==null||port.face().getOpposite()!=towardsMachine)return null;enginePos=peer.attached();output=peer.face();}
        if(!l.hasChunkAt(enginePos))return null;
        var entity=l.getBlockEntity(enginePos);var state=l.getBlockState(enginePos);
        return entity instanceof com.mcmagic.omnira.block.entity.ManaEngineAccess engine
                &&state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                &&state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)==output
                &&engine.engineState().running()?engine:null;
    }
    /** Legacy drive query; native machines use PulseNetworks for reservations. One unit is 512 MPU. */
    public static double driveAt(Level l,BlockPos p,Direction towardsMachine){
        var engine=engineAt(l,p,towardsMachine);if(engine!=null)return engine.engineState().ratedCapacity()/512.0;
        if(!l.hasChunkAt(p))return 0;
        var port=at(l,p);BlockPos source=p;Direction output=towardsMachine;
        if(port!=null){var peer=port.other();if(peer==null||port.face().getOpposite()!=towardsMachine)return 0;source=peer.attached();output=peer.face();}
        if(!l.hasChunkAt(source))return 0;
        var state=l.getBlockState(source);
        if(!state.is(com.mcmagic.omnira.engine.SpacetimeEngineContent.BLOCK.get()))return 0;
        int side=com.mcmagic.omnira.engine.SpacetimeEngineBlock.side(state.getValue(com.mcmagic.omnira.engine.SpacetimeEngineBlock.PART));
        var be=com.mcmagic.omnira.engine.SpacetimeEngineBlock.controller(l,source);
        return side>=0&&output==com.mcmagic.omnira.engine.SpacetimeEngineBlock.OUTPUTS[side]&&be!=null&&Math.abs(be.generatedSpeed(side))>=16
                ?be.capacity(side)/512.:0;
    }
}
