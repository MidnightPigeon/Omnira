package com.mcmagic.omnira.compat;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Loaded only after the optional Create presence guard. */
public final class OrbCreateFilters {
    public static boolean isFilter(ItemStack stack){return FilterItemStack.of(stack).isFilterItem();}
    public static int domains(Level level,ItemStack sample){return domains(level,FilterItemStack.of(sample),0);}
    private static int domains(Level level,FilterItemStack sample,int depth){
        if(sample.isEmpty())return 0;
        if(depth>=16)return com.mcmagic.omnira.block.entity.OrbFilterMatcher.BOTH;
        if(sample instanceof FilterItemStack.ListFilterItemStack list){
            if(list.isBlacklist)return com.mcmagic.omnira.block.entity.OrbFilterMatcher.BOTH;
            int result=0;for(var entry:list.containedItems)result|=domains(level,entry,depth+1);return result;
        }
        if(sample.isFilterItem())return com.mcmagic.omnira.block.entity.OrbFilterMatcher.BOTH;
        return sample.fluid(level).isEmpty()?com.mcmagic.omnira.block.entity.OrbFilterMatcher.ITEMS:com.mcmagic.omnira.block.entity.OrbFilterMatcher.FLUIDS;
    }
    public static FluidStack bottledFluid(ItemStack stack) {
        if(com.simibubi.create.content.fluids.potion.PotionFluidHandler.isPotionItem(stack))
            return com.simibubi.create.content.fluids.potion.PotionFluidHandler.getFluidFromPotionItem(stack);
        if(stack.is(net.minecraft.world.item.Items.HONEY_BOTTLE))
            return new FluidStack((net.minecraft.world.level.material.Fluid)com.simibubi.create.AllFluids.HONEY.getSource(),250);
        return FluidStack.EMPTY;
    }
    public static boolean matches(Level level,ItemStack filter,ItemStack item,FluidStack fluid) {
        var test=FilterItemStack.of(filter);return fluid==null?test.test(level,item):test.test(level,fluid);
    }
}
