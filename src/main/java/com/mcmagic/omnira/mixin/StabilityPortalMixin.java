package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.space.anchor.StabilityField;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Permit only the actual Portal API transition, never arbitrary dimension changes. */
@Mixin(Entity.class)
abstract class StabilityPortalMixin {
    @WrapOperation(method="handlePortal",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;"))
    private Entity omnira$portal(Entity entity,DimensionTransition transition,Operation<Entity> original){
        return StabilityField.portalTravel(entity,()->original.call(entity,transition));
    }
}
