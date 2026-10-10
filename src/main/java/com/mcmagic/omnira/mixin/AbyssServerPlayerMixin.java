package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.abyss.AbyssGravity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerPlayer.class)
abstract class AbyssServerPlayerMixin {
    @ModifyVariable(method="doCheckFallDamage",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private double omnira$fall(double y){var e=(ServerPlayer)(Object)this;int gravity=AbyssGravity.direction(e);return gravity==0?0:gravity*y;}
}
