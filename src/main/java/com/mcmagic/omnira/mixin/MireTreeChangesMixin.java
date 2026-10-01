package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.mire.MireTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
abstract class MireTreeChangesMixin {
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void omnira$forgetAlteredTree(BlockPos pos,BlockState state,boolean moving,CallbackInfoReturnable<BlockState> cir){
        var old=cir.getReturnValue();
        if(old!=null&&old.getBlock()!=state.getBlock()&&((LevelChunk)(Object)this).getLevel() instanceof ServerLevel level)
            MireTrees.get(level).changed(level,pos);
    }
}
