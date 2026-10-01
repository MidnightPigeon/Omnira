package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.mire.MireTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SaplingBlock.class)
abstract class MireSaplingMixin {
    @Inject(method="advanceTree",at=@At("HEAD"),cancellable=true)
    private void omnira$holdRewoundSapling(ServerLevel level,BlockPos pos,BlockState state,RandomSource random,CallbackInfo ci){
        if(MireTrees.get(level).rewinding(pos))ci.cancel();
    }
}
