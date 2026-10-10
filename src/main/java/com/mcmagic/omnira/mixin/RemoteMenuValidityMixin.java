package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.storage.RemoteContainerAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.server.level.ServerPlayer.class)
abstract class RemoteMenuValidityMixin {
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean omnira$valid(AbstractContainerMenu menu,Player player,Operation<Boolean> original){var valid=RemoteContainerAccess.validity(player,menu);return valid==null?original.call(menu,player):valid;}
}
