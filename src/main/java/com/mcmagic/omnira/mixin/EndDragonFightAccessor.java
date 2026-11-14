package com.mcmagic.omnira.mixin;

import java.util.UUID;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EndDragonFight.class)
public interface EndDragonFightAccessor {
    @Accessor("origin") net.minecraft.core.BlockPos omnira$getOrigin();
    @Accessor("dragonUUID") void omnira$setDragonUUID(UUID id);
}
