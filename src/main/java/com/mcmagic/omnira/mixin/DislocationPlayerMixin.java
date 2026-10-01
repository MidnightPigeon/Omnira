package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mcmagic.omnira.time.TemporalDislocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class DislocationPlayerMixin {
    @ModifyExpressionValue(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;isSpectator()Z",ordinal=0))
    private boolean omnira$phaseThroughWalls(boolean spectator){return spectator||TemporalDislocation.throughWalls((Player)(Object)this);}
}
