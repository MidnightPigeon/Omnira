package com.mcmagic.omnira.vehicle;

import com.mcmagic.omnira.block.entity.OrbUpgrades;
import com.mcmagic.omnira.item.OrbUpgradeItem.Kind;
import com.mcmagic.omnira.item.bottle.PocketBottleItem;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The bank is shared by the entity, its menu, and the vortex. */
public final class CruiseOrbStorage {
    public static final int SIZE=12, BASE_CAPACITY=24000;
    private final CruiseOrbEntity owner;
    private boolean loading,portInternal,portDone,portInput;
    private int automationTicks,portCredit,fluidBudget;
    private int trashTicks;
    private ItemStack portSnapshot=ItemStack.EMPTY;
    private ItemStack installedSeat=ItemStack.EMPTY;

    public final ItemStackHandler items=new ItemStackHandler(SIZE) {
        @Override public int getSlotLimit(int slot){return 64*multiplier();}
        @Override protected int getStackLimit(int slot,ItemStack stack){return itemLimit(stack,multiplier());}
        @Override public boolean isItemValid(int slot,ItemStack stack){return !PocketBottleItem.restricted(stack) && !(stack.getItem() instanceof CruiseOrbItem);}
        @Override public void setStackInSlot(int slot,ItemStack stack) {
            if(!loading && !stack.isEmpty() && !isItemValid(slot,stack))return;
            super.setStackInSlot(slot,stack);
        }
        @Override protected void onContentsChanged(int slot){changed();}
    };
    public final ItemStackHandler upgrades=new ItemStackHandler(3) {
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack) {
            var kind=OrbUpgrades.kind(stack);if(kind==null || kind==Kind.INTAKE || kind==Kind.OUTPUT || stack.getCount()!=1)return false;
            for(int i=0;i<3;i++)if(i!=slot && kind.sameFamily(OrbUpgrades.kind(getStackInSlot(i))))return false;
            return fitsMultiplier(multiplierExcept(slot,stack));
        }
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate) {
            return fitsMultiplier(multiplierExcept(slot,ItemStack.EMPTY))?super.extractItem(slot,amount,simulate):ItemStack.EMPTY;
        }
        @Override public void setStackInSlot(int slot,ItemStack stack) {
            if(!loading && ((!stack.isEmpty() && !isItemValid(slot,stack)) || !fitsMultiplier(multiplierExcept(slot,stack))))return;
            super.setStackInSlot(slot,stack);
        }
        @Override protected void onContentsChanged(int slot){changed();}
    };
    public final ItemStackHandler port=new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return stack.getCapability(Capabilities.FluidHandler.ITEM)!=null;}
        @Override public void setStackInSlot(int slot,ItemStack stack) {
            if(!loading && !portInternal && !stack.isEmpty() && (stack.getCount()!=1 || !isItemValid(slot,stack)))return;
            super.setStackInSlot(slot,stack);
        }
        @Override protected void onContentsChanged(int slot){if(!portInternal)resetPort();changed();}
    };
    public final ItemStackHandler trash=new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot,ItemStack stack){return items.isItemValid(0,stack);}
        @Override protected void onContentsChanged(int slot){trashTicks=0;changed();}
    };
    private static boolean filledBucket(ItemStack stack){
        return !stack.isEmpty() && !stack.is(net.minecraft.world.item.Items.BUCKET)
                && (stack.getItem() instanceof net.minecraft.world.item.BucketItem
                    || stack.getItem() instanceof net.minecraft.world.item.MilkBucketItem);
    }
    public final FluidTank tank=new FluidTank(BASE_CAPACITY) {
        @Override public int getCapacity(){return BASE_CAPACITY*multiplier();}
        @Override public int getTankCapacity(int index){return getCapacity();}
        @Override public int getSpace(){return Math.max(0,getCapacity()-getFluidAmount());}
        @Override public boolean isFluidValid(FluidStack stack){return isEmpty() || FluidStack.isSameFluidSameComponents(stack,getFluid());}
        @Override public int fill(FluidStack stack,FluidAction action){capacity=getCapacity();return getFluidAmount()>=capacity?0:super.fill(stack,action);}
        @Override public FluidStack drain(int amount,FluidAction action) {
            if(amount<=0)return FluidStack.EMPTY;
            return infinite()?getFluid().copyWithAmount(Math.min(amount,getCapacity())):super.drain(amount,action);
        }
        @Override protected void onContentsChanged(){changed();}
    };

    public CruiseOrbStorage(CruiseOrbEntity owner){this.owner=owner;}
    public int multiplier(){return multiplierExcept(-1,ItemStack.EMPTY);}
    private int multiplierExcept(int except,ItemStack replacement) {
        int result=1;
        for(int i=0;i<3;i++) {
            var kind=OrbUpgrades.kind(i==except?replacement:upgrades.getStackInSlot(i));
            if(kind!=null)result=Math.max(result,kind.capacityMultiplier);
        }
        return result;
    }
    public boolean stabilized(){for(int i=0;i<3;i++)if(OrbUpgrades.kind(upgrades.getStackInSlot(i))==Kind.STABILIZATION)return true;return false;}
    public boolean hasGoldenToilet(){return installedSeat.is(ModItems.GOLDEN_TOILET.get());}
    public ItemStack installedSeat(){return installedSeat.copy();}
    public boolean installGoldenToilet(ItemStack stack){
        if(hasGoldenToilet() || !stack.is(ModItems.GOLDEN_TOILET.get()))return false;
        installedSeat=stack.copyWithCount(1);changed();return true;
    }
    public int speedMultiplier(){for(int i=0;i<3;i++)if(OrbUpgrades.kind(upgrades.getStackInSlot(i))==Kind.SPEED)return 2;return 1;}
    public int itemLimit(ItemStack stack,int multiplier){return stack.getMaxStackSize()>1?stack.getMaxStackSize()*multiplier:1;}
    public boolean fitsMultiplier(int multiplier) {
        if(multiplier<1 || tank.getFluidAmount()>BASE_CAPACITY*multiplier)return false;
        for(int i=0;i<SIZE;i++)if(items.getStackInSlot(i).getCount()>itemLimit(items.getStackInSlot(i),multiplier))return false;
        return true;
    }
    public boolean infinite() {
        if(tank.isEmpty())return false;
        var fluid=tank.getFluid().getFluid();
        return fluid.canConvertToSource(fluid.defaultFluidState(),owner.level(),owner.blockPosition());
    }
    public void changed(){if(!loading && !owner.level().isClientSide)owner.syncStorage();}
    public ItemStack insert(ItemStack stack,boolean simulate){return insertBounded(items,stack,simulate);}
    /** Execute against a private copy so every drop consumes capacity for subsequent drops. */
    public boolean fitsItems(java.util.List<ItemStack> drops) {
        int capacityMultiplier=multiplier();
        var shadow=new ItemStackHandler(SIZE) {
            @Override protected int getStackLimit(int slot,ItemStack stack){return itemLimit(stack,capacityMultiplier);}
            @Override public boolean isItemValid(int slot,ItemStack stack){return items.isItemValid(slot,stack);}
        };
        for(int i=0;i<SIZE;i++)shadow.setStackInSlot(i,items.getStackInSlot(i).copy());
        for(var drop:drops)if(!insertBounded(shadow,drop.copy(),false).isEmpty())return false;
        return true;
    }
    private static ItemStack insertBounded(IItemHandler handler,ItemStack stack,boolean simulate) {
        int size=Math.min(handler.getSlots(),4096);
        for(int pass=0;pass<2 && !stack.isEmpty();pass++)for(int i=0;i<size && !stack.isEmpty();i++) {
            var existing=handler.getStackInSlot(i);
            if(pass==0?!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing,stack):existing.isEmpty())
                stack=handler.insertItem(i,stack,simulate);
        }
        return stack;
    }

    public void tick() {
        if(owner.level().isClientSide)return;
        if(owner.level().getGameTime()%10==0)
            com.mcmagic.omnira.spacetime.AggregateStorage.handler(items,owner.level().getGameTime(),stabilized());
        var discarded=trash.getStackInSlot(0);
        if(filledBucket(discarded) && ++trashTicks>=200)
            trash.setStackInSlot(0,new ItemStack(net.minecraft.world.item.Items.BUCKET,discarded.getCount()));
        int speed=speedMultiplier();
        fluidBudget=Math.min(500*speed,fluidBudget+25*speed);
        tickPort();
        if(++automationTicks<20)return;
        automationTicks=0;
        for(int i=0;i<3;i++) {
            var kind=OrbUpgrades.kind(upgrades.getStackInSlot(i));
            if(kind==Kind.LAVA)lava(i);
        }
    }
    private void resetPort() {
        portSnapshot=port.getStackInSlot(0).copy();portCredit=0;portDone=false;portInput=false;
        var handler=portSnapshot.getCapability(Capabilities.FluidHandler.ITEM);
        if(handler!=null)for(int i=0;i<Math.min(handler.getTanks(),256);i++)if(!handler.getFluidInTank(i).isEmpty())portInput=true;
    }
    private void tickPort() {
        var stack=port.getStackInSlot(0);
        if(!ItemStack.matches(stack,portSnapshot))resetPort();
        if(stack.isEmpty() || portDone || !port.isItemValid(0,stack))return;
        // Atomic bucket transfers reserve the same shared budget as automation.
        int reserved=Math.min(25*speedMultiplier(),Math.min(fluidBudget,1000-portCredit));
        fluidBudget-=reserved;portCredit+=reserved;
        if(portCredit<1000)return;
        var result=portInput?FluidUtil.tryEmptyContainer(stack,tank,1000,null,true):FluidUtil.tryFillContainer(stack,tank,1000,null,true);
        if(!result.isSuccess())return;
        portInternal=true;
        try {port.setStackInSlot(0,result.getResult());}finally{portInternal=false;}
        portSnapshot=port.getStackInSlot(0).copy();portCredit=0;
        var next=portInput?FluidUtil.tryEmptyContainer(portSnapshot,tank,1000,null,false):FluidUtil.tryFillContainer(portSnapshot,tank,1000,null,false);
        portDone=!next.isSuccess();changed();
    }
    private void settings(int slot,CompoundTag tag) {
        upgrades.getStackInSlot(slot).set(DataComponents.CUSTOM_DATA,CustomData.of(tag));changed();
    }
    private void lava(int slot) {
        var lava=new FluidStack(Fluids.LAVA,100);
        if(tank.fill(lava,FluidAction.SIMULATE)>0){tank.fill(lava,FluidAction.EXECUTE);return;}
        var tag=OrbUpgrades.settings(upgrades.getStackInSlot(slot));
        for(int i=0;i<SIZE;i++) {
            var item=items.getStackInSlot(i);if(item.isEmpty())continue;
            int destination=i;
            if(item.getCount()>1) {
                destination=-1;for(int j=0;j<SIZE;j++)if(items.getStackInSlot(j).isEmpty()){destination=j;break;}
                if(destination<0)continue;
            }
            var source=new FluidTank(1000);source.setFluid(new FluidStack(Fluids.LAVA,1000));
            if(!FluidUtil.tryFillContainer(item,source,1000,null,false).isSuccess())continue;
            int credit=Math.clamp(tag.getInt("LavaCredit"),0,900)+100;
            source.setFluid(new FluidStack(Fluids.LAVA,credit));
            var result=FluidUtil.tryFillContainer(item,source,credit,null,true);
            if(result.isSuccess()) {
                if(destination!=i)items.extractItem(i,1,false);
                items.setStackInSlot(destination,result.getResult());credit=source.getFluidAmount();
            }
            tag.putInt("LavaCredit",credit);settings(slot,tag);return;
        }
    }
    public CompoundTag save() {
        var registries=owner.level().registryAccess();var tag=new CompoundTag();var list=new ListTag();int[] counts=new int[SIZE];
        for(int i=0;i<SIZE;i++) {
            var stack=items.getStackInSlot(i);counts[i]=stack.getCount();
            if(stack.isEmpty())continue;
            var entry=new CompoundTag();entry.putInt("Slot",i);entry.put("Stack",stack.copyWithCount(1).save(registries));list.add(entry);
        }
        tag.put("Items",list);tag.putIntArray("OrbCounts",counts);
        tag.put("Upgrades",upgrades.serializeNBT(registries));tag.put("Port",port.serializeNBT(registries));
        tag.put("Trash",trash.serializeNBT(registries));tag.putInt("TrashTicks",trashTicks);
        if(!installedSeat.isEmpty())tag.put("InstalledSeat",installedSeat.save(registries));
        tag.put("Fluid",tank.writeToNBT(registries,new CompoundTag()));
        tag.putBoolean("PortDone",portDone);tag.putBoolean("PortInput",portInput);return tag;
    }
    public void load(CompoundTag tag) {
        loading=true;
        try {
            var registries=owner.level().registryAccess();
            var upgradeTag=tag.getCompound("Upgrades").copy();upgradeTag.putInt("Size",3);
            upgrades.deserializeNBT(registries,upgradeTag);
            for(int i=0;i<SIZE;i++)items.setStackInSlot(i,ItemStack.EMPTY);
            var counts=tag.getIntArray("OrbCounts");var list=tag.getList("Items",Tag.TAG_COMPOUND);
            for(int i=0;i<Math.min(list.size(),SIZE);i++) {
                var entry=list.getCompound(i);int slot=entry.getInt("Slot");if(slot<0 || slot>=SIZE)continue;
                var stack=ItemStack.parseOptional(registries,entry.getCompound("Stack"));
                if(!stack.isEmpty() && slot<counts.length)stack.setCount(Math.max(1,counts[slot]));
                items.setStackInSlot(slot,stack);
            }
            var portTag=tag.getCompound("Port").copy();portTag.putInt("Size",1);
            port.deserializeNBT(registries,portTag);tank.readFromNBT(registries,tag.getCompound("Fluid"));
            var trashTag=tag.getCompound("Trash").copy();trashTag.putInt("Size",1);
            trash.deserializeNBT(registries,trashTag);trashTicks=Math.clamp(tag.getInt("TrashTicks"),0,199);
            var seat=ItemStack.parseOptional(registries,tag.getCompound("InstalledSeat"));
            installedSeat=seat.is(ModItems.GOLDEN_TOILET.get())?seat.copyWithCount(1):ItemStack.EMPTY;
            resetPort();portDone=tag.getBoolean("PortDone");portInput=tag.getBoolean("PortInput");automationTicks=0;fluidBudget=0;
        }finally{loading=false;}
    }
}
