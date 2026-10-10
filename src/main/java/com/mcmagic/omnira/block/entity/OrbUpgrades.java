package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.item.OrbUpgradeItem;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Upgrade state travels with the plate; slots are deliberately not exposed to pipes. */
public final class OrbUpgrades extends ItemStackHandler {
    private final CrystalBallBlockEntity owner;
    public OrbUpgrades(CrystalBallBlockEntity owner) {super(3);this.owner=owner;}
    private OrbUpgrades target(){return owner.storage().upgrades;}
    @Override public ItemStack getStackInSlot(int slot){var target=target();return target==this?super.getStackInSlot(slot):target.getStackInSlot(slot);}
    @Override public void setStackInSlot(int slot,ItemStack stack){var target=target();if(target==this)super.setStackInSlot(slot,stack);else {target.setStackInSlot(slot,stack);owner.setChanged();}}
    @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){var target=target();return target==this?super.insertItem(slot,stack,simulate):target.insertItem(slot,stack,simulate);}
    @Override public CompoundTag serializeNBT(HolderLookup.Provider registries){var target=target();return target==this?super.serializeNBT(registries):target.serializeNBT(registries);}
    public CompoundTag serializeLocal(HolderLookup.Provider registries){return super.serializeNBT(registries);}
    @Override public int getSlotLimit(int slot) {return 1;}
    @Override public boolean isItemValid(int slot,ItemStack stack) {
        var candidate=kind(stack);if(candidate==null)return false;
        if(owner instanceof VoidCrystalBallBlockEntity)return slot==1&&candidate==OrbUpgradeItem.Kind.INTAKE;
        if(owner instanceof com.mcmagic.omnira.space.sea.StarClusterOrbEntity)return slot==1&&candidate==OrbUpgradeItem.Kind.STABILIZATION;
        for(int i=0;i<3;i++)if(i!=slot) {
            var other=kind(getStackInSlot(i));
            if(candidate.sameFamily(other))return false;
        }
        return true;
    }
    @Override protected void onContentsChanged(int slot) {owner.setChanged();}
    public static OrbUpgradeItem.Kind kind(ItemStack stack) {return stack.getItem() instanceof OrbUpgradeItem upgrade?upgrade.kind:null;}
    public boolean has(OrbUpgradeItem.Kind kind) {for(int i=0;i<3;i++)if(kind(getStackInSlot(i))==kind)return true;return false;}
    public int speedMultiplier() {return has(OrbUpgradeItem.Kind.SPEED)?2:1;}
    public int multiplier(int except) {
        int value=1;
        for(int i=0;i<3;i++)if(i!=except) {
            var kind=kind(getStackInSlot(i));
            if(kind!=null)value=Math.max(value,kind.capacityMultiplier);
        }
        return value;
    }
    public boolean mayRemove(int slot) {return owner.fitsMultiplier(multiplier(slot));}
    @Override public ItemStack extractItem(int slot,int count,boolean simulate) {
        var target=target();if(target!=this)return target.extractItem(slot,count,simulate);
        return mayRemove(slot)?super.extractItem(slot,count,simulate):ItemStack.EMPTY;
    }
    public static CompoundTag settings(ItemStack stack) {return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public void update(int slot,CompoundTag data) {
        getStackInSlot(slot).set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.setChanged();
    }
    public ItemStack filter(int slot,int filter) {
        return ItemStack.parseOptional(owner.getLevel().registryAccess(),settings(getStackInSlot(slot)).getCompound("Filter"+filter));
    }
    public void filter(int slot,int filter,ItemStack stack) {
        setFilter(getStackInSlot(slot),filter,stack,owner.getLevel().registryAccess());
        owner.setChanged();
    }
    public static void setFilter(ItemStack plate,int filter,ItemStack stack,HolderLookup.Provider registries) {
        var tag=settings(plate);
        if(stack.isEmpty())tag.remove("Filter"+filter);
        else {
            var sample=OrbFilterMatcher.preserveComponents(stack)?stack.copyWithCount(1):new ItemStack(stack.getItem());
            tag.put("Filter"+filter,sample.save(registries));
        }
        plate.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    public boolean matches(int slot,ItemStack item,net.neoforged.neoforge.fluids.FluidStack fluid) {
        var data=settings(getStackInSlot(slot));
        var level=owner.getLevel();
        if(level==null)return false;
        for(int i=0;i<3;i++) {
            var sample=ItemStack.parseOptional(level.registryAccess(),data.getCompound("Filter"+i));
            if(OrbFilterMatcher.matches(level,sample,item,fluid))return data.getBoolean("Whitelist");
        }
        return !data.getBoolean("Whitelist");
    }
}
