package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import com.mcmagic.omnira.item.ResonanceTerminalItem;
import com.mcmagic.omnira.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

/** Selects a bound node; all container operations belong to its native menu. */
public final class ResonanceMenu extends AbstractContainerMenu {
    public static final int UNLINK=12,OPEN_SHOP=13,OPEN_CONTAINER=13;
    private static final TicketType<java.util.UUID> TICKET=TicketType.create("omnira_resonance",java.util.UUID::compareTo);
    private final java.util.UUID ticket=java.util.UUID.randomUUID();
    private final Player owner;
    private final int source;
    private final ItemStack terminal;
    private final DataSlot selected=DataSlot.standalone(),kind=DataSlot.standalone();
    private final DataSlot[] statuses;
    private ServerLevel remote;
    private BlockPos position;
    private java.util.UUID identity;
    private net.minecraft.world.level.block.entity.BlockEntity destination;

    public ResonanceMenu(int id,Inventory inventory,RegistryFriendlyByteBuf data){this(id,inventory,data.readInt(),data.readVarInt());}
    public ResonanceMenu(int id,Inventory inventory,int source){this(id,inventory,source,ResonanceTerminalItem.capacity(inventory.getItem(source)));}
    private ResonanceMenu(int id,Inventory inventory,int source,int capacity){
        super(ModMenuTypes.RESONANCE.get(),id);owner=inventory.player;this.source=source;terminal=inventory.getItem(source);selected.set(-1);
        if(capacity!=4&&capacity!=6)throw new IllegalArgumentException("Invalid resonance capacity");
        statuses=new DataSlot[capacity];java.util.Arrays.setAll(statuses,i->DataSlot.standalone());
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)inventorySlot(inventory,9+row*9+col,8+col*18,157+row*18);
        for(int col=0;col<9;col++)inventorySlot(inventory,col,8+col*18,215);
        addDataSlot(selected);addDataSlot(kind);for(var status:statuses)addDataSlot(status);
    }
    private void inventorySlot(Inventory inventory,int index,int x,int y){
        addSlot(new Slot(inventory,index,x,y){
            @Override public boolean mayPickup(Player p){return index!=source;}
            @Override public boolean mayPlace(ItemStack s){return index!=source;}
        });
    }
    public int kind(){return kind.get();}
    public int capacity(){return statuses.length;}
    public int selected(){return selected.get();}
    public int status(int i){return statuses[i].get();}
    @Override public boolean stillValid(Player player){
        var tag=ResonanceTerminalItem.data(terminal);
        return player==owner&&player.isAlive()&&!player.isSpectator()&&(player.level().isClientSide||
                player.getInventory().getItem(source)==terminal&&ResonanceTerminalItem.isTerminal(terminal)
                &&tag.hasUUID("Owner")&&tag.getUUID("Owner").equals(player.getUUID()));
    }
    private void release(){
        if(remote!=null&&position!=null)remote.getChunkSource().removeRegionTicket(TICKET,new ChunkPos(position),2,ticket);
        remote=null;position=null;identity=null;destination=null;kind.set(0);
    }
    private ResonanceCoreBlockEntity core(){
        if(remote==null||position==null||!remote.hasChunkAt(position)||!stillValid(owner))return null;
        var tag=ResonanceTerminalItem.data(terminal);
        if(!(remote.getBlockEntity(position) instanceof ResonanceCoreBlockEntity core)||!core.identity.equals(identity)
                ||!tag.hasUUID("Terminal")||!tag.getUUID("Terminal").equals(core.terminal)||!core.permits(owner))return null;
        return destination!=null&&remote.getBlockEntity(position.below())!=destination?null:core;
    }
    private int viewKind(ResonanceCoreBlockEntity core){
        if(core.kind()==4)return 4;
        var pos=core.getBlockPos().below();return remote.getBlockState(pos).getMenuProvider(remote,pos)!=null?6:0;
    }
    @Override public boolean clickMenuButton(Player player,int id){
        if(player.level().isClientSide||!stillValid(player))return false;
        if(id==OPEN_CONTAINER){
            var core=core();if(core==null)return false;
            var mana=player.getData(com.mcmagic.omnira.registry.ModAttachments.MANA);
            double cost=mana.maximum()/2;
            if(!mana.canSpend(cost))return false;
            if(core.kind()!=4){
                boolean opened=player instanceof ServerPlayer serverPlayer&&com.mcmagic.omnira.storage.RemoteContainerAccess.open(serverPlayer,core,terminal,source);
                if(opened)player.setData(com.mcmagic.omnira.registry.ModAttachments.MANA,mana.spend(cost));
                return opened;
            }
            if(!(remote.getBlockEntity(position.below()) instanceof com.mcmagic.omnira.shop.MarisaOrbBlockEntity orb))return false;
            var access=new com.mcmagic.omnira.shop.MarisaRemoteAccess(core,player,terminal,source);
            if(!access.valid()){access.close();return false;}
            var merchant=orb.connect(player);merchant.remoteAccess(access);
            var previousMenu=player.containerMenu;
            merchant.openTradingScreen(player,net.minecraft.network.chat.Component.translatable("block.omnira.marisa_crystal_ball"),1);
            if(player.containerMenu==previousMenu){access.close();return false;}
            player.setData(com.mcmagic.omnira.registry.ModAttachments.MANA,mana.spend(cost));return true;
        }
        if(id==UNLINK&&selected()>=0&&selected()<capacity()){
            var core=core();if(core!=null){core.terminal=null;core.setChanged();core.refresh();}
            var tag=ResonanceTerminalItem.data(terminal);tag.remove("Link"+selected());ResonanceTerminalItem.save(terminal,tag);
            release();selected.set(-1);broadcastChanges();return true;
        }
        if(id<0||id>=capacity())return false;
        release();selected.set(id);
        var link=ResonanceTerminalItem.data(terminal).getCompound("Link"+id);
        var dimension=ResourceLocation.tryParse(link.getString("Dimension"));if(dimension==null||!link.hasUUID("Identity"))return false;
        remote=player.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));if(remote==null)return false;
        position=BlockPos.of(link.getLong("Pos"));identity=link.getUUID("Identity");
        if(!remote.getWorldBorder().isWithinBounds(position)||remote.isOutsideBuildHeight(position)){release();return false;}
        remote.getChunkSource().addRegionTicket(TICKET,new ChunkPos(position),2,ticket);remote.getChunk(position.getX()>>4,position.getZ()>>4);
        var core=core();if(core==null||core.kind()==0){release();return false;}
        destination=remote.getBlockEntity(position.below());kind.set(viewKind(core));broadcastChanges();return true;
    }
    @Override public void clicked(int index,int button,ClickType type,Player player){
        if(!stillValid(player)||type==ClickType.SWAP&&button==source)return;
        if(index>=0&&index<slots.size()&&slots.get(index).getContainerSlot()==source)return;
        super.clicked(index,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
    @Override public void broadcastChanges(){
        if(!owner.level().isClientSide){
            com.mcmagic.omnira.item.ResonanceLinks.prune(terminal,(ServerLevel)owner.level(),false);
            var tag=ResonanceTerminalItem.data(terminal);
            for(int i=0;i<capacity();i++)statuses[i].set(tag.contains("Link"+i)?tag.getCompound("Link"+i).getInt("Kind"):0);
            if(selected()>=0&&!tag.contains("Link"+selected())){release();selected.set(-1);}
            var core=core();if(core==null){if(remote!=null)release();}else kind.set(viewKind(core));
        }
        super.broadcastChanges();
    }
    @Override public void removed(Player player){release();super.removed(player);}
}
