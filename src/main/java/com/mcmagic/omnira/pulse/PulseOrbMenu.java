package com.mcmagic.omnira.pulse;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class PulseOrbMenu extends AbstractContainerMenu {
    public final PulseOrbEntity orb;
    public final int playerStart;
    public PulseOrbMenu(int id,Inventory inventory,RegistryFriendlyByteBuf buffer){this(id,inventory,PulseOrbEntity.from(inventory.player.level().getBlockEntity(buffer.readBlockPos())));}
    public PulseOrbMenu(int id,Inventory inventory,PulseOrbEntity orb){
        super(PulseContent.MENU.get(),id);this.orb=orb;
        addSlot(new net.neoforged.neoforge.items.SlotItemHandler(orb.upgrade,0,80,34));
        if(orb.resonant())for(int i=0;i<2;i++){
            addSlot(new Slot(new net.minecraft.world.SimpleContainer(2){
                @Override public ItemStack getItem(int index){return orb.frequency[index];}
                @Override public void setItem(int index,ItemStack stack){orb.frequency[index]=stack.copyWithCount(1);}
            },i,184,16+i*27){
                @Override public boolean mayPickup(Player player){return false;}
                @Override public boolean mayPlace(ItemStack stack){return false;}
            });
        }
        playerStart=orb.resonant()?3:1;
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,x+y*9+9,8+x*18,84+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,8+x*18,142));
    }
    @Override public boolean stillValid(Player player){return !orb.isRemoved()&&player.level()==orb.getLevel()&&player.canInteractWithBlock(orb.getBlockPos(),4);}
    @Override public boolean clickMenuButton(Player player,int id){if(!stillValid(player)||id<0||id>1)return false;orb.frequency(id,getCarried());return true;}
    @Override public void clicked(int index,int button,ClickType type,Player player){
        if(com.mcmagic.omnira.menu.UpgradeMenuFeedback.rejectClick(this,index,button,type,player,0,1))return;
        if(orb.resonant()&&index>=1&&index<3){if(type==ClickType.PICKUP&&(button==0||button==1)&&!player.level().isClientSide&&stillValid(player))orb.frequency(index-1,getCarried());return;}
        super.clicked(index,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size()||index>0&&index<playerStart)return ItemStack.EMPTY;
        var slot=getSlot(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index==0){if(!moveItemStackTo(stack,playerStart,slots.size(),true))return ItemStack.EMPTY;}
        else if(!orb.accepts(stack)||!moveItemStackTo(stack,0,1,false)){
            com.mcmagic.omnira.menu.UpgradeMenuFeedback.incompatible(this,stack,player,0,1);return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.set(stack);
        slot.setChanged();return copy;
    }
}
