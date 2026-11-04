package com.mcmagic.omnira.reversal;

import com.mcmagic.omnira.mire.MireContent;
import com.mcmagic.omnira.space.sea.VoidSeaContent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Converts incoming fluid directly to energy credit. There is no stored fluid to extract. */
record EnergyFluidInput(ReversalBlockEntity machine) implements IFluidHandler {
    static final int UNIT=FluidType.BUCKET_VOLUME;
    @Override public int getTanks(){return 1;}
    @Override public int getTankCapacity(int tank){return tank==0?machine.remainingFluidCapacity():0;}
    @Override public FluidStack getFluidInTank(int tank){return FluidStack.EMPTY;}
    @Override public boolean isFluidValid(int tank,FluidStack fluid){
        return tank==0 && fluid.getFluid().isSame(machine.infusion()?VoidSeaContent.SOURCE.get():MireContent.SOURCE.get());
    }
    @Override public int fill(FluidStack fluid,FluidAction action){
        if(fluid.isEmpty() || !isFluidValid(0,fluid))return 0;
        int accepted=Math.min(machine.remainingFluidCapacity(),fluid.getAmount());
        if(accepted>0 && action.execute())machine.creditFluid(accepted);
        return accepted;
    }
    @Override public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
    @Override public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.EMPTY;}
}
