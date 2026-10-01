package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.compat.ForgeMovement;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="dev.simulated_team.simulated.util.assembly.SimAssemblyContraption",remap=false)
public abstract class ForgeSimAssemblyMixin {
    @Shadow public abstract Collection<BlockPos> getBlocks();
    @Inject(method="movementAllowed",at=@At("HEAD"),cancellable=true)
    private void omnira$allowWholeUnbreakableForge(BlockState state,Level level,BlockPos pos,CallbackInfoReturnable<Boolean> callback){
        if(state.is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get())){callback.setReturnValue(false);return;}
        if(state.is(ModBlocks.ADVANCED_FORGE.get()))callback.setReturnValue(ForgeMovement.fullyGlued(level,pos,state));
    }
    @Inject(method="searchMovedStructure",at=@At("RETURN"),cancellable=true)
    private void omnira$rejectFragments(Level level,BlockPos start,CallbackInfoReturnable<Boolean> callback){
        if(callback.getReturnValueZ() && !ForgeMovement.completeSelection(level,new HashSet<>(getBlocks())))callback.setReturnValue(false);
    }
}
