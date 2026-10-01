package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.ModDataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/** Discover changed/loaded storage once; only aggregate containers remain tracked. */
@EventBusSubscriber(modid="omnira")
public final class AggregateStorage {
    private static final java.util.Set<BlockEntity> PENDING=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private AggregateStorage(){}
    public static void changed(BlockEntity block){
        com.mcmagic.omnira.reversal.WetlandRepair.changed(block);
        if(block.getLevel()!=null && !block.getLevel().isClientSide)PENDING.add(block);
    }
    @SubscribeEvent public static void loaded(net.neoforged.neoforge.event.level.ChunkEvent.Load event){
        if(!event.getLevel().isClientSide() && event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)
            chunk.getBlockEntities().values().forEach(AggregateStorage::changed);
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){PENDING.clear();}
    public static boolean repair(ItemStack stack,long now){
        if(!UnstableAggregateItem.isAggregate(stack) || stack.getDamageValue()==0)return false;
        if(stack.getOrDefault(ModDataComponents.AGGREGATE_REPAIRED_AT,Long.MIN_VALUE)==now)return false;
        stack.setDamageValue(stack.getDamageValue()-1);stack.set(ModDataComponents.AGGREGATE_REPAIRED_AT,now);return true;
    }
    public static boolean container(Container storage,long now){
        return container(storage,now,false);
    }
    private static boolean container(Container storage,long now,boolean immune){
        boolean warped=TimeWarpStorage.container(storage,immune || storage instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball && ball.stabilized());
        if(storage instanceof Inventory || storage instanceof net.minecraft.world.inventory.CraftingContainer || storage instanceof net.minecraft.world.inventory.ResultContainer)return warped;
        boolean active=false;
        for(int i=0;i<storage.getContainerSize();i++){
            var stack=storage.getItem(i);
            if(!UnstableAggregateItem.isAggregate(stack))continue;
            var copy=stack.copy();if(repair(copy,now))storage.setItem(i,copy);
            active=true;
        }
        return active || warped;
    }
    public static boolean handler(IItemHandler storage,long now){
        return handler(storage,now,false);
    }
    public static boolean handler(IItemHandler storage,long now,boolean immune){
        if(storage==null)return false;
        storage=com.mcmagic.omnira.item.bottle.BottleStorageHandler.unwrap(storage);
        if(storage instanceof InvWrapper inv)return container(inv.getInv(),now,immune);
        boolean warped=TimeWarpStorage.handler(storage,immune);
        if(!(storage instanceof IItemHandlerModifiable mutable))return false;
        boolean active=false;
        for(int i=0;i<storage.getSlots();i++){
            var stack=storage.getStackInSlot(i);if(!UnstableAggregateItem.isAggregate(stack))continue;
            var copy=stack.copy();
            if(repair(copy,now)){
                mutable.setStackInSlot(i,copy);
            }
            active=true;
        }
        return active || warped;
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        long now=event.getServer().overworld().getGameTime();if(now%10!=0)return;
        for(var block:java.util.List.copyOf(PENDING)){
            PENDING.remove(block);var level=block.getLevel();
            if(block.isRemoved() || level==null || !level.hasChunkAt(block.getBlockPos()))continue;
            boolean immune=block instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball && ball.stabilized();
            boolean active=block instanceof Container inventory && container(inventory,now);
            active|=handler(level.getCapability(Capabilities.ItemHandler.BLOCK,block.getBlockPos(),null),now,immune);
            for(var side:net.minecraft.core.Direction.values())active|=handler(level.getCapability(Capabilities.ItemHandler.BLOCK,block.getBlockPos(),side),now,immune);
            if(active){PENDING.add(block);AggregateParticles.emit((net.minecraft.server.level.ServerLevel)level,net.minecraft.world.phys.Vec3.atCenterOf(block.getBlockPos()),true);}
        }
        for(var player:event.getServer().getPlayerList().getPlayers()){
            for(int i=0;i<player.getInventory().getContainerSize();i++)if(UnstableAggregateItem.isAggregate(player.getInventory().getItem(i))){
                AggregateParticles.emit(player.serverLevel(),player.position().add(0,.8,0),false);break;
            }
            var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Container,Boolean>());
            for(var slot:player.containerMenu.slots)if(seen.add(slot.container))container(slot.container,now);
            // The backpack itself does not repair; an actual item-container stored in it does.
            for(int i=0;i<player.getInventory().getContainerSize();i++){
                var stack=player.getInventory().getItem(i);if(!stack.isEmpty())handler(stack.getCapability(Capabilities.ItemHandler.ITEM),now,
                        com.mcmagic.omnira.item.PortableStorageRules.stabilized(stack));
            }
        }
    }
    @SubscribeEvent public static void entity(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event){
        var entity=event.getEntity();
        if(entity.level().isClientSide || entity instanceof net.minecraft.world.entity.player.Player)return;
        if(entity instanceof net.minecraft.world.entity.LivingEntity living){
            for(var stack:living.getHandSlots())if(UnstableAggregateItem.isAggregate(stack) && UnstableAggregateItem.wear(stack)){
                stack.shrink(1);SpacetimeStorm.trigger((net.minecraft.server.level.ServerLevel)entity.level(),entity.position());
            }
        }
        if(entity.tickCount%10!=0)return;
        long now=entity.level().getServer().overworld().getGameTime();
        boolean stored=entity instanceof Container inventory && container(inventory,now);
        if(!(entity instanceof net.minecraft.world.entity.LivingEntity))stored|=handler(entity.getCapability(Capabilities.ItemHandler.ENTITY,null),now,
                entity instanceof com.mcmagic.omnira.vehicle.CruiseOrbEntity orb && orb.storage.stabilized());
        if(stored)AggregateParticles.emit((net.minecraft.server.level.ServerLevel)entity.level(),entity.position().add(0,entity.getBbHeight()*.5,0),true);
    }
}
