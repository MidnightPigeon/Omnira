package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Optional integration through our facade; no third-party classes on the standalone path. */
@EventBusSubscriber(modid="omnira")
public final class LootrOrbEvents {
    @SubscribeEvent public static void broken(BlockEvent.BreakEvent event){
        if(event.getLevel().getBlockEntity(event.getPos()) instanceof CrystalBallBlockEntity orb
                &&LootrOrbs.manage(orb)&&!LootrOrbs.mayBreak(event.getPlayer())){
            event.setCanceled(true);
            LootrOrbs.warnBreak(event.getPlayer());
        }
    }
    @SubscribeEvent public static void sent(ChunkWatchEvent.Sent event){
        if(!net.neoforged.fml.ModList.get().isLoaded("lootr"))return;
        for(var be:event.getChunk().getBlockEntities().values())
            if(be instanceof CrystalBallBlockEntity orb)LootrOrbs.sync(orb,event.getPlayer());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){LootrOrbs.clear();}
    private LootrOrbEvents(){}
}
