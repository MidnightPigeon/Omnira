package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.item.PortableTableItem;
import com.mcmagic.omnira.registry.ModMenuTypes;
import com.mcmagic.omnira.spell.SpellPattern;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class PortableTableMenu extends AbstractContainerMenu {
    private final int source;
    private final ItemStack bound;
    private final Player owner;

    public PortableTableMenu(int id, Inventory inv, RegistryFriendlyByteBuf buffer) {
        this(id,inv,buffer.readInt());
    }

    public PortableTableMenu(int id, Inventory inv, int source) {
        super(ModMenuTypes.PORTABLE_TABLE.get(),id);
        this.source = source;
        this.owner = inv.player;
        this.bound = inv.getItem(source);
        var contents = PortableTableItem.load(bound);
        int[][] positions = {{52,50},{108,50},{80,75},{80,25},{80,50}};
        for (int i = 0; i < positions.length; i++) {
            final int slot = i;
            addSlot(new FilteredSlot(contents,i,positions[i][0],positions[i][1],stack -> PortableTableItem.accepts(contents,slot,stack)));
        }
        contents.addListener(c -> {
            if (!owner.level().isClientSide && stillValid(owner)) PortableTableItem.store(bound,c);
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) playerSlot(inv,9+row*9+col,8+18*col,112+18*row);
        for (int col = 0; col < 9; col++) playerSlot(inv,col,8+18*col,170);
    }

    private void playerSlot(Inventory inv,int i,int x,int y) {
        addSlot(new Slot(inv,i,x,y) {
            @Override public boolean mayPickup(Player player) { return i != source; }
            @Override public boolean mayPlace(ItemStack stack) { return i != source; }
        });
    }

    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && (player.level().isClientSide || player.getInventory().getItem(source) == bound);
    }

    @Override public void clicked(int i,int button,ClickType type,Player player) {
        if (stillValid(player) && !(type == ClickType.SWAP && button == source)) super.clicked(i,button,type,player);
    }

    @Override public ItemStack quickMoveStack(Player player,int i) {
        if (!stillValid(player) || i < 0 || i >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(i);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var before = stack.copy();
        int size = PortableTableItem.SIZE;
        boolean moved;
        if (i < size) moved = moveItemStackTo(stack,size,slots.size(),true);
        else {
            int target = SpellPattern.cluster(stack) && !getSlot(1).hasItem() ? 1 : 0;
            moved = false;
            for (int j = 0; j < size && !moved; j++) {
                int k = (target+j)%size;
                if (getSlot(k).mayPlace(stack)) moved = moveItemStackTo(stack,k,k+1,false);
            }
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,stack);
        return before;
    }
}
