package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.compat.ForgeMovement;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="dev.ryanhcode.sable.api.SubLevelAssemblyHelper",remap=false)
public abstract class ForgeSableAssemblyMixin {
    @Inject(method="gatherConnectedBlocks",at=@At("RETURN"),cancellable=true)
    private static void omnira$wholeForge(BlockPos start,ServerLevel level,int limit,SubLevelAssemblyHelper.FrontierPredicate predicate,
            CallbackInfoReturnable<SubLevelAssemblyHelper.GatherResult> callback){
        var result=callback.getReturnValue();
        if(result.assemblyState()==SubLevelAssemblyHelper.GatherResult.State.SUCCESS
                && (result.blocks().stream().anyMatch(pos->level.getBlockState(pos).is(com.mcmagic.omnira.registry.ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()))
                || net.neoforged.fml.ModList.get().isLoaded("create") && !ForgeMovement.completeSelection(level,result.blocks())))
            callback.setReturnValue(new SubLevelAssemblyHelper.GatherResult(null,result.checkedBlocks(),null,SubLevelAssemblyHelper.GatherResult.State.NO_BLOCKS));
    }
}
