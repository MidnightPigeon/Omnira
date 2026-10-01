package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.spacetime.SpacetimeSky;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public abstract class SpacetimeChunkLightMixin {
    @Inject(method = "read", at = @At("HEAD"))
    private static void omnira$relightLegacyChunks(ServerLevel level, PoiManager poi, RegionStorageInfo info,
                                                   ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> ci) {
        if (SpacetimeSky.applies(level)) SpacetimeSky.prepareChunkLighting(tag);
    }

    @Inject(method = "write", at = @At("RETURN"))
    private static void omnira$markSkyLighting(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> ci) {
        if (SpacetimeSky.applies(level)) ci.getReturnValue().putInt("omnira:sky_lighting_version", 1);
    }
}
