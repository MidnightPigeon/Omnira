package com.mcmagic.omnira.compat.create.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.compat.PortFluidFace;
import com.llamalad7.mixinextras.sugar.Local;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="com.simibubi.create.content.fluids.FluidNetwork",remap=false)
abstract class PortFluidNetworkMixin {
    @Shadow Level world;
    @Shadow BlockFace start;
    @Shadow java.util.Map<BlockPos,java.lang.ref.WeakReference<com.simibubi.create.content.fluids.FluidTransportBehaviour>> cache;
    @Unique private final java.util.Map<BlockFace,java.util.List<com.mcmagic.omnira.space.SpacetimePortState>> omnira$routes=new java.util.HashMap<>();
    @org.spongepowered.asm.mixin.injection.Inject(method="reset",at=@At("HEAD"))
    private void omnira$reset(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){omnira$routes.clear();}
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private int omnira$delivered(net.neoforged.neoforge.fluids.capability.IFluidHandler handler,net.neoforged.neoforge.fluids.FluidStack fluid,
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action,Operation<Integer> original,
            @Local(name="pair") net.createmod.catnip.data.Pair<BlockFace,com.simibubi.create.content.fluids.FlowSource> target){
        int amount=original.call(handler,fluid,action);
        if(amount>0&&action.execute()){
            var face=target.getFirst();var ports=omnira$routes.get(face);
            if(ports==null){ports=com.mcmagic.omnira.compat.PortFluidProgress.route(world,start,face,fluid,cache);if(ports!=null)omnira$routes.put(face,ports);}
            if(ports!=null)for(var port:ports)if(port.other()!=null)com.mcmagic.omnira.progression.BranchProgress.port(port.host,amount);
        }return amount;
    }
    @WrapOperation(method="tick",at=@At(value="NEW",target="net/createmod/catnip/math/BlockFace"))
    private BlockFace omnira$edge(BlockPos pos,Direction face,Operation<BlockFace> original){return new PortFluidFace(world,pos,face);}
}
