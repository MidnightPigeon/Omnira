package com.mcmagic.omnira.workstation;

import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.item.ItemStack;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class WorkstationCapabilities {
    @net.neoforged.bus.api.SubscribeEvent public static void register(RegisterCapabilitiesEvent e){
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,CrystalWorkstations.ENTITY.get(),(host,face)->CrystalWorkstations.work(host).energyPort(face));
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,CrystalWorkstations.ENTITY.get(),(host,face)->new IItemHandler(){
            private final CrystalWorkstationEntity work=CrystalWorkstations.work(host);
            public int getSlots(){return 3;}
            public ItemStack getStackInSlot(int slot){return work.getItem(slot);}
            public int getSlotLimit(int slot){return slot<2&&work.kind()==CrystalWorkstations.Kind.GRINDSTONE?1:64;}
            public boolean isItemValid(int slot,ItemStack stack){return work.canPlaceItem(slot,stack);}
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
                if(!isItemValid(slot,stack))return stack;
                var old=work.getItem(slot);if(!old.isEmpty()&&!ItemStack.isSameItemSameComponents(old,stack))return stack;
                int n=Math.min(stack.getCount(),Math.min(stack.getMaxStackSize(),getSlotLimit(slot))-old.getCount());
                if(n<=0)return stack;
                if(!simulate){if(old.isEmpty())work.setItem(slot,stack.copyWithCount(n));else{old.grow(n);work.setChanged();}}
                return stack.copyWithCount(stack.getCount()-n);
            }
            public ItemStack extractItem(int slot,int amount,boolean simulate){
                if(slot!=2||amount<=0)return ItemStack.EMPTY;
                return simulate?work.getItem(slot).copyWithCount(Math.min(amount,work.getItem(slot).getCount())):work.removeItem(slot,amount);
            }
        });
    }
}
