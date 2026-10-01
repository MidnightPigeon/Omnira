package com.mcmagic.omnira.shop;

import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import com.mcmagic.omnira.item.ResonanceTerminalItem;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import java.util.UUID;

/** Owns the remote chunk lease after the terminal menu has been replaced by trading. */
public final class MarisaRemoteAccess implements AutoCloseable {
    private static final TicketType<UUID> TICKET=TicketType.create("omnira_marisa_remote",UUID::compareTo);
    private final UUID ticket=UUID.randomUUID(),identity,terminalId;
    private final ResonanceCoreBlockEntity core;
    private final ServerLevel level;
    private final Player player;
    private final ItemStack terminal;
    private final int source;
    private boolean closed;
    public MarisaRemoteAccess(ResonanceCoreBlockEntity core,Player player,ItemStack terminal,int source){
        this.core=core;this.player=player;this.terminal=terminal;this.source=source;
        level=(ServerLevel)core.getLevel();identity=core.identity;
        terminalId=ResonanceTerminalItem.data(terminal).getUUID("Terminal");
        level.getChunkSource().addRegionTicket(TICKET,new ChunkPos(core.getBlockPos()),2,ticket);
    }
    public boolean valid(){
        if(closed||!player.isAlive()||player.isSpectator()||player.getInventory().getItem(source)!=terminal
                ||!ResonanceTerminalItem.isTerminal(terminal)||core.isRemoved()
                ||level.getBlockEntity(core.getBlockPos())!=core||!identity.equals(core.identity)
                ||!terminalId.equals(core.terminal)||core.kind()!=4||!core.permits(player))return false;
        var tag=ResonanceTerminalItem.data(terminal);
        if(!tag.hasUUID("Owner")||!player.getUUID().equals(tag.getUUID("Owner"))
                ||!tag.hasUUID("Terminal")||!terminalId.equals(tag.getUUID("Terminal")))return false;
        for(int i=0;i<ResonanceTerminalItem.capacity(terminal);i++){
            var link=tag.getCompound("Link"+i);
            if(link.hasUUID("Identity")&&identity.equals(link.getUUID("Identity"))
                    &&link.getLong("Pos")==core.getBlockPos().asLong()
                    &&link.getString("Dimension").equals(level.dimension().location().toString()))return true;
        }
        return false;
    }
    @Override public void close(){
        if(closed)return;closed=true;
        level.getChunkSource().removeRegionTicket(TICKET,new ChunkPos(core.getBlockPos()),2,ticket);
    }
}
