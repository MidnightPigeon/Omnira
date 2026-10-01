package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.item.ArcaneArquebusItem;
import com.mcmagic.omnira.registry.ModMenuTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class ArquebusMenu extends AbstractContainerMenu {
    private final Player owner;
    private final int source;
    private final ItemStack bound;
    private final boolean sword;
    private boolean loading=true;
    public ArquebusMenu(int id,Inventory inventory,RegistryFriendlyByteBuf data) {this(id,inventory,data.readInt());}
    public ArquebusMenu(int id,Inventory inventory,int source) {
        this(id,inventory,source,false);
    }
    public static ArquebusMenu sword(int id,Inventory inventory,int source) {return new ArquebusMenu(id,inventory,source,true);}
    public static ArquebusMenu sword(int id,Inventory inventory,RegistryFriendlyByteBuf data) {return sword(id,inventory,data.readInt());}
    private boolean validSource() {return source>=0 && source<36 || sword && source==40;}
    private boolean accepts(ItemStack stack) {return sword?com.mcmagic.omnira.item.RitualSwordItem.accepts(stack):ArcaneArquebusItem.accepts(stack);}
    private ArquebusMenu(int id,Inventory inventory,int source,boolean sword) {
        super(sword?ModMenuTypes.SWORD_SPELL.get():ModMenuTypes.ARQUEBUS.get(),id);
        this.sword=sword;
        this.owner=inventory.player;this.source=source;
        bound=validSource()?inventory.getItem(source):ItemStack.EMPTY;
        var contents=new SimpleContainer(1) {
            @Override public void setChanged() {
                super.setChanged();
                if(!loading && !owner.level().isClientSide && stillValid(owner))
                    bound.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(java.util.List.of(getItem(0).copy())));
            }
        };
        if(!owner.level().isClientSide) contents.setItem(0,ArcaneArquebusItem.crystal(bound).copy());
        loading=false;
        addSlot(new Slot(contents,0,80,35) {
            @Override public boolean mayPlace(ItemStack stack) {return accepts(stack);}
            @Override public int getMaxStackSize() {return 1;}
        });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addInventorySlot(inventory,9+row*9+col,8+col*18,84+row*18);
        for(int col=0;col<9;col++) addInventorySlot(inventory,col,8+col*18,142);
    }
    private void addInventorySlot(Inventory inventory,int index,int x,int y) {
        addSlot(new Slot(inventory,index,x,y) {
            @Override public boolean mayPickup(Player player) {return getContainerSlot()!=source;}
            @Override public boolean mayPlace(ItemStack stack) {return getContainerSlot()!=source;}
        });
    }
    @Override public boolean stillValid(Player player) {
        return player==owner && player.isAlive() && validSource()
                && (player.level().isClientSide || owner.getInventory().getItem(source)==bound
                && (sword?bound.getItem() instanceof com.mcmagic.omnira.item.RitualSwordItem:bound.getItem() instanceof ArcaneArquebusItem));
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(!stillValid(player) || type==ClickType.SWAP && button==source) return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(!stillValid(player) || index<0 || index>=slots.size()) return ItemStack.EMPTY;
        var slot=slots.get(index);
        if(!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(index==0?!moveItemStackTo(stack,1,slots.size(),true):!accepts(stack)||!moveItemStackTo(stack,0,1,false)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return copy;
    }
}
