package com.mcmagic.omnira.reversal;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ReversalMenu extends AbstractContainerMenu {
    private final Container storage;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    public ReversalMenu(int id,Inventory inv,net.minecraft.network.RegistryFriendlyByteBuf buf){this(id,inv,new SimpleContainer(3),ContainerLevelAccess.create(inv.player.level(),buf.readBlockPos()),new SimpleContainerData(2));}
    public ReversalMenu(int id,Inventory inv,ReversalBlockEntity be){this(id,inv,be,ContainerLevelAccess.create(be.getLevel(),be.getBlockPos()),new ContainerData(){public int get(int i){return i==0?be.energy():be.progress();}public void set(int i,int v){}public int getCount(){return 2;}});}
    private ReversalMenu(int id,Inventory inv,Container c,ContainerLevelAccess a,ContainerData d){
        super(ReversalContent.MENU.get(),id);storage=c;access=a;data=d;addDataSlots(d);
        addSlot(new Slot(c,0,53,61){public boolean mayPlace(ItemStack s){return inv.player.level().getRecipeManager().getAllRecipesFor(ReversalContent.TYPE.get()).stream().anyMatch(r->r.value().ingredient().test(s));}});
        addSlot(new Slot(c,1,107,61){public boolean mayPlace(ItemStack s){return false;}});
        addSlot(new Slot(c,2,-24,112){public boolean mayPlace(ItemStack s){return ReversalBlockEntity.fuel(s)>0;}});
        for(int r=0;r<3;r++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+r*9+col,8+18*col,158+18*r));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+18*col,216));
    }
    public int energy(){return data.get(0);} public int progress(){return data.get(1);}
    public boolean stillValid(Player p){return stillValid(access,p,ReversalContent.MACHINE.get());}
    public ItemStack quickMoveStack(Player p,int i){if(i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);var s=slot.getItem();if(s.isEmpty())return ItemStack.EMPTY;var copy=s.copy();if(i<3){if(!moveItemStackTo(s,3,39,true))return ItemStack.EMPTY;}else if(slots.get(0).mayPlace(s)){if(!moveItemStackTo(s,0,1,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(s,2,3,false))return ItemStack.EMPTY;if(s.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return copy;}
}
