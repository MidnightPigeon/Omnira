package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.OrbUpgrades;
import com.mcmagic.omnira.item.OrbUpgradeItem;
import com.mcmagic.omnira.registry.ModMenuTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Edits the held plate, never the installed container or its contents. */
public final class OrbUpgradeMenu extends AbstractContainerMenu {
    private final Player owner;
    private final int source;
    private final ItemStack bound;
    private final SimpleContainer filters;
    private final DataSlot whitelist=DataSlot.standalone(),directions=DataSlot.standalone();
    public final boolean intake;
    public final boolean recipeControl;
    private final int filterCount;
    public OrbUpgradeMenu(int id,Inventory inventory,RegistryFriendlyByteBuf data) {
        this(id,inventory,data.readInt(),data.readBoolean());
    }
    public OrbUpgradeMenu(int id,Inventory inventory,int source,boolean intake) {
        super(ModMenuTypes.ORB_UPGRADE.get(),id);
        this.owner=inventory.player;this.source=source;this.intake=intake;
        bound=inventory.getItem(source);
        recipeControl=OrbUpgrades.kind(bound)==OrbUpgradeItem.Kind.RECIPE_CONTROL;
        filterCount=recipeControl?9:3;filters=new SimpleContainer(filterCount);
        for(int i=0;i<filterCount;i++)addSlot(new Slot(filters,i,62+i%3*18,30+(recipeControl?i/3*18:0)) {
            @Override public boolean mayPickup(Player player){return false;}
            @Override public boolean mayPlace(ItemStack stack){return false;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)inventorySlot(inventory,9+row*9+col,8+18*col,133+18*row);
        for(int col=0;col<9;col++)inventorySlot(inventory,col,8+18*col,191);
        addDataSlot(whitelist);addDataSlot(directions);
        broadcastChanges();
    }
    private void inventorySlot(Inventory inventory,int index,int x,int y) {
        addSlot(new Slot(inventory,index,x,y) {
            @Override public boolean mayPickup(Player player){return index!=source;}
            @Override public boolean mayPlace(ItemStack stack){return index!=source;}
        });
    }
    public boolean whitelist(){return whitelist.get()!=0;}
    public int directions(){return directions.get();}
    @Override public boolean stillValid(Player player) {
        return player==owner && player.isAlive() && (player.level().isClientSide ||
                owner.getInventory().getItem(source)==bound && OrbUpgrades.kind(bound)==
                        (recipeControl?OrbUpgradeItem.Kind.RECIPE_CONTROL:intake?OrbUpgradeItem.Kind.INTAKE:OrbUpgradeItem.Kind.OUTPUT));
    }
    @Override public void broadcastChanges() {
        if(!owner.level().isClientSide && stillValid(owner)) {
            var tag=OrbUpgrades.settings(bound);
            whitelist.set(tag.getBoolean("Whitelist")?1:0);
            directions.set(tag.contains("Directions")?tag.getInt("Directions"):1);
            for(int i=0;i<filterCount;i++)filters.setItem(i,ItemStack.parseOptional(owner.registryAccess(),tag.getCompound("Filter"+i)));
        }
        super.broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player,int id) {
        if(player.level().isClientSide || !stillValid(player))return false;
        var tag=OrbUpgrades.settings(bound);
        if(id==0)tag.putBoolean("Whitelist",!tag.getBoolean("Whitelist"));
        else if(!recipeControl && id>=1 && id<=6)
            tag.putInt("Directions",directions.get()^(1<<(id-1)));
        else return false;
        bound.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        owner.getInventory().setChanged();broadcastChanges();return true;
    }
    @Override public void clicked(int index,int button,ClickType type,Player player) {
        if(!stillValid(player))return;
        if(type==ClickType.SWAP && button==source)return;
        if(index>=filterCount && index<slots.size() && slots.get(index).getContainerSlot()==source)return;
        if(index>=0 && index<filterCount) {
            if(type==ClickType.PICKUP && !player.level().isClientSide) {
                OrbUpgrades.setFilter(bound,index,!recipeControl&&button==1?ItemStack.EMPTY:getCarried(),player.registryAccess());
                owner.getInventory().setChanged();broadcastChanges();
            }
            return;
        }
        super.clicked(index,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(!stillValid(player) || index<filterCount || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem() || !slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(index<filterCount+27?!moveItemStackTo(stack,filterCount+27,filterCount+36,false):!moveItemStackTo(stack,filterCount,filterCount+27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return copy;
    }
}
