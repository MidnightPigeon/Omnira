package com.mcmagic.omnira.compat.create.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcmagic.omnira.compat.PortFluidFace;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="com.simibubi.create.content.fluids.pump.PumpBlockEntity",remap=false)
abstract class PortPumpMixin {
    @WrapOperation(method="distributePressureTo",at=@At(value="NEW",target="net/createmod/catnip/math/BlockFace"))
    private BlockFace omnira$edge(BlockPos pos,Direction face,Operation<BlockFace> original){return new PortFluidFace(((BlockEntity)(Object)this).getLevel(),pos,face);}
    @WrapOperation(method="distributePressureTo",at=@At(value="INVOKE",target="Lnet/minecraft/core/Direction;getOpposite()Lnet/minecraft/core/Direction;",ordinal=1))
    private Direction omnira$startSide(Direction side,Operation<Direction> original,@Local(name="start") BlockFace start){return start.getOppositeFace();}
    @WrapOperation(method="distributePressureTo",at=@At(value="INVOKE",target="Lnet/minecraft/core/Direction;getOpposite()Lnet/minecraft/core/Direction;",ordinal=2))
    private Direction omnira$remoteSide(Direction side,Operation<Direction> original,@Local(name="blockFace") BlockFace edge){return edge.getOppositeFace();}
    @WrapOperation(method="distributePressureTo",at=@At(value="INVOKE",target="Lnet/createmod/catnip/math/BlockFace;getOppositeFace()Lnet/minecraft/core/Direction;"))
    private Direction omnira$localSeed(BlockFace edge,Operation<Direction> original){return edge.getFace().getOpposite();}
    @WrapOperation(method="searchForEndpointRecursively",at=@At(value="NEW",target="net/createmod/catnip/math/BlockFace",ordinal=1))
    private BlockFace omnira$recurse(BlockPos pos,Direction face,Operation<BlockFace> original){
        var level=((BlockEntity)(Object)this).getLevel();
        return new PortFluidFace(level,pos.relative(face),face.getOpposite()).getOpposite();
    }
}
