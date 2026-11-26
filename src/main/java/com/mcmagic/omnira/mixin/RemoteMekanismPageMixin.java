package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.storage.RemoteContainerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;

@Pseudo
@Mixin(targets = "mekanism.common.network.to_server.button.PacketTileButtonPress", remap = false)
abstract class RemoteMekanismPageMixin {
    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;"))
    private OptionalInt omnira$openPage(Player player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra,
                                       Operation<OptionalInt> original) {
        var lease = RemoteContainerAccess.currentMachine(player);
        return lease == null ? original.call(player, provider, extra) : lease.openMachinePage(provider, extra);
    }
}
