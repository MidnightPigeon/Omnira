package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.compat.OrbCreateFilters;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** Read-only sample interpretation, independent of upgrade slot state and transfer direction. */
public final class OrbFilterMatcher {
    private OrbFilterMatcher() {}
    static boolean preserveComponents(ItemStack sample) {
        return sample.has(DataComponents.POTION_CONTENTS)
                || sample.getCapability(Capabilities.FluidHandler.ITEM)!=null || createFilter(sample);
    }
    public static boolean matches(Level level,ItemStack sample,ItemStack item,FluidStack fluid) {
        if(sample.isEmpty())return false;
        if(createFilter(sample))return OrbCreateFilters.matches(level,sample,item,fluid);
        if(fluid==null)return item.is(sample.getItem());
        if(fluid.isEmpty())return false;
        var handler=sample.getCapability(Capabilities.FluidHandler.ITEM);
        if(handler!=null)for(int tank=0;tank<handler.getTanks();tank++)
            if(FluidStack.isSameFluidSameComponents(fluid,handler.getFluidInTank(tank)))return true;
        return FluidStack.isSameFluidSameComponents(fluid,bottledFluid(sample));
    }
    private static boolean createFilter(ItemStack sample) {
        return ModList.get().isLoaded("create") && OrbCreateFilters.isFilter(sample);
    }
    private static FluidStack bottledFluid(ItemStack sample) {
        if(sample.is(Items.POTION) && new PotionContents(Potions.WATER).equals(sample.get(DataComponents.POTION_CONTENTS)))
            return new FluidStack(Fluids.WATER,250);
        return ModList.get().isLoaded("create")?OrbCreateFilters.bottledFluid(sample):FluidStack.EMPTY;
    }
}
