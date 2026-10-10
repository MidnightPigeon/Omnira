package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.storage.RemoteContainerAccess;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
interface RemoteContainerValidityMixin {
    @Inject(method="stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;F)Z",at=@At("HEAD"),cancellable=true)
    private static void omnira$remote(BlockEntity entity,Player player,float distance,CallbackInfoReturnable<Boolean> ci){if(RemoteContainerAccess.permits(player,entity))ci.setReturnValue(true);}
}
