package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.abyss.AbyssGravity;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class AbyssMovementValidationMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;
    @Inject(method="handleMovePlayer",at=@At("TAIL"))
    private void omnira$allowGravityMovement(ServerboundMovePlayerPacket packet,CallbackInfo ci){if(AbyssGravity.direction(player)!=1)clientIsFloating=false;}
}
