package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.ManaEngineState;
import com.mcmagic.omnira.registry.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ManaEngineMenu extends AbstractContainerMenu {
    public static final int[][] CENTERS={{88,77},{88,48},{124,98},{52,98}};
    private final Container inventory;
    private final ContainerLevelAccess access;
    private final ManaEngineState engine;
    private final ContainerData data;
    public ManaEngineMenu(int id,Inventory player,RegistryFriendlyByteBuf buf) {
        this(id,player,new SimpleContainer(4),ContainerLevelAccess.create(player.player.level(),buf.readBlockPos()),null,new SimpleContainerData(10));
    }
    public ManaEngineMenu(int id,Inventory player,ManaEngineState engine) {
        this(id,player,engine.inventory,ContainerLevelAccess.create(engine.host.getLevel(),engine.host.getBlockPos()),engine,new ContainerData() {
            public int get(int i) {return switch(i) {
                case 0->engine.enabled?1:0;case 1->engine.redstoneControl?1:0;case 2->engine.clockwise?1:0;
                case 3->engine.ratedCapacity()&0xffff;case 5->engine.kinetic?1:0;
                case 7->engine.ratedCapacity()>>>16;case 8->engine.ratedEnergy()&0xffff;case 9->engine.ratedEnergy()>>>16;
                case 6->com.mcmagic.omnira.energy.EnergyIntegration.available()?1:0;default->engine.running()?1:0;
            };}
            public void set(int i,int value) {}
            public int getCount() {return 10;}
        });
    }
    private ManaEngineMenu(int id,Inventory player,Container inventory,ContainerLevelAccess access,ManaEngineState engine,ContainerData data) {
        super(ModMenuTypes.MANA_ENGINE.get(),id);
        this.inventory=inventory;this.access=access;this.engine=engine;this.data=data;addDataSlots(data);
        for(int i=0;i<4;i++) {
            final int index=i;
            addSlot(new Slot(inventory,i,CENTERS[i][0]-8,CENTERS[i][1]-8) {
                @Override public int getMaxStackSize() {return 1;}
                @Override public boolean mayPlace(ItemStack stack) {return ManaEngineState.accepts(index,stack);}
            });
        }
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(player,9+row*9+col,8+col*18,158+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(player,col,8+col*18,216));
    }
    public boolean setting(int i) {return data.get(i)!=0;}
    public int capacity() {return (data.get(3)&0xffff)|((data.get(7)&0xffff)<<16);}
    public int energyPerSecond() {return (data.get(8)&0xffff)|((data.get(9)&0xffff)<<16);}
    public boolean electrical() {return data.get(6)!=0;}
    public boolean kinetic() {return data.get(5)!=0;}
    public boolean running() {return data.get(4)!=0;}
    @Override public boolean stillValid(Player player) {return stillValid(access,player,ModBlocks.MANA_ENGINE.get());}
    @Override public boolean clickMenuButton(Player player,int id) {
        if(engine==null || id<0 || id>2 || !stillValid(player)) return false;
        engine.control(id);broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem()) return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(index<4) {if(!moveItemStackTo(stack,4,slots.size(),true)) return ItemStack.EMPTY;}
        else {
            int from=ManaEngineState.accepts(0,stack)?0:1;
            if(!ManaEngineState.accepts(from,stack) || !moveItemStackTo(stack,from,from==0?1:4,false)) return ItemStack.EMPTY;
        }
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return copy;
    }
}
