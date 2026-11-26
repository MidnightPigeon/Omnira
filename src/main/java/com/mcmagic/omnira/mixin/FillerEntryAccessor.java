package com.mcmagic.omnira.mixin;

import net.minecraft.world.level.storage.loot.entries.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootPoolSingletonContainer.class)
public interface FillerEntryAccessor {
    @Accessor("entry") LootPoolEntry omnira$entry();
}
