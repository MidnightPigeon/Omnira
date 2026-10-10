package com.mcmagic.omnira.item;

import com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity;
import com.mcmagic.omnira.block.entity.OrbUpgrades;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Portable orbs retain real volume only; renewable extraction belongs to placed block interfaces. */
public final class PortableLiquidOrbFluidHandler extends FluidTank implements IFluidHandlerItem {
    private final ItemStack container;
    private final RegistryAccess registries;
    public PortableLiquidOrbFluidHandler(ItemStack stack){
        super(LiquidCrystalBallBlockEntity.BASE_CAPACITY);
        container=stack;
        var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        registries=server==null?RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY):server.registryAccess();
        var data=stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY).copyTag();
        var upgrades=data.getCompound("Upgrades").getList("Items",10);
        int multiplier=1;
        for(int i=0;i<upgrades.size();i++){
            var kind=OrbUpgrades.kind(ItemStack.parseOptional(registries,upgrades.getCompound(i)));
            if(kind!=null)multiplier=Math.max(multiplier,kind.capacityMultiplier);
        }
        setCapacity(LiquidCrystalBallBlockEntity.BASE_CAPACITY*multiplier);
        readFromNBT(registries,data.getCompound("Fluid"));
    }
    @Override public ItemStack getContainer(){return container;}
    private void refresh(){
        readFromNBT(registries,container.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY).copyTag().getCompound("Fluid"));
    }
    @Override public net.neoforged.neoforge.fluids.FluidStack getFluid(){refresh();return super.getFluid();}
    @Override protected void onContentsChanged(){
        var data=container.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY).copyTag();
        data.put("Fluid",writeToNBT(registries,new CompoundTag()));
        container.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(data));
    }
    @Override public int fill(net.neoforged.neoforge.fluids.FluidStack stack,FluidAction action){
        refresh();
        return container.getCount()==1&&PortableStorageRules.stabilized(container)?super.fill(stack,action):0;
    }
    @Override public net.neoforged.neoforge.fluids.FluidStack drain(int amount,FluidAction action){
        refresh();
        return container.getCount()==1&&PortableStorageRules.stabilized(container)?super.drain(amount,action):net.neoforged.neoforge.fluids.FluidStack.EMPTY;
    }
    @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack stack,FluidAction action){
        refresh();return super.drain(stack,action);
    }
}
