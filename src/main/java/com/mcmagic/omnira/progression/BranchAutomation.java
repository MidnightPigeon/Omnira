package com.mcmagic.omnira.progression;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** External capability views only; internal recipe mutations and menu slots remain unwrapped. */
public final class BranchAutomation {
    public static IItemHandler items(BlockEntity owner,IItemHandler delegate){
        if(delegate==null)return null;
        return new IItemHandler(){
            public int getSlots(){return delegate.getSlots();}
            public ItemStack getStackInSlot(int slot){return delegate.getStackInSlot(slot);}
            public int getSlotLimit(int slot){return delegate.getSlotLimit(slot);}
            public boolean isItemValid(int slot,ItemStack item){return delegate.isItemValid(slot,item);}
            public ItemStack insertItem(int slot,ItemStack item,boolean simulate){var rest=delegate.insertItem(slot,item,simulate);if(!simulate)BranchProgress.automated(owner,item.getCount()-rest.getCount());return rest;}
            public ItemStack extractItem(int slot,int amount,boolean simulate){var result=delegate.extractItem(slot,amount,simulate);if(!simulate)BranchProgress.automated(owner,result.getCount());return result;}
        };
    }
    public static IFluidHandler fluids(BlockEntity owner,IFluidHandler delegate){
        if(delegate==null)return null;
        return new IFluidHandler(){
            public int getTanks(){return delegate.getTanks();}
            public FluidStack getFluidInTank(int tank){return delegate.getFluidInTank(tank);}
            public int getTankCapacity(int tank){return delegate.getTankCapacity(tank);}
            public boolean isFluidValid(int tank,FluidStack fluid){return delegate.isFluidValid(tank,fluid);}
            public int fill(FluidStack fluid,FluidAction action){int result=delegate.fill(fluid,action);if(action.execute())BranchProgress.automated(owner,result);return result;}
            public FluidStack drain(FluidStack fluid,FluidAction action){var result=delegate.drain(fluid,action);if(action.execute())BranchProgress.automated(owner,result.getAmount());return result;}
            public FluidStack drain(int amount,FluidAction action){var result=delegate.drain(amount,action);if(action.execute())BranchProgress.automated(owner,result.getAmount());return result;}
        };
    }
    private BranchAutomation(){}
}
