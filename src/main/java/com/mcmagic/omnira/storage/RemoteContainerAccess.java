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
    private static final Map<ServerPlayer,RemoteContainerAccess> ACTIVE=new IdentityHashMap<>();
    private static final Map<ServerPlayer,BlockPos> CLOSED_MACHINE = new WeakHashMap<>();
    private static final TicketType<UUID> TICKET=TicketType.create("omnira_native_menu",UUID::compareTo);
    private final UUID ticket=UUID.randomUUID(),identity,terminalId;
    private final ResonanceCoreBlockEntity core;
    private final BlockEntity target;
    private final BlockEntity support;
    private final BlockPos menuPosition;
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
        menuPosition=core.menuPosition();
        support=core.getLevel().getBlockEntity(core.getBlockPos().below());
        level=(ServerLevel)core.getLevel();identity=core.identity;terminalId=ResonanceTerminalItem.data(terminal).getUUID("Terminal");
        level.getChunkSource().addRegionTicket(TICKET,new ChunkPos(core.getBlockPos()),2,ticket);
    }
    // A block-specific provider may deliberately return null (blocked chests, incomplete structures).
    // Only use the block entity directly when the block inherits the default no-menu implementation.
    private static final ClassValue<Boolean> DEFAULT_PROVIDER=new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type){
            for(var c=type;c!=net.minecraft.world.level.block.state.BlockBehaviour.class;c=c.getSuperclass()){
                for(var m:c.getDeclaredMethods()){
                    if(MenuProvider.class.isAssignableFrom(m.getReturnType()) && Arrays.equals(m.getParameterTypes(),new Class<?>[]{
                            net.minecraft.world.level.block.state.BlockState.class,net.minecraft.world.level.Level.class,BlockPos.class}))return false;
                }
            }
            return true;
        }
    };
    public static MenuProvider provider(net.minecraft.world.level.Level level,BlockPos pos){
        var provider=level.getBlockState(pos).getMenuProvider(level,pos);
        if(provider!=null)return provider;
        var target=level.getBlockEntity(pos);
        if(target instanceof com.mcmagic.omnira.block.entity.AssemblyAccess assembly)return assembly.assembly();
        if(target instanceof MenuProvider entityMenu && DEFAULT_PROVIDER.get(target.getBlockState().getBlock().getClass()))return entityMenu;
        var backpack=com.mcmagic.omnira.compat.SophisticatedBackpackMenus.provider(target);
        if(backpack!=null)return backpack;
        var machine=com.mcmagic.omnira.compat.MekanismRemoteMenus.provider(target);
        if(machine!=null)return machine;
        if(net.neoforged.fml.ModList.get().isLoaded("create"))
            return com.mcmagic.omnira.compat.CreateRemoteToolbox.provider(target);
        return null;
    }
    public static boolean open(ServerPlayer player,ResonanceCoreBlockEntity core,ItemStack terminal,int slot){
        var level=core.getLevel();var pos=core.menuPosition();
        MenuProvider provider=provider(level,pos);
        if(provider==null)return false;
        var target=provider instanceof BlockEntity be?level.getBlockEntity(be.getBlockPos()):level.getBlockEntity(pos);if(target==null||com.mcmagic.omnira.compat.LootrOrbs.blocksAutomation(target))return false;
        return openNative(player,core,target,terminal,slot,provider,null).isPresent();
    }

    private static OptionalInt openNative(ServerPlayer player, ResonanceCoreBlockEntity core, BlockEntity target,
            ItemStack terminal, int slot, MenuProvider provider, java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> extra) {
        var access=new RemoteContainerAccess(player,core,target,terminal,slot);
        var level=access.level;
        var pos=access.menuPosition;
        if(!access.valid()){access.close();return OptionalInt.empty();}
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
                    if(extra!=null){extra.accept(buffer);return;}
                    provider.writeClientSideData(menu,buffer);
                    if(buffer.writerIndex()==0 && net.neoforged.fml.ModList.get().isLoaded("create"))
                        com.mcmagic.omnira.compat.CreateRemoteToolbox.write(provider,buffer);
                    if(buffer.writerIndex()==0&&net.minecraft.core.registries.BuiltInRegistries.MENU.getKey(menu.getType()).getNamespace().equals("omnira"))buffer.writeBlockPos(target.getBlockPos());
                }
            });
            if(result.isEmpty())access.close();
            return result;
        }catch(RuntimeException ex){access.close();throw ex;}
    }
    public static RemoteContainerAccess currentMachine(Player player) {
        if (!(player instanceof ServerPlayer server)) return null;
        var lease = ACTIVE.get(server);
        if (lease == null || lease.menu != player.containerMenu) return null;
        return com.mcmagic.omnira.compat.MekanismRemoteMenus.isMachine(lease.target) ? lease : null;
    }

    public BlockEntity machineAt(BlockPos pos) {
        return target.getBlockPos().equals(pos) && valid() ? target : null;
    }

    /** A queued GUI click may arrive after Mekanism has closed a now-private machine. */
    public static net.minecraft.world.level.Level machineSettingLevel(Player player, BlockPos pos) {
        var lease = currentMachine(player);
        if (lease != null) return lease.machineAt(pos) == null ? null : lease.level;
        if (player instanceof ServerPlayer server && player.containerMenu == player.inventoryMenu
                && pos.equals(CLOSED_MACHINE.get(server))) return null;
        return player.level();
    }

    public OptionalInt openMachinePage(MenuProvider provider, java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> extra) {
        return openNative(player, core, target, terminal, slot, provider, extra);
    }

    public boolean valid(){
        if(closed||!player.isAlive()||player.isSpectator()||player.getInventory().getItem(slot)!=terminal||!ResonanceTerminalItem.isTerminal(terminal)
                ||core.isRemoved()||target.isRemoved()||level.getBlockEntity(core.getBlockPos())!=core||level.getBlockEntity(target.getBlockPos())!=target
                ||level.getBlockEntity(core.getBlockPos().below())!=support||!menuPosition.equals(core.menuPosition())
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
    public static Boolean validity(Player player,AbstractContainerMenu menu){var a=player instanceof ServerPlayer server?ACTIVE.get(server):null;return a!=null&&a.menu==menu?a.valid():null;}
    /** Result for a positional menu check, only within the current remote session. */
    public static Boolean blockValidity(Player player,net.minecraft.world.inventory.ContainerLevelAccess access,
                                        net.minecraft.world.level.block.Block expected){
        var lease=player instanceof ServerPlayer server?ACTIVE.get(server):null;
        if(lease==null || player.containerMenu!=lease.menu)return null;
        if(!access.evaluate((level,pos)->level==lease.level && pos.equals(lease.target.getBlockPos()),false))return null;
        return lease.valid() && lease.target.getBlockState().is(expected);
    }
    public static boolean canReachMenuHost(Player player,BlockEntity host){
        var lease=player instanceof ServerPlayer server?ACTIVE.get(server):null;
        if(lease!=null && player.containerMenu==lease.menu && host==lease.target)return lease.valid();
        return player.level()==host.getLevel() && player.canInteractWithBlock(host.getBlockPos(),4);
    }
    public static boolean permits(Player player,BlockEntity target){var a=player instanceof ServerPlayer server?ACTIVE.get(server):null;return a!=null&&(a.target==target||a.members.contains(target))&&a.valid();}
    private void sync(){
        var data=target.getUpdateTag(level.registryAccess());data.putString("id",net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(target.getType()).toString());
        data.putInt("x",target.getBlockPos().getX());data.putInt("y",target.getBlockPos().getY());data.putInt("z",target.getBlockPos().getZ());
        var snapshot=new CompoundTag();snapshot.put("State",NbtUtils.writeBlockState(target.getBlockState()));snapshot.put("Entity",data);
        if(!snapshot.equals(lastSnapshot)){lastSnapshot=snapshot.copy();PacketDistributor.sendToPlayer(player,new RemoteContainerSnapshot(menu.containerId,snapshot));}
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer server))return;var a=ACTIVE.get(server);if(a==null)return;
        if(a.player.containerMenu!=a.menu){a.close();return;}
        if(!a.valid()){a.player.closeContainer();a.close();return;}
        if(a.level.getGameTime()%5==0)a.sync();
    }
    @SubscribeEvent public static void closeMenu(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close event){
        if(!(event.getEntity() instanceof ServerPlayer server))return;var a=ACTIVE.get(server);
        if(a!=null&&a.menu==event.getContainer()){
            if(com.mcmagic.omnira.compat.MekanismRemoteMenus.isMachine(a.target))CLOSED_MACHINE.put(server,a.target.getBlockPos());
            a.close();
        }
    }
    @SubscribeEvent public static void openMenu(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open event){
        if(event.getEntity() instanceof ServerPlayer server)CLOSED_MACHINE.remove(server);
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){if(!(event.getEntity() instanceof ServerPlayer server))return;var a=ACTIVE.get(server);if(a!=null)a.close();}
    @Override public void close(){if(closed)return;closed=true;ACTIVE.remove(player,this);level.getChunkSource().removeRegionTicket(TICKET,new ChunkPos(core.getBlockPos()),2,ticket);}
}
