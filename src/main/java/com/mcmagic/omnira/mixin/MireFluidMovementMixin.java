package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.mire.MireFluidEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Entity.class)
abstract class MireFluidMovementMixin {
    // Scale actual movement once, including swimming, without stacking walk/swim attributes.
    @ModifyVariable(method="move",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Vec3 omnira$mireMovement(Vec3 delta){return MireFluidEffects.movement((Entity)(Object)this,delta);}
}
