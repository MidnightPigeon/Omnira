package com.mcmagic.omnira.alchemy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** One shared capacity; order is persistent and the last entry is drained first. */
public final class PoolFluids implements IFluidHandler {
    public static final int CAPACITY=40000, KINDS=4;
    private final List<FluidStack> contents=new ArrayList<>();
    private final Runnable changed;
    private final java.util.function.IntSupplier capacity;
    public PoolFluids(Runnable changed){this(changed,()->CAPACITY);}
    public PoolFluids(Runnable changed,java.util.function.IntSupplier capacity){this.changed=changed;this.capacity=capacity;}
    public int total(){return contents.stream().mapToInt(FluidStack::getAmount).sum();}
    public int count(){return contents.size();}
    public int amount(FluidStack fluid){return contents.stream().filter(f->FluidStack.isSameFluidSameComponents(f,fluid)).mapToInt(FluidStack::getAmount).sum();}
    public List<FluidStack> snapshot(){return contents.stream().map(FluidStack::copy).toList();}
    public void moveLast(int row){if(row>=0&&row<contents.size()){contents.add(contents.remove(row));changed.run();}}
    public int getTanks(){return KINDS;}
    public FluidStack getFluidInTank(int tank){return tank>=0&&tank<contents.size()?contents.get(tank).copy():FluidStack.EMPTY;}
    public int getTankCapacity(int tank){
        if(tank<0||tank>=KINDS)return 0;
        // Virtual entries share one reservoir: report the free space exactly once.
        int stored=getFluidInTank(tank).getAmount();
        return stored+(tank==Math.min(contents.size(),KINDS-1)?Math.max(0,capacity.getAsInt()-total()):0);
    }
    public boolean isFluidValid(int tank,FluidStack fluid){return !fluid.isEmpty();}
    public int fill(FluidStack fluid,FluidAction action){
        if(fluid.isEmpty())return 0;
        int found=-1;
        for(int i=0;i<contents.size();i++)if(FluidStack.isSameFluidSameComponents(fluid,contents.get(i))){found=i;break;}
        if(found<0&&contents.size()>=KINDS)return 0;
        int added=Math.min(fluid.getAmount(),Math.max(0,capacity.getAsInt()-total()));
        if(added>0&&action.execute()){
            if(found<0)contents.add(fluid.copyWithAmount(added));else contents.get(found).grow(added);
            changed.run();
        }
        return added;
    }
    public FluidStack drain(FluidStack fluid,FluidAction action){
        if(fluid.isEmpty())return FluidStack.EMPTY;
        for(int i=0;i<contents.size();i++)if(FluidStack.isSameFluidSameComponents(fluid,contents.get(i))){
            var result=fluid.copyWithAmount(Math.min(fluid.getAmount(),contents.get(i).getAmount()));
            if(action.execute()){
                contents.get(i).shrink(result.getAmount());if(contents.get(i).isEmpty())contents.remove(i);changed.run();
            }
            return result;
        }
        return FluidStack.EMPTY;
    }
    public FluidStack drain(int amount,FluidAction action){return contents.isEmpty()?FluidStack.EMPTY:drain(contents.getLast().copyWithAmount(amount),action);}
    public ListTag save(HolderLookup.Provider registries){var tag=new ListTag();for(var f:contents)tag.add(f.save(registries));return tag;}
    public void load(ListTag tag,HolderLookup.Provider registries){contents.clear();for(var entry:tag){
        var f=FluidStack.parseOptional(registries,(CompoundTag)entry);fill(f,FluidAction.EXECUTE);
    }}
}
