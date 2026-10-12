package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.OrbUpgradeItem;
import com.mcmagic.omnira.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class CrystalBallMenu extends AbstractContainerMenu {
    public final CrystalBallBlockEntity ball;
    public final int storageSlots,upgradeStart,playerStart,frequencyStart;
    public CrystalBallMenu(int id,Inventory inventory,RegistryFriendlyByteBuf data) {
        this(id,inventory,(CrystalBallBlockEntity)inventory.player.level().getBlockEntity(data.readBlockPos()));
    }
    public CrystalBallMenu(int id,Inventory inventory,CrystalBallBlockEntity ball) {
        super(ModMenuTypes.CRYSTAL_BALL.get(),id);this.ball=ball;ball.startOpen(inventory.player);
        storageSlots=liquid()||singleOrb()?1:12;upgradeStart=storageSlots;frequencyStart=upgradeStart+(starOrb()?2:singleOrb()?1:3);playerStart=frequencyStart+(ball.resonant()?2:0);
        if(singleOrb())addSlot(new Slot(ball,0,80,55){@Override public int getMaxStackSize(ItemStack stack){return ball.getMaxStackSize(stack);}});
        else if(liquid())addSlot(new Slot(ball,0,-26,55) {
            @Override public int getMaxStackSize(){return 1;}
            @Override public int getMaxStackSize(ItemStack stack){return 1;}
            @Override public boolean mayPlace(ItemStack stack){return ball.canPlaceItem(0,stack);}
        });
        else {
            int slot=0;
            for(int row=0;row<4;row++) {
                int columns=row==0 || row==3?2:4;
                for(int col=0;col<columns;col++)addSlot(new Slot(ball,slot++,88-columns*9+col*18,29+row*18) {
                    @Override public int getMaxStackSize(ItemStack stack){return ball.getMaxStackSize(stack);}
                });
            }
        }
        for(int i=0;i<(starOrb()?2:singleOrb()?1:3);i++) {
            final int slot=starOrb()?i:singleOrb()?1:i;
            addSlot(new SlotItemHandler(ball.upgrades,slot,starOrb()?66+i*28:singleOrb()?80:53+i*27,119) {
                @Override public boolean mayPickup(Player player){return ball.upgrades.mayRemove(slot);}
            });
        }
        if(ball.resonant())for(int i=0;i<2;i++){
            final int frequency=i;
            addSlot(new Slot(new SimpleContainer(2){
                @Override public ItemStack getItem(int index){return ball.frequency[index];}
                @Override public void setItem(int index,ItemStack stack){ball.frequency[index]=stack.copyWithCount(1);}
            },i,184,43+i*27){
                @Override public boolean mayPickup(Player player){return false;}
                @Override public boolean mayPlace(ItemStack stack){return false;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,160+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,218));
    }
    public boolean liquid(){return ball instanceof LiquidCrystalBallBlockEntity;}
    public boolean voidOrb(){return ball instanceof VoidCrystalBallBlockEntity;}
    public boolean starOrb(){return ball instanceof com.mcmagic.omnira.space.sea.StarClusterOrbEntity;}
    public boolean singleOrb(){return voidOrb()||starOrb();}
    @Override public void clicked(int index,int button,ClickType type,Player player) {
        if(ball.resonant()&&index>=frequencyStart&&index<frequencyStart+2){
            if(!player.level().isClientSide)ball.setFrequency(index-frequencyStart,getCarried());return;
        }
        if(index>=0 && index<storageSlots && type==ClickType.PICKUP) {
            var slot=slots.get(index);var stored=slot.getItem();var carried=getCarried();
            if(carried.isEmpty()) {
                int take=Math.min(stored.getCount(),stored.getMaxStackSize());if(button==1)take=(take+1)/2;setCarried(slot.remove(take));
            } else if(slot.mayPlace(carried) && (stored.isEmpty() || ItemStack.isSameItemSameComponents(stored,carried))) {
                int amount=Math.min(slot.getMaxStackSize(carried)-stored.getCount(),button==1?1:carried.getCount());
                if(amount>0){slot.set(carried.copyWithCount(stored.getCount()+amount));carried.shrink(amount);}
            } else if(button==0 && slot.mayPlace(carried) && stored.getCount()<=stored.getMaxStackSize() && carried.getCount()<=slot.getMaxStackSize(carried)) {
                slot.set(carried);setCarried(stored);
            }
            slot.setChanged();return;
        }
        if(index>=0 && index<storageSlots && type==ClickType.SWAP && slots.get(index).getItem().getCount()>slots.get(index).getItem().getMaxStackSize()) {
            if((button>=0 && button<9 || button==40) && player.getInventory().getItem(button).isEmpty()) {
                var slot=slots.get(index);player.getInventory().setItem(button,slot.remove(slot.getItem().getMaxStackSize()));slot.setChanged();
            }
            return;
        }
        if(index>=0 && index<storageSlots && type==ClickType.THROW && getCarried().isEmpty()) {
            var slot=slots.get(index);var stack=slot.getItem();
            player.drop(slot.remove(button==0?1:Math.min(stack.getCount(),stack.getMaxStackSize())),true);slot.setChanged();return;
        }
        super.clicked(index,button,type,player);
    }
    @Override public boolean stillValid(Player player){return ball.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem() || !slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<playerStart) {
            boolean moved=false;
            while(!stack.isEmpty()) {
                int before=stack.getCount();
                if(!moveItemStackTo(stack,playerStart,slots.size(),true) || stack.getCount()==before)break;
                moved=true;
            }
            if(!moved)return ItemStack.EMPTY;
        } else if(stack.getItem() instanceof OrbUpgradeItem) {
            if(!moveItemStackTo(stack,upgradeStart,playerStart,false)&&(!singleOrb()||!moveItemStackTo(stack,0,1,false)))return ItemStack.EMPTY;
        } else if(liquid()) {
            if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        } else {
            for(int i=0;i<storageSlots && !stack.isEmpty();i++) {var remainder=ball.itemHandler.insertItem(i,stack,false);stack.setCount(remainder.getCount());}
            if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
    @Override public void removed(Player player){super.removed(player);ball.stopOpen(player);}
}
