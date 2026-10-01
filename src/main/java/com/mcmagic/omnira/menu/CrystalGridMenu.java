package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.item.CrystalGridItem;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.registry.ModMenuTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import top.theillusivec4.curios.api.CuriosApi;

public final class CrystalGridMenu extends AbstractContainerMenu {
    public final int capacity;
    private final Player owner;
    private final int source;
    private final ItemStack bound;
    private final SimpleContainer contents;
    private int clientCursor;
    private boolean loading=true;

    public CrystalGridMenu(int id,Inventory inventory,RegistryFriendlyByteBuf data) {
        this(id,inventory,data.readInt(),data.readInt(),true);
    }
    public CrystalGridMenu(int id,Inventory inventory,int source) {
        this(id,inventory,source,((CrystalGridItem)locate(inventory.player,source).getItem()).capacity,false);
    }
    private CrystalGridMenu(int id,Inventory inventory,int source,int capacity,boolean client) {
        super(ModMenuTypes.CRYSTAL_GRID.get(),id);
        this.capacity=capacity;this.owner=inventory.player;this.source=source;
        this.bound=client?ItemStack.EMPTY:locate(owner,source);
        contents=new SimpleContainer(capacity) {
            @Override public void setChanged() {
                super.setChanged();
                if(!loading && !owner.level().isClientSide && stillValid(owner)) {
                    NonNullList<ItemStack> stacks=NonNullList.withSize(capacity,ItemStack.EMPTY);
                    for(int i=0;i<capacity;i++) stacks.set(i,getItem(i).copy());
                    bound.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(stacks));
                    bound.set(ModDataComponents.GRID_CURSOR,0);
                }
            }
        };
        if(!client) {
            NonNullList<ItemStack> stacks=NonNullList.withSize(capacity,ItemStack.EMPTY);
            bound.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(stacks);
            for(int i=0;i<capacity;i++) contents.setItem(i,stacks.get(i));
        }
        loading=false;
        for(int i=0;i<capacity;i++) {
            final int index=i;
            addSlot(new Slot(contents,i,slotX(capacity,i),slotY(capacity,i)) {
                @Override public boolean mayPlace(ItemStack stack) { return isCrystal(stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) inventorySlot(inventory,9+row*9+col,8+col*18,158+row*18);
        for(int col=0;col<9;col++) inventorySlot(inventory,col,8+col*18,216);
        addDataSlot(new DataSlot() {
            @Override public int get() { return owner.level().isClientSide?clientCursor:next(bound,capacity); }
            @Override public void set(int value) { clientCursor=value; }
        });
    }
    private void inventorySlot(Inventory inventory,int index,int x,int y) {
        addSlot(new Slot(inventory,index,x,y) {
            @Override public boolean mayPickup(Player player) { return source!=getContainerSlot(); }
            @Override public boolean mayPlace(ItemStack stack) { return source!=getContainerSlot(); }
        });
    }
    public static boolean isCrystal(ItemStack stack) {
        return stack.getItem() instanceof com.mcmagic.omnira.item.LowTierMagicCrystalItem && stack.has(ModDataComponents.SPELL_PATTERN);
    }
    public static ItemStack locate(Player player,int source) {
        if(source==-2) return com.mcmagic.omnira.item.PrimarySpellAccessory.equipped(player,"crystal_grid");
        return source==-1?player.getOffhandItem():source>=0 && source<36?player.getInventory().getItem(source):ItemStack.EMPTY;
    }
    public static java.util.OptionalInt editingSource(Player player){
        if(locate(player,-2).getItem() instanceof CrystalGridItem)return java.util.OptionalInt.of(-2);
        for(int i=0;i<36;i++)if(locate(player,i).getItem() instanceof CrystalGridItem)return java.util.OptionalInt.of(i);
        return locate(player,-1).getItem() instanceof CrystalGridItem?java.util.OptionalInt.of(-1):java.util.OptionalInt.empty();
    }
    public static int next(ItemStack grid,int capacity) {
        var contents=grid.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        NonNullList<ItemStack> list=NonNullList.withSize(capacity,ItemStack.EMPTY);
        contents.copyInto(list);
        int cursor=grid.getOrDefault(ModDataComponents.GRID_CURSOR,0);
        for(int i=0;i<capacity;i++) {int slot=(cursor+i)%capacity;if(isCrystal(list.get(slot))) return slot;}
        return -1;
    }
    public ItemStack icon() {return locate(owner,source);}
    public int nextSlot() { return owner.level().isClientSide?clientCursor:next(bound,capacity); }
    public static int slotX(int count,int i) {return 80+(int)Math.round(Math.sin(i*Math.PI*2/count)*52);}
    public static int slotY(int count,int i) {return 72-(int)Math.round(Math.cos(i*Math.PI*2/count)*58);}
    @Override public boolean stillValid(Player player) {
        return player==owner && player.isAlive() && (player.level().isClientSide || locate(player,source)==bound);
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(!stillValid(player)) return;
        if(type==ClickType.SWAP && (button==source || source==-1 && button==40)) return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<capacity?!moveItemStackTo(stack,capacity,slots.size(),true):!isCrystal(stack)||!moveItemStackTo(stack,0,capacity,false)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,stack);
        return copy;
    }
}
