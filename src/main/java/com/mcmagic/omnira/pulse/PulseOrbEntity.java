package com.mcmagic.omnira.pulse;

import com.mcmagic.omnira.item.OrbUpgradeItem;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PulseOrbEntity extends BlockEntity implements MenuProvider,PulseOrbHost {
    public final BlockEntity host;
    public final PulseConversions conversion=new PulseConversions(this);
    public final ItemStack[] frequency={ItemStack.EMPTY,ItemStack.EMPTY};
    public int potential,occupied,shaftState;
    public java.util.UUID storageIdentity=java.util.UUID.randomUUID();
    public final PulseOrbUpgrade upgrade=new PulseOrbUpgrade(this);
    public PulseOrbEntity(BlockPos p,BlockState s){this(p,s,null);}
    public PulseOrbEntity(BlockPos p,BlockState s,BlockEntity host){super(PulseContent.ENTITY.get(),p,s);this.host=host==null?this:host;}
    @Override public PulseOrbEntity orb(){return this;}
    public static PulseOrbEntity from(BlockEntity entity){return entity instanceof PulseOrbHost host?host.orb():null;}
    public Direction inlet(){return host.getBlockState().getValue(PulseOrbBlock.MOUNT).getOpposite();}
    @Override public void setChanged(){if(host!=null&&host!=this)host.setChanged();else super.setChanged();}
    public void saveState(CompoundTag tag,HolderLookup.Provider registries){saveAdditional(tag,registries);}
    public void loadState(CompoundTag tag,HolderLookup.Provider registries){loadAdditional(tag,registries);}
    public boolean resonant(){return ((PulseOrbBlock)getBlockState().getBlock()).resonant;}
    public boolean accepts(ItemStack stack){return stack.getItem() instanceof OrbUpgradeItem item&&
            (resonant()?com.mcmagic.omnira.item.UpgradeCompatibility.Host.RESONANT_PULSE_ORB:com.mcmagic.omnira.item.UpgradeCompatibility.Host.PULSE_ORB).accepts(item.kind);}
    public boolean stabilized(){return upgrade.getStackInSlot(0).getItem() instanceof OrbUpgradeItem item&&item.kind==OrbUpgradeItem.Kind.STABILIZATION;}
    public int capacity(){return 8192*(upgrade.getStackInSlot(0).getItem() instanceof OrbUpgradeItem item?item.kind.capacityMultiplier:1);}
    public boolean hasFrequency(){return !frequency[0].isEmpty()||!frequency[1].isEmpty();}
    public void frequency(int slot,ItemStack sample){if(!resonant()||slot<0||slot>1)return;
        if(ItemStack.isSameItemSameComponents(frequency[slot],sample))return;
        if(level instanceof net.minecraft.server.level.ServerLevel server)PulseOrbGroups.get(server).detach(this);
        frequency[slot]=sample.copyWithCount(1);
        if(level instanceof net.minecraft.server.level.ServerLevel server)PulseOrbGroups.get(server).register(this);
        sync();PulseNetworks.orbChanged(this);}
    @Override public void onLoad(){super.onLoad();if(level instanceof net.minecraft.server.level.ServerLevel server)PulseOrbGroups.get(server).register(this);PulseNetworks.orbChanged(this);}
    @Override public void setRemoved(){PulseNetworks.removed(this);super.setRemoved();}
    public void tick(){conversion.tick();if(level.getGameTime()%5==0){int next=PulseOrbShaftState.capture(this);if(next!=shaftState){shaftState=next;sync();}}if(Math.floorMod(level.getGameTime()+worldPosition.hashCode(),20)==0){PulseOrbGroups.get((net.minecraft.server.level.ServerLevel)level).register(this);int used=(int)Math.ceil(Math.max(0,PulseNetworks.occupied(this)-1e-6));
        int next=Math.min(Math.max(0,capacity()-used),(int)Math.floor(PulseNetworks.available(this)+1e-6));if(next!=potential||used!=occupied){potential=next;occupied=used;sync();}}}
    void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new PulseOrbMenu(id,inventory,this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("ShaftState",shaftState);tag.putUUID("PulseIdentity",storageIdentity);tag.put("Upgrade",upgrade.serializeNBT(registries));tag.putInt("Potential",potential);tag.putInt("Occupied",occupied);for(int i=0;i<2;i++)tag.put("Frequency"+i,frequency[i].saveOptional(registries));}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);shaftState=tag.getInt("ShaftState");if(tag.hasUUID("PulseIdentity"))storageIdentity=tag.getUUID("PulseIdentity");upgrade.deserializeNBT(registries,tag.getCompound("Upgrade"));if(tag.contains("SharedUpgrade"))upgrade.local(ItemStack.parseOptional(registries,tag.getCompound("SharedUpgrade")));potential=Math.clamp(tag.getInt("Potential"),0,32768);occupied=Math.clamp(tag.getInt("Occupied"),0,32768);for(int i=0;i<2;i++)frequency[i]=ItemStack.parseOptional(registries,tag.getCompound("Frequency"+i));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=new CompoundTag();saveAdditional(tag,registries);tag.put("SharedUpgrade",upgrade.getStackInSlot(0).saveOptional(registries));return tag;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
