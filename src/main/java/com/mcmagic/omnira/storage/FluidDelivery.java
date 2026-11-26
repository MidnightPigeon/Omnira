package com.mcmagic.omnira.storage;

import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;

/** A simulated fill is a quote, not a reservation. Retain any volume neither endpoint accepts. */
public final class FluidDelivery {
    private static final String KEY="PendingFluidDelivery";

    public static int move(IFluidHandler source,IFluidHandler target,FluidStack request,Consumer<FluidStack> retain) {
        var available=source.drain(request,SIMULATE);
        if(available.isEmpty())return 0;
        int quoted=target.fill(available,SIMULATE);
        if(quoted<=0)return 0;
        var taken=source.drain(available.copyWithAmount(quoted),EXECUTE);
        if(taken.isEmpty())return 0;
        int delivered=target.fill(taken,EXECUTE);
        int remainder=taken.getAmount()-delivered;
        if(remainder>0){
            remainder-=source.fill(taken.copyWithAmount(remainder),EXECUTE);
            if(remainder>0)retain.accept(taken.copyWithAmount(remainder));
        }
        return delivered;
    }

    public static int retry(IFluidHandler target,FluidStack pending,int limit,Consumer<FluidStack> retain) {
        var offer=pending.copyWithAmount(Math.min(limit,pending.getAmount()));
        int delivered=target.fill(offer,EXECUTE);
        if(delivered>0)retain.accept(pending.copyWithAmount(pending.getAmount()-delivered));
        return delivered;
    }

    public static FluidStack read(CompoundTag data,HolderLookup.Provider registries) {
        return hasPending(data) ? FluidStack.parseOptional(registries,data.getCompound(KEY)) : FluidStack.EMPTY;
    }

    public static boolean hasPending(CompoundTag data) { return data.contains(KEY,CompoundTag.TAG_COMPOUND); }

    public static void write(CompoundTag data,HolderLookup.Provider registries,FluidStack pending) {
        if(pending.isEmpty())data.remove(KEY);
        else data.put(KEY,pending.save(registries));
    }

    private FluidDelivery() {}
}
