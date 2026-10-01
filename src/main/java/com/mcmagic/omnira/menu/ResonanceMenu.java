package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.ResonanceTerminalItem;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/** Remote slots are snapshots, never real inventory slots: every mutation goes through a paid transaction. */
public final class ResonanceMenu extends AbstractContainerMenu {
    public static final int PREVIOUS=10,NEXT=11,UNLINK=12,OPEN_SHOP=13;
    private static final TicketType<java.util.UUID> TICKET=TicketType.create("omnira_resonance",java.util.UUID::compareTo);
    private final java.util.UUID ticket=java.util.UUID.randomUUID();
    private final Player owner;
    private final int source;
    private final ItemStack terminal;
    private final SimpleContainer display=new SimpleContainer(27),port=new SimpleContainer(1) {
        @Override public void setChanged(){super.setChanged();if(!portInternal){cancelFluid();portDone=false;fluidDirectionSet=false;}}
    };
    private final DataSlot selected=DataSlot.standalone(),kind=DataSlot.standalone(),page=DataSlot.standalone(),pages=DataSlot.standalone(),progress=DataSlot.standalone();
    private final DataSlot[] statuses;
    private final DataSlot[] fluidInfo={DataSlot.standalone(),DataSlot.standalone(),DataSlot.standalone(),DataSlot.standalone(),DataSlot.standalone()};
    private ServerLevel remote;
    private BlockPos position;
    private java.util.UUID identity;
    private BlockEntityIdentity destination;
    private long lastTick=Long.MIN_VALUE;
    private boolean portInternal,portDone,fluidInput,fluidDirectionSet;
    private int reserved,fluidAmount;
    private ItemStack fluidSnapshot=ItemStack.EMPTY;
    private record BlockEntityIdentity(net.minecraft.world.level.block.entity.BlockEntity entity) {}
    public ResonanceMenu(int id,Inventory inventory,RegistryFriendlyByteBuf data){this(id,inventory,data.readInt(),data.readVarInt());}
    public ResonanceMenu(int id,Inventory inventory,int source){this(id,inventory,source,ResonanceTerminalItem.capacity(inventory.getItem(source)));}
    private ResonanceMenu(int id,Inventory inventory,int source,int capacity) {
        super(ModMenuTypes.RESONANCE.get(),id);owner=inventory.player;this.source=source;terminal=inventory.getItem(source);selected.set(-1);
        if(capacity!=4&&capacity!=6)throw new IllegalArgumentException("Invalid resonance capacity");
        statuses=new DataSlot[capacity];java.util.Arrays.setAll(statuses,i->DataSlot.standalone());
        for(int i=0;i<27;i++)addSlot(new Slot(display,i,8+(i%9)*18,70+(i/9)*18) {
            @Override public boolean mayPickup(Player p){return false;}
            @Override public boolean mayPlace(ItemStack s){return false;}
            @Override public boolean isActive(){return kind()==2;}
        });
        addSlot(new Slot(port,0,80,88) {
            @Override public int getMaxStackSize(){return 1;}
            @Override public int getMaxStackSize(ItemStack s){return 1;}
            @Override public boolean mayPlace(ItemStack s){return s.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM)!=null;}
            @Override public boolean isActive(){return kind()==3;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)inventorySlot(inventory,9+row*9+col,8+col*18,157+row*18);
        for(int col=0;col<9;col++)inventorySlot(inventory,col,8+col*18,215);
        addDataSlot(selected);addDataSlot(kind);addDataSlot(page);addDataSlot(pages);addDataSlot(progress);
        for(var status:statuses)addDataSlot(status);
        for(var value:fluidInfo)addDataSlot(value);
    }
    private void inventorySlot(Inventory inventory,int index,int x,int y) {
        addSlot(new Slot(inventory,index,x,y) {
            @Override public boolean mayPickup(Player p){return index!=source;}
            @Override public boolean mayPlace(ItemStack s){return index!=source;}
        });
    }
    public int kind(){return kind.get();}
    public int capacity(){return statuses.length;}
    public int selected(){return selected.get();}
    public int page(){return page.get();}
    public int pages(){return pages.get();}
    public int status(int i){return statuses[i].get();}
    public int progress(){return progress.get();}
    public int fluidVolume(){return (fluidInfo[1].get()&65535)|((fluidInfo[2].get()&65535)<<16);}
    public int fluidCapacity(){return (fluidInfo[3].get()&65535)|((fluidInfo[4].get()&65535)<<16);}
    public net.minecraft.network.chat.Component fluidName(){
        var fluid=net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(fluidInfo[0].get());
        return fluid==null || fluid==net.minecraft.world.level.material.Fluids.EMPTY?net.minecraft.network.chat.Component.translatable("gui.omnira.resonance.no_fluid"):new FluidStack(fluid,1).getHoverName();
    }
    @Override public boolean stillValid(Player player) {
        var tag=ResonanceTerminalItem.data(terminal);
        return player==owner && player.isAlive() && !player.isSpectator() && (player.level().isClientSide ||
                player.getInventory().getItem(source)==terminal && ResonanceTerminalItem.isTerminal(terminal)
                        && tag.hasUUID("Owner") && tag.getUUID("Owner").equals(player.getUUID()));
    }
    private void release() {
        cancelFluid();destination=null;fluidDirectionSet=false;
        if(remote!=null && position!=null)remote.getChunkSource().removeRegionTicket(TICKET,new ChunkPos(position),2,ticket);
        remote=null;position=null;identity=null;kind.set(0);display.clearContent();
    }
    private ResonanceCoreBlockEntity core() {
        if(remote==null || position==null || !remote.hasChunkAt(position) || !stillValid(owner))return null;
        var tag=ResonanceTerminalItem.data(terminal);
        if(!(remote.getBlockEntity(position) instanceof ResonanceCoreBlockEntity core) || !core.identity.equals(identity)
                || !tag.hasUUID("Terminal") || !tag.getUUID("Terminal").equals(core.terminal) || !core.permits(owner))return null;
        var be=remote.getBlockEntity(position.below());
        if(destination!=null && destination.entity()!=be)return null;
        return core;
    }
    @Override public boolean clickMenuButton(Player player,int id) {
        if(player.level().isClientSide || !stillValid(player))return false;
        if(id==OPEN_SHOP){
            var core=core();
            if(core==null||core.kind()!=4||!(remote.getBlockEntity(position.below()) instanceof com.mcmagic.omnira.shop.MarisaOrbBlockEntity orb))return false;
            var access=new com.mcmagic.omnira.shop.MarisaRemoteAccess(core,player,terminal,source);
            if(!access.valid()){access.close();return false;}
            var merchant=orb.connect(player);merchant.remoteAccess(access);
            merchant.openTradingScreen(player,net.minecraft.network.chat.Component.translatable("block.omnira.marisa_crystal_ball"),1);
            return true;
        }
        if(id==UNLINK && selected()>=0 && selected()<capacity()) {
            var tag=ResonanceTerminalItem.data(terminal);var link=tag.getCompound("Link"+selected());
            var dim=ResourceLocation.tryParse(link.getString("Dimension"));
            var level=dim==null?null:player.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dim));
            var pos=BlockPos.of(link.getLong("Pos"));
            if(level!=null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity core
                    && link.hasUUID("Identity") && link.getUUID("Identity").equals(core.identity)
                    && tag.hasUUID("Terminal") && tag.getUUID("Terminal").equals(core.terminal) && core.permits(player)) {
                core.terminal=null;core.setChanged();core.refresh();
            }
            tag.remove("Link"+selected());ResonanceTerminalItem.save(terminal,tag);release();selected.set(-1);broadcastChanges();return true;
        }
        if(id==PREVIOUS || id==NEXT) {page.set(Math.clamp(page.get()+(id==PREVIOUS?-1:1),0,Math.max(0,pages.get()-1)));return true;}
        if(id<0 || id>=capacity())return false;
        release();selected.set(id);page.set(0);portDone=false;
        var link=ResonanceTerminalItem.data(terminal).getCompound("Link"+id);
        var dimension=ResourceLocation.tryParse(link.getString("Dimension"));
        if(dimension==null || !link.hasUUID("Identity"))return false;
        remote=player.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
        if(remote==null)return false;
        position=BlockPos.of(link.getLong("Pos"));identity=link.getUUID("Identity");
        if(!remote.getWorldBorder().isWithinBounds(position) || remote.isOutsideBuildHeight(position)){release();return false;}
        remote.getChunkSource().addRegionTicket(TICKET,new ChunkPos(position),2,ticket);
        remote.getChunk(position.getX()>>4,position.getZ()>>4);
        var core=core();if(core==null || core.kind()==0){release();return false;}
        destination=new BlockEntityIdentity(remote.getBlockEntity(position.below()));kind.set(core.kind());broadcastChanges();return true;
    }
    private int affordable(){return (int)Math.min(Integer.MAX_VALUE,owner.getData(ModAttachments.MANA).current()/100);}
    private boolean pay(double amount) {
        var mana=owner.getData(ModAttachments.MANA);if(!mana.canSpend(amount))return false;
        owner.setData(ModAttachments.MANA,mana.spend(amount));return true;
    }
    private void refund(double amount) {
        if(amount>0){var mana=owner.getData(ModAttachments.MANA);owner.setData(ModAttachments.MANA,mana.withCurrent(mana.current()+amount));}
    }
    private ItemStack extract(IItemHandler handler,int slot,int count) {
        int limit=Math.min(count,affordable());if(limit<=0)return ItemStack.EMPTY;
        var simulated=handler.extractItem(slot,limit,true);if(simulated.isEmpty() || !pay(simulated.getCount()*100D))return ItemStack.EMPTY;
        var actual=handler.extractItem(slot,simulated.getCount(),false);
        refund((simulated.getCount()-actual.getCount())*100D);return actual;
    }
    private int insert(IItemHandler handler,int slot,ItemStack stack,int count) {
        int limit=Math.min(Math.min(count,stack.getCount()),affordable());if(limit<=0)return 0;
        var offered=stack.copyWithCount(limit);int accepted=limit-handler.insertItem(slot,offered,true).getCount();
        if(accepted<=0 || !pay(accepted*100D))return 0;
        int actual=accepted-handler.insertItem(slot,stack.copyWithCount(accepted),false).getCount();
        refund((accepted-actual)*100D);stack.shrink(actual);return actual;
    }
    @Override public void clicked(int index,int button,ClickType type,Player player) {
        if(!stillValid(player))return;
        if(type==ClickType.SWAP && button==source)return;
        if(index>=28 && index<slots.size() && slots.get(index).getContainerSlot()==source)return;
        if(index>=0 && index<27) {
            if(player.level().isClientSide || kind()!=2)return;
            var core=core();if(core==null){release();return;}var handler=core.items();int real=page()*27+index;
            if(handler==null || real>=handler.getSlots())return;
            if(type==ClickType.QUICK_MOVE){quickMoveStack(player,index);return;}
            if(type!=ClickType.PICKUP || button<0 || button>1)return;
            var cursor=getCarried();
            if(cursor.isEmpty()) {
                var stored=handler.getStackInSlot(real);int take=Math.min(stored.getCount(),stored.getMaxStackSize());
                setCarried(extract(handler,real,button==1?(take+1)/2:take));
            } else insert(handler,real,cursor,button==1?1:cursor.getCount());
            broadcastChanges();return;
        }
        if(index==27 && kind()!=3)return;
        super.clicked(index,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(player.level().isClientSide || !stillValid(player) || index<0 || index>=slots.size())return ItemStack.EMPTY;
        if(index<27) {
            var core=core();var handler=core==null?null:core.items();int real=page()*27+index;
            if(kind()!=2 || handler==null || real>=handler.getSlots())return ItemStack.EMPTY;
            var stored=handler.getStackInSlot(real);if(stored.isEmpty())return ItemStack.EMPTY;
            int space=0;
            for(int i=28;i<slots.size();i++){var slot=slots.get(i);if(!slot.mayPlace(stored))continue;var other=slot.getItem();if(other.isEmpty() || ItemStack.isSameItemSameComponents(other,stored))space+=Math.max(0,slot.getMaxStackSize(stored)-other.getCount());}
            var taken=extract(handler,real,Math.min(stored.getMaxStackSize(),space));var original=taken.copy();
            if(!taken.isEmpty()){moveItemStackTo(taken,28,slots.size(),true);if(!taken.isEmpty())player.drop(taken,false);}
            broadcastChanges();return original;
        }
        var slot=slots.get(index);if(!slot.hasItem() || !slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index==27){if(!moveItemStackTo(stack,28,slots.size(),true))return ItemStack.EMPTY;}
        else if(kind()==3){if(!moveItemStackTo(stack,27,28,false))return ItemStack.EMPTY;}
        else if(kind()==2) {
            var core=core();var handler=core==null?null:core.items();if(handler==null)return ItemStack.EMPTY;
            for(int i=0;i<handler.getSlots() && !stack.isEmpty() && affordable()>0;i++)insert(handler,i,stack,stack.getCount());
        } else return ItemStack.EMPTY;
        if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
    private static int volume(ItemStack stack) {
        var h=stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);if(h==null)return 0;
        int total=0;for(int i=0;i<h.getTanks();i++)total+=h.getFluidInTank(i).getAmount();return total;
    }
    private void cancelFluid(){reserved=0;fluidAmount=0;fluidSnapshot=ItemStack.EMPTY;if(progress!=null)progress.set(0);}
    private void fluidTick(ResonanceCoreBlockEntity core) {
        var stack=port.getItem(0);if(stack.isEmpty() || portDone)return;
        IFluidHandler handler=remote.getBlockEntity(position.below()) instanceof LiquidCrystalBallBlockEntity ball?ball.tank:core.fluids();
        if(handler==null)return;
        if(fluidAmount==0) {
            if(!fluidDirectionSet){fluidInput=volume(stack)>0;fluidDirectionSet=true;}
            var preview=fluidInput?FluidUtil.tryEmptyContainer(stack,handler,1000,null,false):FluidUtil.tryFillContainer(stack,handler,1000,null,false);
            if(!preview.isSuccess())return;
            int amount=Math.abs(volume(stack)-volume(preview.getResult()));
            if(amount<=0 || !pay(amount*.2))return;
            fluidAmount=amount;reserved=0;fluidSnapshot=stack.copy();
        }
        if(!ItemStack.matches(stack,fluidSnapshot)){cancelFluid();return;}
        int step=Math.min(25,fluidAmount-reserved);
        if(remote.getBlockEntity(position.below()) instanceof LiquidCrystalBallBlockEntity ball)step=ball.reserveRemoteTransfer(step);
        reserved+=step;progress.set(reserved*100/Math.max(1,fluidAmount));
        if(reserved<fluidAmount)return;
        var result=fluidInput?FluidUtil.tryEmptyContainer(stack,handler,fluidAmount,null,true):FluidUtil.tryFillContainer(stack,handler,fluidAmount,null,true);
        portInternal=true;
        try {if(result.isSuccess())port.setItem(0,result.getResult());}finally{portInternal=false;}
        // Prepaid mana is intentionally not refunded on cancellation or target rejection.
        cancelFluid();
        var next=fluidInput?FluidUtil.tryEmptyContainer(port.getItem(0),handler,1000,null,false):FluidUtil.tryFillContainer(port.getItem(0),handler,1000,null,false);
        portDone=!result.isSuccess() || !next.isSuccess();
    }
    @Override public void broadcastChanges() {
        if(!owner.level().isClientSide) {
            com.mcmagic.omnira.item.ResonanceLinks.prune(terminal,(ServerLevel)owner.level(),false);
            var tag=ResonanceTerminalItem.data(terminal);
            for(int i=0;i<capacity();i++)statuses[i].set(tag.contains("Link"+i)?tag.getCompound("Link"+i).getInt("Kind"):0);
            if(selected()>=0&&!tag.contains("Link"+selected())){release();selected.set(-1);}
            var core=core();
            if(core==null){if(remote!=null)release();}
            else {
                if(core.kind()!=kind()){release();super.broadcastChanges();return;}
                if(kind()==3) {
                    var fluids=core.fluids();int index=0;
                    if(fluids!=null && fluids.getTanks()>0) {
                        for(int i=0;i<fluids.getTanks();i++)if(!fluids.getFluidInTank(i).isEmpty()){index=i;break;}
                        var fluid=fluids.getFluidInTank(index);int amount=fluid.getAmount(),capacity=fluids.getTankCapacity(index);
                        fluidInfo[0].set(net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluid.getFluid()));
                        fluidInfo[1].set(amount&65535);fluidInfo[2].set(amount>>>16);fluidInfo[3].set(capacity&65535);fluidInfo[4].set(capacity>>>16);
                    }
                }
                var items=core.items();
                if(kind()==2 && items!=null) {
                    pages.set((items.getSlots()+26)/27);page.set(Math.clamp(page.get(),0,Math.max(0,pages.get()-1)));
                    for(int i=0;i<27;i++){int real=page()*27+i;var value=real<items.getSlots()?items.getStackInSlot(real):ItemStack.EMPTY;display.setItem(i,value.copy());}
                }
                long time=owner.level().getGameTime();if(time!=lastTick){lastTick=time;if(kind()==3)fluidTick(core);}
            }
        }
        super.broadcastChanges();
    }
    @Override public void removed(Player player){release();super.removed(player);if(!player.level().isClientSide)clearContainer(player,port);}
}
