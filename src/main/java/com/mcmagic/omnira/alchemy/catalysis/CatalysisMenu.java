package com.mcmagic.omnira.alchemy.catalysis;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class CatalysisMenu extends AbstractContainerMenu {
    public final CatalysisBlockEntity chamber;
    public CatalysisMenu(int id,Inventory inv,RegistryFriendlyByteBuf data){this(id,inv,find(inv,data));}
    private static CatalysisBlockEntity find(Inventory inv,RegistryFriendlyByteBuf data){
        var pos=data.readBlockPos();var be=CatalysisBlock.controller(inv.player.level(),pos);if(be!=null)return be;
        be=new CatalysisBlockEntity(pos,CatalysisContent.BLOCK.get().defaultBlockState());be.setLevel(inv.player.level());return be;
    }
    public CatalysisMenu(int id,Inventory inv,CatalysisBlockEntity chamber){
        super(CatalysisContent.MENU.get(),id);this.chamber=chamber;
        addSlot(new SlotItemHandler(chamber.items,0,62,57));addSlot(new SlotItemHandler(chamber.items,1,98,57));
        addSlot(new SlotItemHandler(chamber.items,2,80,25){@Override public boolean mayPlace(ItemStack stack){return false;}});
        addSlot(new SlotItemHandler(chamber.upgrades,0,44,123));addSlot(new SlotItemHandler(chamber.upgrades,1,116,123));
        addSlot(new SlotItemHandler(chamber.upgrades,2,80,123));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,166+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,224));
    }
    @Override public boolean stillValid(Player player){return chamber.complete()&&stillValid(ContainerLevelAccess.create(player.level(),chamber.getBlockPos()),player,CatalysisContent.BLOCK.get());}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();boolean moved;
        if(index<6)moved=moveItemStackTo(stack,6,slots.size(),true);
        else if(com.mcmagic.omnira.block.entity.OrbUpgrades.kind(stack)!=null)moved=moveItemStackTo(stack,3,6,false);
        else moved=moveItemStackTo(stack,0,2,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
