package com.mcmagic.omnira.forging;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.item.SpellCoreItem;
import com.mcmagic.omnira.menu.ArcaneAssemblyTableMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class AdvancedForgeMenu extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    public AdvancedForgeMenu(int id,Inventory inv,RegistryFriendlyByteBuf b){this(id,inv,new SimpleContainer(10),new SimpleContainerData(1),ContainerLevelAccess.create(inv.player.level(),b.readBlockPos()));}
    public AdvancedForgeMenu(int id,Inventory inv,AdvancedForgeBlockEntity forge){this(id,inv,forge,new ContainerData(){
        public int get(int i){return forge.working()?1:0;}public void set(int i,int v){}public int getCount(){return 1;}
    },ContainerLevelAccess.create(forge.getLevel(),forge.getBlockPos()));}
    private AdvancedForgeMenu(int id,Inventory inv,Container c,ContainerData data,ContainerLevelAccess access){
        super(ModMenuTypes.ADVANCED_FORGE.get(),id);this.inventory=c;this.data=data;this.access=access;addDataSlots(data);
        for(int i=0;i<7;i++){
            int slot=i==6?9:i;int[] point=ArcaneAssemblyTableMenu.CENTERS[i];
            addSlot(new Slot(c,slot,point[0]-8,point[1]-8){
                public int getMaxStackSize(){return 1;}
                public boolean mayPlace(ItemStack stack){return !working() && (slot!=9 || stack.getItem() instanceof SpellCoreItem);}
                public boolean mayPickup(Player p){return !working();}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,158+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,216));
    }
    public boolean working(){return data.get(0)!=0;}
    public boolean stillValid(Player p){return inventory.stillValid(p) && stillValid(access,p,ModBlocks.ADVANCED_FORGE.get());}
    public ItemStack quickMoveStack(Player p,int index){
        if(working() || index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        boolean moved=index<7?moveItemStackTo(stack,7,slots.size(),true):stack.getItem() instanceof SpellCoreItem?
                moveItemStackTo(stack,6,7,false):moveItemStackTo(stack,0,6,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
