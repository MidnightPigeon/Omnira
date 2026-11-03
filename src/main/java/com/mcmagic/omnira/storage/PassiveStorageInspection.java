package com.mcmagic.omnira.storage;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.vehicle.ContainerEntity;

/** Background maintenance must not roll loot or race personal-loot conversion.
 * Check metadata before asking for capabilities or slots: even getItem/isEmpty can generate loot.
 */
public final class PassiveStorageInspection {
    private PassiveStorageInspection() {}
    /** Per-visit identity deduplication only; never retain capabilities across ticks or invalidation. */
    public static final class Pass {
        private java.util.Set<Object> seen;
        public boolean first(Object storage) {
            if(storage instanceof net.neoforged.neoforge.items.IItemHandler handler)
                storage=com.mcmagic.omnira.item.bottle.BottleStorageHandler.unwrap(handler);
            // Subclasses may expose different slot mappings; do not merge their views by owner.
            if(storage!=null && storage.getClass()==net.neoforged.neoforge.items.wrapper.InvWrapper.class)
                storage=((net.neoforged.neoforge.items.wrapper.InvWrapper)storage).getInv();
            if(storage==null)return false;
            if(seen==null)seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            return seen.add(storage);
        }
    }
    public static boolean unopened(Object storage) {
        if(storage instanceof RandomizableContainer loot && loot.getLootTable()!=null)return true;
        if(storage instanceof ContainerEntity loot && loot.getLootTable()!=null)return true;
        if(storage instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball && ball.hasPendingLoot())return true;
        if(storage instanceof CompoundContainer compound){
            var access=(com.mcmagic.omnira.mixin.CompoundContainerAccessor)compound;
            return unopened(access.omnira$first()) || unopened(access.omnira$second());
        }
        if(storage instanceof net.neoforged.neoforge.items.wrapper.InvWrapper wrapper)return unopened(wrapper.getInv());
        return false;
    }
}
