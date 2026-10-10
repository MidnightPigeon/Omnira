package com.mcmagic.omnira.compat;

import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Create bottles do not expose NeoForge's item-fluid capability. Keep this adapter optional. */
public final class OrbPotionContainers {
    public static boolean accepts(ItemStack stack){return stack.is(Items.GLASS_BOTTLE)||PotionFluidHandler.isPotionItem(stack);}
    public static boolean filled(ItemStack stack){return PotionFluidHandler.isPotionItem(stack);}

    public static FluidActionResult transfer(ItemStack stack,IFluidHandler tank,boolean input,boolean execute){
        if(stack.getCount()!=1)return FluidActionResult.FAILURE;
        var action=execute?IFluidHandler.FluidAction.EXECUTE:IFluidHandler.FluidAction.SIMULATE;
        if(input){
            if(!filled(stack))return FluidActionResult.FAILURE;
            var fluid=PotionFluidHandler.getFluidFromPotionItem(stack);
            if(fluid.isEmpty()||tank.fill(fluid,IFluidHandler.FluidAction.SIMULATE)!=fluid.getAmount())return FluidActionResult.FAILURE;
            tank.fill(fluid,action);
            return new FluidActionResult(new ItemStack(Items.GLASS_BOTTLE));
        }
        if(!stack.is(Items.GLASS_BOTTLE))return FluidActionResult.FAILURE;
        var fluid=tank.drain(250,IFluidHandler.FluidAction.SIMULATE);
        if(fluid.getAmount()!=250)return FluidActionResult.FAILURE;
        ItemStack result;
        if(fluid.getFluid()==Fluids.WATER)result=PotionContents.createItemStack(Items.POTION,Potions.WATER);
        else if(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString().equals("create:potion"))result=PotionFluidHandler.fillBottle(stack,fluid);
        else return FluidActionResult.FAILURE;
        tank.drain(fluid,action);
        return new FluidActionResult(result);
    }
    private OrbPotionContainers(){}
}
