package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Area glue may surround the separator, but must never create a bond to either face. */
@Pseudo
@Mixin(targets="com.simibubi.create.content.contraptions.glue.SuperGlueEntity",remap=false)
public abstract class SpacetimeGlueMixin {
    private static boolean sealedFace(LevelAccessor level,BlockPos pos,Direction direction){
        return level.getBlockState(pos).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get())
                || level.getBlockState(pos.relative(direction)).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get());
    }
    @Inject(method="isGlued",at=@At("HEAD"),cancellable=true)
    private static void omnira$noSealedBond(LevelAccessor level,BlockPos pos,Direction direction,java.util.Set<?> cache,CallbackInfoReturnable<Boolean> result){
        if(sealedFace(level,pos,direction))result.setReturnValue(false);
    }
    @Inject(method="isValidFace",at=@At("HEAD"),cancellable=true)
    private static void omnira$noSealedFace(Level level,BlockPos pos,Direction direction,CallbackInfoReturnable<Boolean> result){
        if(sealedFace(level,pos,direction))result.setReturnValue(false);
    }
}
