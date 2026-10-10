package com.mcmagic.omnira.storage;

import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import com.mcmagic.omnira.item.ResonanceTerminalItem;
import com.mcmagic.omnira.network.RemoteContainerSnapshot;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** A native menu lease; permissions and block identity remain live for its whole lifetime. */
@EventBusSubscriber(modid="omnira")
public final class RemoteContainerAccess implements AutoCloseable {
    private static final Map<Player,RemoteContainerAccess> ACTIVE=new WeakHashMap<>();
    private static final TicketType<UUID> TICKET=TicketType.create("omnira_native_menu",UUID::compareTo);
    private final UUID ticket=UUID.randomUUID(),identity,terminalId;
    private final ResonanceCoreBlockEntity core;
    private final BlockEntity target;
    private final BlockEntity support;
    private final List<BlockEntity> members=new ArrayList<>();
    private final ServerLevel level;
    private final ServerPlayer player;
    private final ItemStack terminal;
    private final int slot;
    private AbstractContainerMenu menu;
    private boolean closed;
    private CompoundTag lastSnapshot;
    private RemoteContainerAccess(ServerPlayer player,ResonanceCoreBlockEntity core,BlockEntity target,ItemStack terminal,int slot){
        this.player=player;this.core=core;this.target=target;this.terminal=terminal;this.slot=slot;
        support=core.getLevel().getBlockEntity(core.getBlockPos().below());
        level=(ServerLevel)core.getLevel();identity=core.identity;terminalId=ResonanceTerminalItem.data(terminal).getUUID("Terminal");
        level.getChunkSource().addRegionTicket(TICKET,new ChunkPos(core.getBlockPos()),2,ticket);
    }
    public static boolean open(ServerPlayer player,ResonanceCoreBlockEntity core,ItemStack terminal,int slot){
        var level=core.getLevel();var pos=core.getBlockPos().below();var state=level.getBlockState(pos);
        MenuProvider provider=state.getMenuProvider(level,pos);
        if(provider==null)return false;
        var target=provider instanceof BlockEntity be?be:level.getBlockEntity(pos);if(target==null)return false;
        var access=new RemoteContainerAccess(player,core,target,terminal,slot);
        if(!access.valid()){access.close();return false;}
        try {
            var result=player.openMenu(new MenuProvider(){
                public net.minecraft.network.chat.Component getDisplayName(){return provider.getDisplayName();}
                public AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inventory,Player owner){
                    var nativeMenu=provider.createMenu(id,inventory,owner);if(nativeMenu==null)return null;
                    access.menu=nativeMenu;
                    if(nativeMenu instanceof net.minecraft.world.inventory.ChestMenu chest&&chest.getContainer() instanceof net.minecraft.world.CompoundContainer combined){
                        for(var direction:Direction.Plane.HORIZONTAL){
                            var adjacent=level.getBlockEntity(pos.relative(direction));
                            if(adjacent instanceof net.minecraft.world.Container container&&combined.contains(container))access.members.add(adjacent);
                        }
                    }
                    var previous=ACTIVE.put(player,access);if(previous!=null)previous.close();
                    access.sync();return nativeMenu;
                }
                public void writeClientSideData(AbstractContainerMenu menu,net.minecraft.network.RegistryFriendlyByteBuf buffer){
                    provider.writeClientSideData(menu,buffer);
                    if(buffer.writerIndex()==0&&net.minecraft.core.registries.BuiltInRegistries.MENU.getKey(menu.getType()).getNamespace().equals("omnira"))buffer.writeBlockPos(target.getBlockPos());
                }
            });
            if(result.isEmpty()){access.close();return false;}return true;
        }catch(RuntimeException ex){access.close();throw ex;}
    }
    public boolean valid(){
        if(closed||!player.isAlive()||player.isSpectator()||player.getInventory().getItem(slot)!=terminal||!ResonanceTerminalItem.isTerminal(terminal)
                ||core.isRemoved()||target.isRemoved()||level.getBlockEntity(core.getBlockPos())!=core||level.getBlockEntity(target.getBlockPos())!=target
                ||level.getBlockEntity(core.getBlockPos().below())!=support
                ||!identity.equals(core.identity)||!terminalId.equals(core.terminal)||!core.permits(player))return false;
        for(var member:members)if(member.isRemoved()||level.getBlockEntity(member.getBlockPos())!=member)return false;
        var tag=ResonanceTerminalItem.data(terminal);
        if(!tag.hasUUID("Owner")||!player.getUUID().equals(tag.getUUID("Owner"))||!tag.hasUUID("Terminal")||!terminalId.equals(tag.getUUID("Terminal")))return false;
        for(int i=0;i<ResonanceTerminalItem.capacity(terminal);i++){
            var link=tag.getCompound("Link"+i);
            if(link.hasUUID("Identity")&&identity.equals(link.getUUID("Identity"))&&link.getLong("Pos")==core.getBlockPos().asLong()
                    &&link.getString("Dimension").equals(level.dimension().location().toString()))return true;
        }
        return false;
    }
    public static Boolean validity(Player player,AbstractContainerMenu menu){var a=ACTIVE.get(player);return a!=null&&a.menu==menu?a.valid():null;}
    public static boolean permits(Player player,BlockEntity target){var a=ACTIVE.get(player);return a!=null&&a.target==target&&a.valid();}
    private void sync(){
        var data=target.getUpdateTag(level.registryAccess());data.putString("id",net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(target.getType()).toString());
        data.putInt("x",target.getBlockPos().getX());data.putInt("y",target.getBlockPos().getY());data.putInt("z",target.getBlockPos().getZ());
        var snapshot=new CompoundTag();snapshot.put("State",NbtUtils.writeBlockState(target.getBlockState()));snapshot.put("Entity",data);
        if(!snapshot.equals(lastSnapshot)){lastSnapshot=snapshot.copy();PacketDistributor.sendToPlayer(player,new RemoteContainerSnapshot(menu.containerId,snapshot));}
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){
        var a=ACTIVE.get(event.getEntity());if(a==null)return;
        if(a.player.containerMenu!=a.menu){a.close();return;}
        if(!a.valid()){a.player.closeContainer();a.close();return;}
        if(a.level.getGameTime()%5==0)a.sync();
    }
    @SubscribeEvent public static void closeMenu(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close event){
        var a=ACTIVE.get(event.getEntity());if(a!=null&&a.menu==event.getContainer())a.close();
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){var a=ACTIVE.get(event.getEntity());if(a!=null)a.close();}
    @Override public void close(){if(closed)return;closed=true;ACTIVE.remove(player,this);level.getChunkSource().removeRegionTicket(TICKET,new ChunkPos(core.getBlockPos()),2,ticket);}
}
