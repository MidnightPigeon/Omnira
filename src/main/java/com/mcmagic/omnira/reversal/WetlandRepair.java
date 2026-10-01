package com.mcmagic.omnira.reversal;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;

/** Track changed storage, not all blocks or every slot in the world each tick. */
@EventBusSubscriber(modid="omnira")
public final class WetlandRepair {
    private static final java.util.Set<BlockEntity> TRACKED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static void changed(BlockEntity be){if(be.getLevel()!=null&&!be.getLevel().isClientSide)TRACKED.add(be);}
    public static boolean repair(IItemHandler raw,boolean apply){
        if(raw==null)return false;var handler=com.mcmagic.omnira.item.bottle.BottleStorageHandler.unwrap(raw);boolean found=false;
        for(int i=0;i<handler.getSlots();i++){
            var s=handler.getStackInSlot(i);if(!s.is(com.mcmagic.omnira.archaeology.ArchaeologyContent.DAMAGED_FACILITY.get()))continue;
            found=true;
            if(apply&&handler instanceof IItemHandlerModifiable mutable)mutable.setStackInSlot(i,new net.minecraft.world.item.ItemStack(ReversalContent.PART.get(),s.getCount()));
        }
        return found;
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        long now=event.getServer().overworld().getGameTime();if(now%20!=0)return;
        for(var be:java.util.List.copyOf(TRACKED)){
            TRACKED.remove(be);var l=be.getLevel();if(be.isRemoved()||l==null||!l.hasChunkAt(be.getBlockPos()))continue;
            boolean apply=now>0&&now%com.mcmagic.omnira.mire.MireCycle.PERIOD==0&&l.getBiome(be.getBlockPos()).is(com.mcmagic.omnira.mire.MireCycle.BIOME);
            boolean found=be instanceof net.minecraft.world.Container c&&repair(new net.neoforged.neoforge.items.wrapper.InvWrapper(c),apply);
            found|=repair(l.getCapability(Capabilities.ItemHandler.BLOCK,be.getBlockPos(),null),apply);
            for(var side:net.minecraft.core.Direction.values())found|=repair(l.getCapability(Capabilities.ItemHandler.BLOCK,be.getBlockPos(),side),apply);
            if(found){TRACKED.add(be);if(apply)be.setChanged();}
        }
    }
    @SubscribeEvent public static void entity(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event){
        var e=event.getEntity();long now=e.level().getGameTime();
        if(e.level().isClientSide||e instanceof net.minecraft.world.entity.player.Player||now==0||now%6000!=0||!e.level().getBiome(e.blockPosition()).is(com.mcmagic.omnira.mire.MireCycle.BIOME))return;
        if(e instanceof net.minecraft.world.Container c)repair(new net.neoforged.neoforge.items.wrapper.InvWrapper(c),true);
        repair(e.getCapability(Capabilities.ItemHandler.ENTITY,null),true);
    }
    @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event){TRACKED.clear();}
    private WetlandRepair(){}
}
