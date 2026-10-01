package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mcmagic.omnira.mire.MireTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Set;
import java.util.HashSet;

@Mixin(TreeFeature.class)
abstract class MireTreeFeatureMixin {
    @Inject(method="place",at=@At("RETURN"))
    private void omnira$recordGeneratedTree(FeaturePlaceContext<TreeConfiguration> context,CallbackInfoReturnable<Boolean> cir,
            @Local(ordinal=1) Set<BlockPos> logs,@Local(ordinal=2) Set<BlockPos> leaves){
        if(!cir.getReturnValue())return;
        var footprint=new HashSet<>(logs);footprint.addAll(leaves);
        MireTrees.generated(context.level(),context.origin(),footprint);
    }
}
