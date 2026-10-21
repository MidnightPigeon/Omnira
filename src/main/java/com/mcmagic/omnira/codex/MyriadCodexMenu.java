package com.mcmagic.omnira.codex;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class MyriadCodexMenu extends AbstractContainerMenu {
    public static final String[] ERRORS={"","busy","power","heart","range","material","chunks","border","biome","invalid","limit","contents"};
    public final MyriadCodexEntity codex;
    private final ContainerData data;
    private int error;
    public MyriadCodexMenu(int id,Inventory inv,RegistryFriendlyByteBuf buf){this(id,inv,find(inv,buf.readBlockPos()),true);}
    private static MyriadCodexEntity find(Inventory inv,net.minecraft.core.BlockPos pos){
        var found=MyriadCodexEntity.from(inv.player.level().getBlockEntity(pos));if(found!=null)return found;
        var e=new MyriadCodexEntity(pos,MyriadCodexContent.BLOCK.get().defaultBlockState());e.setLevel(inv.player.level());return e;
    }
    public MyriadCodexMenu(int id,Inventory inv,MyriadCodexEntity be){this(id,inv,be,false);}
    private MyriadCodexMenu(int id,Inventory inv,MyriadCodexEntity be,boolean client){
        super(MyriadCodexContent.MENU.get(),id);codex=be;
        data=client?new SimpleContainerData(8):new ContainerData(){
            public int get(int i){return switch(i){case 0->be.mode;case 1->be.shape;case 2->be.axis;case 3->be.hollow?1:0;case 4->be.potential;case 5->be.heartPos!=null?1:0;case 6->be.busy()?1:0;default->error;};}
            public void set(int i,int v){}public int getCount(){return 8;}
        };addDataSlots(data);
        for(int i=0;i<2;i++)addSlot(new SlotItemHandler(be.materials,i,i==0?201:265,104){
            @Override public boolean mayPickup(Player p){return isActive()&&!codex.busy()&&super.mayPickup(p);}
            @Override public boolean mayPlace(ItemStack stack){return isActive()&&!codex.busy()&&super.mayPlace(stack);}
            @Override public boolean isActive(){return mode()==0;}
        });
        for(int i=0;i<2;i++){int m=i+1;addSlot(new SlotItemHandler(be.materials,0,233,i==0?80:104){
            @Override public boolean mayPickup(Player p){return isActive()&&!codex.busy()&&super.mayPickup(p);}
            @Override public boolean mayPlace(ItemStack stack){return isActive()&&!codex.busy()&&super.mayPlace(stack);}
            @Override public boolean isActive(){return mode()==m;}
        });}
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,CodexLayout.INVENTORY_X+x*18,CodexLayout.INVENTORY_Y+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,CodexLayout.INVENTORY_X+x*18,CodexLayout.HOTBAR_Y));
    }
    public int mode(){return data.get(0);}public int shape(){return data.get(1);}public int axis(){return data.get(2);}
    public boolean hollow(){return data.get(3)!=0;}public int power(){return data.get(4);}public boolean placed(){return data.get(5)!=0;}public boolean busy(){return data.get(6)!=0;}
    public String error(){return ERRORS[Math.clamp(data.get(7),0,ERRORS.length-1)];}
    @Override public boolean stillValid(Player p){return codex.live()&&p.level()==codex.getLevel()&&!p.isSpectator()&&p.canInteractWithBlock(codex.getBlockPos(),4);}
    @Override public boolean clickMenuButton(Player p,int id){
        if(!stillValid(p)||codex.busy())return false;error=0;
        if(id>=0&&id<=2){
            if(id!=0&&!codex.materials.getStackInSlot(1).isEmpty()){var stack=codex.materials.extractItem(1,1,false);if(!p.getInventory().add(stack))p.drop(stack,false);}
            codex.mode=id;
        }else if(id>=10&&id<=12)codex.shape=id-10;
        else if(id>=20&&id<=22)codex.axis=id-20;
        else if(id==30)codex.hollow=!codex.hollow;
        else if(id==40){
            String reason=codex.start((ServerPlayer)p);
            if(reason.isEmpty()){p.closeContainer();return true;}
            error=java.util.Arrays.asList(ERRORS).indexOf(reason);if(error<1)error=9;
        }else return false;
        codex.sync();broadcastChanges();return true;
    }
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(!stillValid(p)||codex.busy()||slot>=0&&slot<4&&!(codex.mode==0?slot<2:slot==codex.mode+1)||getCarried().is(MyriadCodexContent.HEART_ITEM.get()))return;
        super.clicked(slot,button,type,p);
    }
    @Override public boolean canDragTo(Slot slot){return slot.isActive()&&super.canDragTo(slot);}
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.isActive()&&super.canTakeItemForPickAll(stack,slot);}
    @Override public ItemStack quickMoveStack(Player p,int i){
        if(!stillValid(p)||codex.busy()||i<0||i>=slots.size()||!slots.get(i).hasItem())return ItemStack.EMPTY;
        var slot=slots.get(i);var stack=slot.getItem();var copy=stack.copy();
        if(i<4){if(!moveItemStackTo(stack,4,slots.size(),true))return ItemStack.EMPTY;}
        else if(!codex.materials.isItemValid(0,stack)||!(codex.mode==0?moveItemStackTo(stack,0,2,false):moveItemStackTo(stack,codex.mode+1,codex.mode+2,false)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
