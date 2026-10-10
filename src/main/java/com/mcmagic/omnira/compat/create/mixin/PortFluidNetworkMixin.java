package com.mcmagic.omnira.compat.create.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.compat.PortFluidFace;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="com.simibubi.create.content.fluids.FluidNetwork",remap=false)
abstract class PortFluidNetworkMixin {
    @Shadow Level world;
    @WrapOperation(method="tick",at=@At(value="NEW",target="net/createmod/catnip/math/BlockFace"))
    private BlockFace omnira$edge(BlockPos pos,Direction face,Operation<BlockFace> original){return new PortFluidFace(world,pos,face);}
}
