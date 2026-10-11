package com.mcmagic.omnira.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class MirrorTerrainChangesMixin {
    @Inject(method="sendBlockUpdated",at=@At("HEAD"))
    private void omnira$invalidateMirror(BlockPos pos,BlockState old,BlockState state,int flags,CallbackInfo ci){
        com.mcmagic.omnira.client.renderer.MirrorScene.changed(pos);
    }
}
