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
    public void fillEnergy(){energy.fill(ratedEnergy());host.setChanged();}
    private final net.neoforged.neoforge.energy.IEnergyStorage[] ports=new net.neoforged.neoforge.energy.IEnergyStorage[6];
    private Direction lastOutput;
    private boolean allowed() {
        return !timeStopped() && enabled
                && (ratedCapacity()>0 || ratedEnergy()>0) && !host.isRemoved();
    }
    public boolean running() {return allowed();}
    private boolean timeStopped() {return host.getLevel()!=null && com.mcmagic.omnira.time.StilledTime.stopped(host.getLevel(),host.getBlockPos());}
    private Direction output() {return host.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);}
    private void prepareEnergy() {
        if(host.getLevel()!=null && !host.getLevel().isClientSide)
            energy.beginTick(host.getLevel().getGameTime(),ratedEnergy(),electricalOutput());
    }
    private boolean electricalOutput(){return allowed()&&com.mcmagic.omnira.energy.EnergyIntegration.available()
            &&!com.mcmagic.omnira.pulse.PulseOutput.nativeConnection(host.getLevel(),host.getBlockPos(),output());}
    public net.neoforged.neoforge.energy.IEnergyStorage energyPort(Direction side) {
        if(side==null || side!=output() || !com.mcmagic.omnira.energy.EnergyIntegration.available()) return null;
        if(ports[side.ordinal()]==null) ports[side.ordinal()]=new net.neoforged.neoforge.energy.IEnergyStorage() {
            private boolean valid() {
                return host.getLevel()!=null && !host.getLevel().isClientSide && !host.isRemoved() && side==output()
                        && com.mcmagic.omnira.energy.EnergyIntegration.available();
            }
            public int receiveEnergy(int amount,boolean simulate) {return 0;}
            public int extractEnergy(int amount,boolean simulate) {
                if(!valid() || !electricalOutput()) return 0;
                prepareEnergy();
                int result=energy.extract(amount,simulate);
                if(!simulate && result>0) host.setChanged();
                return result;
            }
            public int getEnergyStored() {return valid()&&electricalOutput()?Math.min(energy.stored(),ratedEnergy()):0;}
            public int getMaxEnergyStored() {return ratedEnergy();}
            public boolean canExtract() {return valid()&&electricalOutput();}
            public boolean canReceive() {return false;}
        };
        return ports[side.ordinal()];
    }
    public void tickEnergy() {
        if(host.getLevel()==null || host.getLevel().isClientSide) return;
        int mode=com.mcmagic.omnira.pulse.PulseOutput.mode(host.getLevel(),host.getBlockPos(),output());
        if(mode!=outputMode){outputMode=mode;host.setChanged();host.getLevel().sendBlockUpdated(host.getBlockPos(),host.getBlockState(),host.getBlockState(),3);}
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
    public boolean enabled=true;
    private boolean dirty=true,loading;
    public float rotorAngle,previousRotorAngle;
    public int outputMode;
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
    public int ratedEnergy() {return ratedCapacity();}
    private int ratedOutput(boolean electrical) {
        var stack=inventory.getItem(0);
        if(!accepts(0,stack))return 0;
        var output=((com.mcmagic.omnira.energy.EngineCore)stack.getItem()).engineOutput(stack);
        double result=electrical?output.fePerSecond():output.stress();
        for(int i=1;i<4;i++)result*=Math.max(1,multiplier(inventory.getItem(i)));
        return (int)Math.min(Integer.MAX_VALUE,Math.round(result));
    }
    public float generatedSpeed() {
        return !timeStopped() && kinetic && enabled && ratedCapacity()>0?RPM:0;
    }
    public boolean pollChanges() {
        boolean changed=dirty;dirty=false;return changed;
    }
    public void animate() {
        previousRotorAngle=rotorAngle;rotorAngle+=(running()?RPM:0)*.3F;
        if(Math.abs(rotorAngle)>36000) {rotorAngle%=360;previousRotorAngle=rotorAngle;}
    }
    public void control(int id) {
        if(id==0) enabled=!enabled;
        else return;
        dirty=true;host.setChanged();
        com.mcmagic.omnira.pulse.PulseNetworks.changed(host.getLevel(),host.getBlockPos());
        if(host.getLevel()!=null)host.getLevel().sendBlockUpdated(host.getBlockPos(),host.getBlockState(),host.getBlockState(),3);
    }
    public boolean controlHit(net.minecraft.world.phys.BlockHitResult hit){
        var face=output();var pos=host.getBlockPos();double y=hit.getLocation().y-pos.getY();
        double x=face.getAxis()==Direction.Axis.Z?hit.getLocation().x-pos.getX():hit.getLocation().z-pos.getZ();
        return hit.getDirection()==face.getOpposite()&&y>=.375&&y<=.625&&x>=.375&&x<=.625;
    }
    public void write(CompoundTag tag,HolderLookup.Provider registries) {
        tag.putInt("PulseOutputMode",outputMode);
        var items=NonNullList.withSize(4,ItemStack.EMPTY);
        for(int i=0;i<4;i++) items.set(i,inventory.getItem(i));
        ContainerHelper.saveAllItems(tag,items,registries);
        tag.putInt("Energy",energy.stored());tag.putInt("EnergyFraction",energy.remainder());
        tag.putBoolean("Enabled",enabled);
    }
    public void read(CompoundTag tag,HolderLookup.Provider registries) {
        outputMode=Math.clamp(tag.getInt("PulseOutputMode"),0,2);
        var items=NonNullList.withSize(4,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag,items,registries);
        loading=true;for(int i=0;i<4;i++) inventory.setItem(i,items.get(i));loading=false;
        enabled=!tag.contains("Enabled") || tag.getBoolean("Enabled");
        dirty=true;
        energy.restore(tag.getInt("Energy"),tag.getInt("EnergyFraction"));
    }
}
