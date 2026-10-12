package com.mcmagic.omnira.alchemy;

import com.mcmagic.omnira.registry.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class ReactionPoolMenu extends AbstractContainerMenu {
    public final ReactionPoolBlockEntity pool;
    private final ContainerLevelAccess access;
    public ReactionPoolMenu(int id,Inventory inv,RegistryFriendlyByteBuf data){this(id,inv,clientPool(inv,data));}
    private static ReactionPoolBlockEntity clientPool(Inventory inv,RegistryFriendlyByteBuf data){
        var pos=data.readBlockPos();var found=ReactionPoolBlock.controller(inv.player.level(),pos);
        if(found!=null)return found;
        var pool=new ReactionPoolBlockEntity(pos,ModBlocks.REACTION_POOL.get().defaultBlockState());pool.setLevel(inv.player.level());return pool;
    }
    public ReactionPoolMenu(int id,Inventory inv,ReactionPoolBlockEntity pool){
        super(ModMenuTypes.REACTION_POOL.get(),id);this.pool=pool;access=ContainerLevelAccess.create(inv.player.level(),pool.getBlockPos());
        for(int i=0;i<9;i++){final int slot=i;addSlot(new SlotItemHandler(pool.items,i,166+i%3*20,26+i/3*20){
            @Override public int getMaxStackSize(ItemStack stack){
                int low=0,high=pool.itemLimit(stack,pool.multiplier(-1));
                while(low<high){int mid=(low+high+1)/2;if(pool.canSetItem(slot,stack.copyWithCount(mid)))low=mid;else high=mid-1;}return low;
            }
            @Override public boolean mayPlace(ItemStack stack){return pool.canSetItem(slot,stack.copyWithCount(1));}
            @Override public void set(ItemStack stack){if(pool.canSetItem(slot,stack))super.set(stack);}
        });}
        for(int i=0;i<4;i++){final int slot=i;addSlot(new SlotItemHandler(pool.upgrades,i,156+i*20,105){
            @Override public boolean mayPickup(Player player){return pool.fitsMultiplier(pool.multiplier(slot));}
            @Override public boolean mayPlace(ItemStack stack){return mayPickup(inv.player)&&super.mayPlace(stack);}
        });}
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,45+col*18,156+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,45+col*18,214));
    }
    @Override public boolean clickMenuButton(Player player,int row){
        if(!stillValid(player)||row<0||row>=pool.fluids.count())return false;
        pool.fluids.moveLast(row);return true;
    }
    public boolean stillValid(Player player){return stillValid(access,player,ModBlocks.REACTION_POOL.get());}
    @Override public void clicked(int index,int button,ClickType type,Player player){
        if(index>=0&&index<9&&type==ClickType.PICKUP){
            var slot=slots.get(index);var stored=slot.getItem();var carried=getCarried();
            if(carried.isEmpty()){
                int take=Math.min(stored.getCount(),stored.getMaxStackSize());if(button==1)take=(take+1)/2;setCarried(slot.remove(take));
            }else if(slot.mayPlace(carried)&&(stored.isEmpty()||ItemStack.isSameItemSameComponents(stored,carried))){
                var rest=pool.items.insertItem(index,carried.copyWithCount(button==1?1:carried.getCount()),false);
                carried.shrink((button==1?1:carried.getCount())-rest.getCount());
            }else if(button==0&&stored.getCount()<=stored.getMaxStackSize()&&pool.canSetItem(index,carried)){
                slot.set(carried);setCarried(stored);
            }
            slot.setChanged();return;
        }
        if(index>=0&&index<9&&type==ClickType.SWAP&&slots.get(index).getItem().getCount()>slots.get(index).getItem().getMaxStackSize()){
            if((button>=0&&button<9||button==40)&&player.getInventory().getItem(button).isEmpty()){
                var slot=slots.get(index);player.getInventory().setItem(button,slot.remove(slot.getItem().getMaxStackSize()));slot.setChanged();
            }return;
        }
        if(index>=0&&index<9&&type==ClickType.THROW&&getCarried().isEmpty()){
            var slot=slots.get(index);player.drop(slot.remove(button==0?1:Math.min(slot.getItem().getCount(),slot.getItem().getMaxStackSize())),true);return;
        }
        super.clicked(index,button,type,player);
    }
    public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        boolean moved;
        if(index<13)moved=moveItemStackTo(stack,13,slots.size(),true);
        else if(com.mcmagic.omnira.block.entity.OrbUpgrades.kind(stack)!=null)moved=moveItemStackTo(stack,9,13,false);
        else {int before=stack.getCount();for(int i=0;i<9&&!stack.isEmpty();i++)stack.setCount(pool.items.insertItem(i,stack,false).getCount());moved=stack.getCount()!=before;}
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
