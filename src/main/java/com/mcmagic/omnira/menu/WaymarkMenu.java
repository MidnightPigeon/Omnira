package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.WaymarkBlockEntity;
import com.mcmagic.omnira.item.RecallCrystalItem;
import com.mcmagic.omnira.registry.ModMenuTypes;
import com.mcmagic.omnira.travel.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class WaymarkMenu extends AbstractContainerMenu {
    public final boolean editing;
    public final InteractionHand hand;
    public final List<WaymarkDirectory.Entry> entries;
    public final Set<Integer> removedEntries=new HashSet<>();
    public final UUID amber;
    private final ItemStack bound;
    private final Player owner;
    public WaymarkMenu(int id,Inventory inventory,RegistryFriendlyByteBuf data) {
        this(id,inventory,data.readBoolean(),data.readEnum(InteractionHand.class),readEntries(data),data.readNullable(b->b.readUUID()));
    }
    private WaymarkMenu(int id,Inventory inventory,boolean editing,InteractionHand hand,List<WaymarkDirectory.Entry> entries,UUID amber) {
        super(ModMenuTypes.WAYMARK.get(),id);this.amber=amber;this.editing=editing;this.hand=hand;this.entries=List.copyOf(entries);
        owner=inventory.player;bound=owner.getItemInHand(hand);
    }
    private static List<WaymarkDirectory.Entry> readEntries(RegistryFriendlyByteBuf buf) {
        int count=buf.readVarInt();if(count<0 || count>1024) throw new IllegalArgumentException("Invalid waymark count");
        var entries=new ArrayList<WaymarkDirectory.Entry>();
        for(int i=0;i<count;i++) entries.add(new WaymarkDirectory.Entry(buf.readUUID(),buf.readResourceLocation(),buf.readBlockPos(),buf.readUtf(32),buf.readNullable(net.minecraft.network.FriendlyByteBuf::readResourceLocation),buf.readVarInt()));
        return entries;
    }
    private static void open(ServerPlayer player,boolean editing,InteractionHand hand,List<WaymarkDirectory.Entry> entries) {
        var directory = WaymarkDirectory.get(player.serverLevel());
        var located = entries.stream().map(entry -> directory.withLocation(player.serverLevel(),entry)).toList();
        var grave=com.mcmagic.omnira.spacetime.AmberDirectory.get(player.serverLevel()).grave(player.getUUID());
        UUID amber=grave==null?null:grave.entity();
        RecallTravel.cancel(player,null);
        player.openMenu(new SimpleMenuProvider((id,inventory,p)->new WaymarkMenu(id,inventory,editing,hand,located,amber),
                Component.translatable(editing?"block.omnira.spacetime_waymark":"item.omnira.recall_crystal")),buf->writeOpenData(buf,editing,hand,located,amber));
    }
    public static void writeOpenData(RegistryFriendlyByteBuf buf,boolean editing,InteractionHand hand,List<WaymarkDirectory.Entry> located,UUID amber) {
            buf.writeBoolean(editing);buf.writeEnum(hand);buf.writeVarInt(located.size());
            for(var entry:located) {buf.writeUUID(entry.id());buf.writeResourceLocation(entry.dimension());buf.writeBlockPos(entry.pos());buf.writeUtf(entry.name(),32);buf.writeNullable(entry.biome(),net.minecraft.network.FriendlyByteBuf::writeResourceLocation);buf.writeVarInt(entry.lostNumber());}
            buf.writeNullable(amber,(b,id)->b.writeUUID(id));
    }
    public static void openRecall(ServerPlayer player,InteractionHand hand) {
        open(player,false,hand,WaymarkDirectory.get(player.serverLevel()).list(player.getUUID()).stream().limit(1024).toList());
    }
    public static void openEdit(ServerPlayer player,WaymarkBlockEntity mark) {open(player,true,InteractionHand.MAIN_HAND,List.of(mark.entry()));}
    @Override public boolean stillValid(Player player) {
        if(player!=owner || !player.isAlive() || player.isSpectator()) return false;
        if(player.level().isClientSide) return true;
        if(!editing) return player.getItemInHand(hand)==bound && bound.getItem() instanceof RecallCrystalItem;
        if(entries.size()!=1) return false;
        var entry=entries.getFirst();
        return player.level().dimension().location().equals(entry.dimension()) && player.distanceToSqr(entry.pos().getCenter())<=64
                && player.level().getBlockEntity(entry.pos()) instanceof WaymarkBlockEntity mark && mark.id().equals(entry.id());
    }
    public void rename(ServerPlayer player,String name) {
        if(editing && player.mayBuild() && stillValid(player) && player.level().getBlockEntity(entries.getFirst().pos()) instanceof WaymarkBlockEntity mark) {
            mark.rename(name);player.closeContainer();
        }
    }
    @Override public boolean clickMenuButton(Player player,int index) {
        if(editing || !(player instanceof ServerPlayer server) || !stillValid(player)) return false;
        // Negative row commands remove a personal bookmark, never the world destination.
        if(index<0) {
            if(index < -entries.size()) return false;
            var target=entries.get(-index-1).id();
            if(com.mcmagic.omnira.spacetime.AmberDirectory.get(server.serverLevel()).owns(server.getUUID(),target))return false;
            WaymarkDirectory.get(server.serverLevel()).forget(server.getUUID(),target);
            removedEntries.add(-index-1);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server,new com.mcmagic.omnira.network.SelectWaymarkPayload(containerId,index));
            return true;
        }
        if(index>=entries.size() || removedEntries.contains(index)) return false;
        if(RecallTravel.start(server,hand,entries.get(index).id())) {server.closeContainer();return true;}
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {return ItemStack.EMPTY;}
}
