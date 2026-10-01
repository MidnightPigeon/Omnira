package com.mcmagic.omnira.vehicle;

import com.mcmagic.omnira.item.OrbUpgradeItem;
import com.mcmagic.omnira.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;

public final class CruiseOrbMenu extends AbstractContainerMenu {
    public final CruiseOrbEntity orb;
    public static final int PORT=12,UPGRADES=13,CORE=16,TRASH=17,PLAYER=18;
    public CruiseOrbMenu(int id,Inventory inv,RegistryFriendlyByteBuf data){this(id,inv,(CruiseOrbEntity)inv.player.level().getEntity(data.readInt()));}
    public CruiseOrbMenu(int id,Inventory inv,CruiseOrbEntity orb){
        super(ModMenuTypes.CRUISE_ORB.get(),id);this.orb=orb;
        int index=0;
        for(int row=0;row<4;row++){
            int columns=row==0 || row==3?2:4;
            for(int col=0;col<columns;col++)addSlot(new SlotItemHandler(orb.storage.items,index++,288-columns*9+col*18,29+row*18){
                @Override public int getMaxStackSize(ItemStack stack){return stack.getMaxStackSize()>1?stack.getMaxStackSize()*orb.storage.multiplier():1;}
                @Override public int getMaxStackSize(){return 64*orb.storage.multiplier();}
            });
        }
        addSlot(new SlotItemHandler(orb.storage.port,0,-26,55));
        for(int i=0;i<3;i++){
            final int slot=i;
            addSlot(new SlotItemHandler(orb.storage.upgrades,i,153+i*27,119){
                @Override public boolean mayPickup(Player player){
                    int multiplier=1;
                    for(int n=0;n<3;n++)if(n!=slot && orb.storage.upgrades.getStackInSlot(n).getItem() instanceof OrbUpgradeItem plate)multiplier=Math.max(multiplier,plate.kind.capacityMultiplier);
                    return orb.storage.fitsMultiplier(multiplier);
                }
            });
        }
        var coreSlot=new ItemStackHandler(1){
            @Override public int getSlotLimit(int slot){return 1;}
            @Override public boolean isItemValid(int slot,ItemStack stack){return stack.getItem() instanceof com.mcmagic.omnira.item.SpellCoreItem;}
            @Override protected void onContentsChanged(int slot){orb.setCore(getStackInSlot(slot));}
        };
        coreSlot.setStackInSlot(0,orb.core().copy());addSlot(new SlotItemHandler(coreSlot,0,180,25));
        addSlot(new SlotItemHandler(orb.storage.trash,0,180,85));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,108+col*18,160+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,108+col*18,218));
    }
    @Override public boolean stillValid(Player player){return orb!=null && orb.isAlive() && player.getVehicle()==orb;}
    @Override public boolean clickMenuButton(Player player,int button){
        return button==0 && stillValid(player) && player instanceof net.minecraft.server.level.ServerPlayer server && CruiseVortex.activate(orb,server);
    }
    @Override public void clicked(int index,int button,ClickType type,Player player){
        if(!stillValid(player))return;
        if(index==TRASH){
            var slot=slots.get(TRASH);
            if(type==ClickType.PICKUP && !getCarried().isEmpty() && (button==0 || button==1)){
                var carried=getCarried();
                if(slot.mayPlace(carried))slot.setByPlayer(carried.split(button==1?1:Math.min(carried.getCount(),slot.getMaxStackSize(carried))));
                return;
            }
            if(type==ClickType.SWAP && (button>=0 && button<9 || button==40)){
                var incoming=player.getInventory().getItem(button);
                if(!incoming.isEmpty()){
                    if(slot.mayPlace(incoming))slot.setByPlayer(incoming.split(Math.min(incoming.getCount(),slot.getMaxStackSize(incoming))));
                    return;
                }
            }
        }
        if(index>=0 && index<12 && type==ClickType.PICKUP){
            var slot=slots.get(index);var stored=slot.getItem();var carried=getCarried();
            if(carried.isEmpty()){
                int take=Math.min(stored.getCount(),stored.getMaxStackSize());if(button==1)take=(take+1)/2;setCarried(slot.remove(take));
            } else if(slot.mayPlace(carried) && (stored.isEmpty() || ItemStack.isSameItemSameComponents(stored,carried))){
                int amount=Math.min(slot.getMaxStackSize(carried)-stored.getCount(),button==1?1:carried.getCount());
                if(amount>0){slot.set(carried.copyWithCount(stored.getCount()+amount));carried.shrink(amount);}
            } else if(button==0 && slot.mayPlace(carried) && stored.getCount()<=stored.getMaxStackSize() && carried.getCount()<=slot.getMaxStackSize(carried)){
                slot.set(carried);setCarried(stored);
            }
            slot.setChanged();return;
        }
        if(index>=0 && index<12 && type==ClickType.SWAP && slots.get(index).getItem().getCount()>slots.get(index).getItem().getMaxStackSize()){
            if((button>=0 && button<9 || button==40) && player.getInventory().getItem(button).isEmpty()){
                var slot=slots.get(index);player.getInventory().setItem(button,slot.remove(slot.getItem().getMaxStackSize()));slot.setChanged();
            }return;
        }
        if(index>=0 && index<12 && type==ClickType.THROW && getCarried().isEmpty()){
            var slot=slots.get(index);player.drop(slot.remove(button==0?1:Math.min(slot.getItem().getCount(),slot.getItem().getMaxStackSize())),true);slot.setChanged();return;
        }
        super.clicked(index,button,type,player);
    }
    @Override public boolean canDragTo(Slot slot){return slot!=slots.get(TRASH) && super.canDragTo(slot);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player) || index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem() || !slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<PLAYER){
            while(!stack.isEmpty()){
                int before=stack.getCount();if(!moveItemStackTo(stack,PLAYER,slots.size(),true) || before==stack.getCount())break;
            }
        } else if(stack.getItem() instanceof OrbUpgradeItem){moveItemStackTo(stack,UPGRADES,CORE,false);}
        else if(stack.getItem() instanceof com.mcmagic.omnira.item.SpellCoreItem){moveItemStackTo(stack,CORE,CORE+1,false);}
        else {
            if(orb.storage.port.isItemValid(0,stack) && !slots.get(PORT).hasItem())moveItemStackTo(stack,PORT,UPGRADES,false);
            else stack.setCount(orb.storage.insert(stack,false).getCount());
        }
        if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
