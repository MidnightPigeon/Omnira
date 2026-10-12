package com.mcmagic.omnira.workstation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class CrystalWorkstationMenu extends AbstractContainerMenu {
    public final CrystalWorkstationEntity machine;
    public final int index;
    public final net.minecraft.core.BlockPos position;
    private final Container storage;
    private final ContainerData data;
    public CrystalWorkstationMenu(int id,Inventory inv,RegistryFriendlyByteBuf b){
        this(id,inv,CrystalWorkstations.work(inv.player.level().getBlockEntity(b.readBlockPos())),true);
    }
    public CrystalWorkstationMenu(int id,Inventory inv,CrystalWorkstationEntity machine){this(id,inv,machine,false);}
    private CrystalWorkstationMenu(int id,Inventory inv,CrystalWorkstationEntity work,boolean client){
        super(CrystalWorkstations.MENU.get(),id);this.machine=client?null:work;index=work==null?0:CrystalWorkstations.index(work.getBlockState());
        position=work==null?net.minecraft.core.BlockPos.ZERO:work.getBlockPos();storage=machine==null?new SimpleContainer(3):machine;
        data=machine==null?new SimpleContainerData(7):new ContainerData(){
            public int get(int i){return switch(i){case 0->machine.durability;case 1->machine.progress;case 2->machine.duration;case 3->machine.burn;case 4->machine.burnTotal;case 5->machine.kind()==CrystalWorkstations.Kind.ANVIL?machine.recipe().levels():0;default->machine.powered()?1:0;};}
            public void set(int i,int v){}public int getCount(){return 7;}
        };
        addDataSlots(data);
        int[] xs=switch(index%3){case 0->new int[]{56,56,116};case 1->new int[]{49,49,129};default->new int[]{27,76,134};};
        int[] ys=switch(index%3){case 0->new int[]{17,53,35};case 1->new int[]{19,40,34};default->new int[]{47,47,47};};
        for(int i=0;i<2;i++){
            final int slot=i;
            addSlot(new Slot(storage,i,xs[i],ys[i]){
                @Override public boolean mayPlace(ItemStack s){return machine==null||machine.canPlaceItem(slot,s);}
                @Override public int getMaxStackSize(){return index%3==1?1:64;}
            });
        }
        addSlot(new Slot(storage,2,xs[2],ys[2]){
            @Override public ItemStack getItem(){return machine==null?super.getItem():machine.preview();}
            @Override public boolean mayPlace(ItemStack s){return false;}
            @Override public boolean mayPickup(Player p){return machine==null||machine.canTake(p);}
            @Override public ItemStack remove(int amount){return machine==null?super.remove(amount):machine.take(inv.player,amount);}
            @Override public void set(ItemStack s){if(machine==null||!machine.getItem(2).isEmpty())super.set(s);}
            @Override public void onTake(Player p,ItemStack s){}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+18*col,84+18*row));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+18*col,142));
    }
    public int value(int i){return data.get(i);}
    @Override public boolean stillValid(Player p){return machine==null||machine.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int slotIndex){
        if(slotIndex<0||slotIndex>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(slotIndex);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        var copy=slot.getItem().copy();
        if(slotIndex==2){
            if(machine==null)return ItemStack.EMPTY;
            var result=machine.take(p,copy.getCount());
            moveItemStackTo(result,3,39,true);
            if(!result.isEmpty())p.getInventory().placeItemBackInInventory(result);
            return copy;
        }
        var stack=slot.getItem();
        if(slotIndex<3){if(!moveItemStackTo(stack,3,39,true))return ItemStack.EMPTY;}
        else if(index%3==0){
            boolean smeltable=p.level().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,new net.minecraft.world.item.crafting.SingleRecipeInput(stack),p.level()).isPresent();
            boolean moved=smeltable?moveItemStackTo(stack,0,1,false):storage.canPlaceItem(1,stack)?moveItemStackTo(stack,1,2,false):slotIndex<30?moveItemStackTo(stack,30,39,false):moveItemStackTo(stack,3,30,false);
            if(!moved)return ItemStack.EMPTY;
        }else if(!moveItemStackTo(stack,0,2,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;
    }
    public boolean configure(Player player,String value,boolean rename){
        if(machine==null||!stillValid(player)||!player.level().mayInteract(player,machine.getBlockPos()))return false;
        if(rename){if(machine.kind()!=CrystalWorkstations.Kind.ANVIL||value.length()>50)return false;machine.rename=value;}
        else{
            if(machine.kind()!=CrystalWorkstations.Kind.GRINDSTONE||!machine.infused())return false;
            if(!WorkstationRecipes.singleInput(machine.getItem(0),machine.getItem(1)))return false;
            machine.recipe();
            if(WorkstationRecipes.enchantments(machine).keySet().stream().noneMatch(e->e.getRegisteredName().equals(value)))return false;
            if(!machine.removedEnchantments.add(value))machine.removedEnchantments.remove(value);
        }
        machine.changedSelection();broadcastChanges();return true;
    }
}
