package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** Either warp point's complete stack collapses into one aggregate in unprotected storage. */
public final class TimeWarpStorage {
    private TimeWarpStorage(){}
    private static boolean warpPoint(ItemStack stack){return stack.is(ModItems.TIME_WARP_POINT.get())||stack.is(ModItems.SPACE_WARP_POINT.get());}
    public static boolean container(Container storage,boolean immune){
        if(immune || storage instanceof net.minecraft.world.inventory.ResultContainer)return false;
        boolean active=false;
        for(int slot=0;slot<storage.getContainerSize();slot++){
            var stack=storage.getItem(slot);
            if(!warpPoint(stack) || storage instanceof Inventory && (slot<9 || slot>=36))continue;
            active=true;
            if(stack.getCount()==16)storage.setItem(slot,new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()));
        }
        return active;
    }
    public static boolean handler(IItemHandler storage,boolean immune){
        if(immune || !(storage instanceof IItemHandlerModifiable mutable))return false;
        boolean active=false;
        for(int slot=0;slot<storage.getSlots();slot++){
            var stack=storage.getStackInSlot(slot);
            if(!warpPoint(stack))continue;
            active=true;
            if(stack.getCount()==16)mutable.setStackInSlot(slot,new ItemStack(ModItems.UNSTABLE_SPACETIME_AGGREGATE.get()));
        }
        return active;
    }
}
