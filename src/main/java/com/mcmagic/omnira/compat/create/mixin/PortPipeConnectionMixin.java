package com.mcmagic.omnira.compat.create.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.compat.PortFluidFace;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="com.simibubi.create.content.fluids.PipeConnection",remap=false)
abstract class PortPipeConnectionMixin {
    @Shadow public Direction side;
    @WrapOperation(method="determineSource",at=@At(value="NEW",target="net/createmod/catnip/math/BlockFace"))
    private BlockFace omnira$edge(BlockPos pos,Direction face,Operation<BlockFace> original,Level level,BlockPos origin){return new PortFluidFace(level,pos,face);}
    @WrapOperation(method="determineSource",at=@At(value="INVOKE",target="Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;get(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;)Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;"))
    private BlockEntityBehaviour omnira$pipe(BlockGetter world,BlockPos pos,BehaviourType<?> type,Operation<BlockEntityBehaviour> original,Level level,BlockPos origin){
        var remote=PortFluidFace.remote(level,origin,side);
        return original.call(world,remote==null?pos:remote.getPos(),type);
    }
}
