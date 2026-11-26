package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.storage.RemoteContainerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.BlockGetter;

@Pseudo
@Mixin(targets = {
        "mekanism.common.network.to_server.PacketGuiInteract",
        "mekanism.common.network.to_server.PacketDropperUse",
        "mekanism.common.network.to_server.PacketRemoveModule",
        "mekanism.common.network.to_server.button.PacketTileButtonPress"
}, remap = false)
abstract class RemoteMekanismButtonMixin {
    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = "Lmekanism/common/util/WorldUtils;getTileEntity(Ljava/lang/Class;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity omnira$machine(Class<?> type, BlockGetter level, BlockPos pos, Operation<BlockEntity> original,
                                      @Local(argsOnly = true) IPayloadContext context) {
        var destination = RemoteContainerAccess.machineSettingLevel(context.player(), pos);
        return destination == null ? null : original.call(type, destination, pos);
    }
}
