package com.mcmagic.omnira.world.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface MagicDimensionAccess {
    ResourceKey<Level> magicMod$targetDimension();
}
