package com.mcmagic.omnira.engine;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class SpacetimeEngineMenu extends AbstractContainerMenu {
    public final SpacetimeEngineController engine;
    private final ContainerData data;
    public SpacetimeEngineMenu(int id,Inventory inv,RegistryFriendlyByteBuf buf){this(id,inv,find(inv,buf),true);}
    private static SpacetimeEngineController find(Inventory inv,RegistryFriendlyByteBuf buf){
        var p=buf.readBlockPos();var be=SpacetimeEngineBlock.controller(inv.player.level(),p);
        if(be!=null)return be;be=new SpacetimeEngineController(p,SpacetimeEngineContent.BLOCK.get().defaultBlockState());be.setLevel(inv.player.level());return be;
    }
    public SpacetimeEngineMenu(int id,Inventory inv,SpacetimeEngineController be){this(id,inv,be,false);}
    private SpacetimeEngineMenu(int id,Inventory inv,SpacetimeEngineController be,boolean client){
        super(SpacetimeEngineContent.MENU.get(),id);engine=be;
        data=client?new SimpleContainerData(19):new ContainerData(){
            public int get(int index){
                if(index==18)return be.frontSide();
                if(index>=16)return index==16?be.limit()&65535:be.limit()>>>16;
                int side=index/4;return switch(index%4){case 0->be.capacity(side)&65535;case 1->be.capacity(side)>>>16;case 2->be.rpm(side);default->be.reversed(side)?1:0;};
            }
            public void set(int index,int value){}public int getCount(){return 19;}
        };addDataSlots(data);
        for(int i=0;i<3;i++)addSlot(new SlotItemHandler(be.grids,i,96+i*30,174));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,54+col*18,210+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,54+col*18,268));
    }
    public int capacity(int side){return (data.get(side*4)&65535)|((data.get(side*4+1)&65535)<<16);}
    public int displaySide(int row){return Math.floorMod(data.get(18)+row,4);}
    public int rpm(int side){return data.get(side*4+2);}
    public boolean reversed(int side){return data.get(side*4+3)!=0;}
    public int limit(){return (data.get(16)&65535)|((data.get(17)&65535)<<16);}
    @Override public boolean stillValid(Player p){return engine.complete()&&!p.isSpectator()&&stillValid(ContainerLevelAccess.create(p.level(),engine.getBlockPos()),p,SpacetimeEngineContent.BLOCK.get());}
    @Override public ItemStack quickMoveStack(Player p,int i){
        if(i<0||i>=slots.size()||!slots.get(i).hasItem())return ItemStack.EMPTY;var slot=slots.get(i);var stack=slot.getItem();var copy=stack.copy();
        if(!(i<3?moveItemStackTo(stack,3,slots.size(),true):moveItemStackTo(stack,0,3,false)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
