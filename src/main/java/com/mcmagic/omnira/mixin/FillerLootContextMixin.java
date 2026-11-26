package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.loot.FillerLootSource;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LootContext.class)
abstract class FillerLootContextMixin implements FillerLootSource.Context {
    @Unique private FillerLootSource omnira$draw;
    public FillerLootSource omnira$filler() { return omnira$draw; }
    public void omnira$filler(FillerLootSource draw) { omnira$draw = draw; }
}
