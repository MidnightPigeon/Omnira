package com.mcmagic.omnira.gametest;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.util.Unit;

/** Headless tests have no real player tracking tickets to keep their physics fixtures loaded. */
final class SableTestBody implements AutoCloseable {
    private final ServerSubLevel ship;
    SableTestBody(ServerSubLevel ship){
        this.ship=ship;
        if(!SubLevelContainer.getContainer(ship.getLevel()).addForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE))
            throw new IllegalStateException("Cannot retain physics test fixture");
    }
    @Override public void close(){
        var container=SubLevelContainer.getContainer(ship.getLevel());
        container.removeForceLoadTicket(ship,SubLevelLoadingTicketType.COMMAND_FORCED,Unit.INSTANCE);
        if(!ship.isRemoved())container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
    }
}
