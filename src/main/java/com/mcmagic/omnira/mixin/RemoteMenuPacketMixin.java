package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.storage.RemoteContainerAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.server.network.ServerGamePacketListenerImpl.class)
abstract class RemoteMenuPacketMixin {
    @WrapOperation(method={"handleContainerClick","handleContainerButtonClick"},at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean omnira$valid(AbstractContainerMenu menu,Player player,Operation<Boolean> original){var valid=RemoteContainerAccess.validity(player,menu);return valid==null?original.call(menu,player):valid;}
}
