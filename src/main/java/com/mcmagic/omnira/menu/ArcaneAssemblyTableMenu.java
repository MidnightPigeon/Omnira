package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity;
import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.mana.ManaCosts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ArcaneAssemblyTableMenu extends AbstractContainerMenu {
    // Clockwise from upper-right; the center serves as the fixture, then the output.
    public static final int[][] CENTERS={{105,48},{122,78},{105,108},{71,108},{54,78},{71,48},{88,78}};
    private final Container inventory;
    private final Player owner;
    private final ContainerLevelAccess access;
    private final ArcaneAssemblyTableBlockEntity table;
    private final ContainerData data;
    public ArcaneAssemblyTableMenu(int id,Inventory player,RegistryFriendlyByteBuf buf) {
        this(id,player,new SimpleContainer(7),ContainerLevelAccess.create(player.player.level(),buf.readBlockPos()),null,new SimpleContainerData(4));
    }
    public ArcaneAssemblyTableMenu(int id,Inventory player,ArcaneAssemblyTableBlockEntity table) {
        this(id,player,table,ContainerLevelAccess.create(table.getLevel(),table.getBlockPos()),table,new ContainerData() {
            @Override public int get(int i) {return i==0?table.progress():i==1?(table.finished()?1:0):i==2?table.cooldown():table.mechanicalPowered()?1:0;}
            @Override public void set(int i,int value) {}
            @Override public int getCount() {return 4;}
        });
    }
    private ArcaneAssemblyTableMenu(int id,Inventory player,Container container,ContainerLevelAccess access,ArcaneAssemblyTableBlockEntity table,ContainerData data) {
        super(ModMenuTypes.ARCANE_ASSEMBLY_TABLE.get(),id);
        this.inventory=container;this.owner=player.player;this.access=access;this.table=table;this.data=data;
        container.startOpen(owner);addDataSlots(data);
        for(int i=0;i<7;i++) {
            final int index=i;
            addSlot(new Slot(container,i,CENTERS[i][0]-8,CENTERS[i][1]-8) {
                @Override public int getMaxStackSize() {return 1;}
                @Override public boolean mayPlace(ItemStack stack) {return !finished();}
            });
        }
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(player,9+row*9+col,8+col*18,158+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(player,col,8+col*18,216));
    }
    public int progress() {return data.get(0);}
    public int cooldown() {return data.get(2);}
    public boolean finished() {return data.get(1)!=0;}
    public boolean mechanicalPowered() {return data.get(3)!=0;}
    public double manaCost() {return mechanicalPowered()?0:ManaCosts.cost(owner,ArcaneAssemblyTableBlockEntity.BASE_COST);}
    public boolean canStrike() {
        return !mechanicalPowered() && cooldown()==0 && !finished() && ManaCosts.canSpend(owner,ArcaneAssemblyTableBlockEntity.BASE_COST)
                && com.mcmagic.omnira.recipe.AssemblyWork.find(owner.level(),inventory)!=null;
    }
    @Override public boolean stillValid(Player player) {
        return player==owner && inventory.stillValid(player) && stillValid(access,player,ModBlocks.ARCANE_ASSEMBLY_TABLE.get());
    }
    @Override public boolean clickMenuButton(Player player,int id) {
        return id==0 && player==owner && table!=null && player instanceof ServerPlayer server && stillValid(player) && table.strike(server);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem()) return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(index<7) {
            if(!moveItemStackTo(stack,7,slots.size(),true)) return ItemStack.EMPTY;
        } else {
            if(finished()) return ItemStack.EMPTY;
            var part=com.mcmagic.omnira.item.staff.StaffPart.of(stack);
            int target=part==null?-1:part.role().assemblySlot();
            if(target<0) {
                if(!inventory.getItem(6).isEmpty()) {
                    for(int i=0;i<6;i++) if(inventory.getItem(i).isEmpty()) {target=i;break;}
                } else target=6;
            }
            if(target<0 || !moveItemStackTo(stack,target,target+1,false)) return ItemStack.EMPTY;
        }
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return copy;
    }
    @Override public void removed(Player player) {super.removed(player);inventory.stopOpen(player);}
}
