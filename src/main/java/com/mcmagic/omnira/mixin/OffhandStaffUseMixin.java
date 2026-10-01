package com.mcmagic.omnira.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

@Mixin(Minecraft.class)
public abstract class OffhandStaffUseMixin {
    @WrapOperation(method="startUseItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult omnira$prioritizeMainHand(MultiPlayerGameMode gameMode,Player player,InteractionHand hand,
                                                       Operation<InteractionResult> original) {
        // Main-hand use always resolves before any offhand staff cast.
        boolean before=hand==InteractionHand.OFF_HAND;
        if(before)com.mcmagic.omnira.client.OffhandStaffInput.castPending();
        InteractionResult result=original.call(gameMode,player,hand);
        if(!before && result==InteractionResult.PASS)
            com.mcmagic.omnira.client.OffhandStaffInput.castPending();
        return result;
    }
}
