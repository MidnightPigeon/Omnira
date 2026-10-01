package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.menu.ManaEngineMenu;
import com.mcmagic.omnira.registry.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Storage shared with the decorative fallback; no Create classes are referenced. */
public final class ManaEngineState {
    public static final int RPM=16;
    private final com.mcmagic.omnira.energy.EngineEnergyBuffer energy=new com.mcmagic.omnira.energy.EngineEnergyBuffer();
    private final net.neoforged.neoforge.energy.IEnergyStorage[] ports=new net.neoforged.neoforge.energy.IEnergyStorage[6];
    private Direction lastOutput;
    private boolean allowed() {
        return !timeStopped() && enabled && (!redstoneControl || (host.getLevel()!=null && host.getLevel().hasNeighborSignal(host.getBlockPos())))
                && (ratedCapacity()>0 || ratedEnergy()>0) && !host.isRemoved();
    }
    public boolean running() {return allowed();}
    private boolean timeStopped() {return host.getLevel()!=null && com.mcmagic.omnira.time.StilledTime.stopped(host.getLevel(),host.getBlockPos());}
    private Direction output() {return host.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);}
    private void prepareEnergy() {
        if(host.getLevel()!=null && !host.getLevel().isClientSide)
            energy.beginTick(host.getLevel().getGameTime(),ratedEnergy(),allowed() && com.mcmagic.omnira.energy.EnergyIntegration.available());
    }
    public net.neoforged.neoforge.energy.IEnergyStorage energyPort(Direction side) {
        if(side==null || side!=output() || !com.mcmagic.omnira.energy.EnergyIntegration.available()) return null;
        if(ports[side.ordinal()]==null) ports[side.ordinal()]=new net.neoforged.neoforge.energy.IEnergyStorage() {
            private boolean valid() {
                return host.getLevel()!=null && !host.getLevel().isClientSide && !host.isRemoved() && side==output()
                        && com.mcmagic.omnira.energy.EnergyIntegration.available();
            }
            public int receiveEnergy(int amount,boolean simulate) {return 0;}
            public int extractEnergy(int amount,boolean simulate) {
                if(!valid() || !allowed()) return 0;
                prepareEnergy();
                int result=energy.extract(amount,simulate);
                if(!simulate && result>0) host.setChanged();
                return result;
            }
            public int getEnergyStored() {return valid()?Math.min(energy.stored(),ratedEnergy()):0;}
            public int getMaxEnergyStored() {return ratedEnergy();}
            public boolean canExtract() {return valid();}
            public boolean canReceive() {return false;}
        };
        return ports[side.ordinal()];
    }
    public void tickEnergy() {
        if(host.getLevel()==null || host.getLevel().isClientSide) return;
        Direction face=output();
        if(face!=lastOutput) {lastOutput=face;host.getLevel().invalidateCapabilities(host.getBlockPos());}
        if(!com.mcmagic.omnira.energy.EnergyIntegration.available()) return;
        prepareEnergy();
        var port=energyPort(face);
        int offer=port.extractEnergy(Integer.MAX_VALUE,true);
        var neighbor=host.getBlockPos().relative(face);
        if(offer>0 && host.getLevel().hasChunkAt(neighbor)) {
            var receiver=host.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,neighbor,face.getOpposite());
            if(receiver!=null && receiver.canReceive()) {
                int accepted=Math.clamp(receiver.receiveEnergy(offer,false),0,offer);
                port.extractEnergy(accepted,false);
            }
        }
        if(allowed()) host.setChanged();
    }
    public final BlockEntity host;
    public final boolean kinetic;
    public final SimpleContainer inventory=new SimpleContainer(4);
    public boolean enabled=true,redstoneControl=true,clockwise=true;
    private boolean dirty=true,powered,loading;
    public float rotorAngle,previousRotorAngle;
    public ManaEngineState(BlockEntity host,boolean kinetic) {
        this.host=host;this.kinetic=kinetic;
        inventory.addListener(container->{if(!loading) {dirty=true;host.setChanged();}});
    }
    public static boolean accepts(int slot,ItemStack stack) {
        return !stack.isEmpty() && (slot==0?stack.getItem() instanceof com.mcmagic.omnira.energy.EngineCore:slot>=1 && slot<=3 && multiplier(stack)>0);
    }
    public static double multiplier(ItemStack stack) {
        if(stack.is(ModItems.BASIC_CRYSTAL_GRID.get())) return 1.2;
        if(stack.is(ModItems.ELEMENTAL_CRYSTAL_GRID.get())) return 1.5;
        if(stack.is(ModItems.ARCANE_CRYSTAL_GRID.get())) return 2;
        if(stack.is(ModItems.OMNI_CRYSTAL_GRID.get())) return 4;
        return 0;
    }
    public int ratedCapacity() {
        return ratedOutput(false);
    }
    public int ratedEnergy() {return ratedOutput(true);}
    private int ratedOutput(boolean electrical) {
        var stack=inventory.getItem(0);
        if(!accepts(0,stack))return 0;
        var output=((com.mcmagic.omnira.energy.EngineCore)stack.getItem()).engineOutput(stack);
        double result=electrical?output.fePerSecond():output.stress();
        for(int i=1;i<4;i++)result*=Math.max(1,multiplier(inventory.getItem(i)));
        return (int)Math.min(Integer.MAX_VALUE,Math.round(result));
    }
    public float generatedSpeed() {
        return !timeStopped() && kinetic && enabled && (!redstoneControl || powered) && ratedCapacity()>0?(clockwise?RPM:-RPM):0;
    }
    public boolean pollChanges() {
        boolean signal=host.getLevel().hasNeighborSignal(host.getBlockPos());
        boolean changed=dirty || signal!=powered;
        powered=signal;dirty=false;return changed;
    }
    public void animate() {
        previousRotorAngle=rotorAngle;rotorAngle+=(running()?(clockwise?RPM:-RPM):0)*.3F;
        if(Math.abs(rotorAngle)>36000) {rotorAngle%=360;previousRotorAngle=rotorAngle;}
    }
    public void control(int id) {
        if(id==0) enabled=!enabled;
        else if(id==1) redstoneControl=!redstoneControl;
        else if(id==2) clockwise=!clockwise;
        else return;
        dirty=true;host.setChanged();
    }
    public void write(CompoundTag tag,HolderLookup.Provider registries) {
        var items=NonNullList.withSize(4,ItemStack.EMPTY);
        for(int i=0;i<4;i++) items.set(i,inventory.getItem(i));
        ContainerHelper.saveAllItems(tag,items,registries);
        tag.putInt("Energy",energy.stored());tag.putInt("EnergyFraction",energy.remainder());
        tag.putBoolean("Enabled",enabled);tag.putBoolean("RedstoneControl",redstoneControl);
        tag.putBoolean("Clockwise",clockwise);tag.putBoolean("Powered",powered);
    }
    public void read(CompoundTag tag,HolderLookup.Provider registries) {
        var items=NonNullList.withSize(4,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag,items,registries);
        loading=true;for(int i=0;i<4;i++) inventory.setItem(i,items.get(i));loading=false;
        enabled=!tag.contains("Enabled") || tag.getBoolean("Enabled");
        redstoneControl=!tag.contains("RedstoneControl") || tag.getBoolean("RedstoneControl");
        clockwise=!tag.contains("Clockwise") || tag.getBoolean("Clockwise");
        powered=tag.getBoolean("Powered");dirty=true;
        energy.restore(tag.getInt("Energy"),tag.getInt("EnergyFraction"));
    }
}
