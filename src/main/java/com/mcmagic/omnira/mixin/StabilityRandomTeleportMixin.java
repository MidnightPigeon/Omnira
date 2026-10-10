package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.anchor.StabilityField;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class StabilityRandomTeleportMixin {
    @Inject(method="randomTeleport",at=@At("HEAD"),cancellable=true)
    private void omnira$random(double x,double y,double z,boolean particles,CallbackInfoReturnable<Boolean> cir){
        var entity=(LivingEntity)(Object)this;
        if(entity.level() instanceof ServerLevel level){
            if(StabilityField.blocks(entity,level,new Vec3(x,y,z))){cir.setReturnValue(false);return;}
            // Vanilla lowers a random landing to solid ground before calling teleportTo.
            var pos=net.minecraft.core.BlockPos.containing(x,y,z);double actualY=y;
            if(level.hasChunkAt(pos)){
                while(pos.getY()>level.getMinBuildHeight()&&!level.getBlockState(pos.below()).blocksMotion()){pos=pos.below();actualY--;}
                if(StabilityField.blocks(entity,level,new Vec3(x,actualY,z)))cir.setReturnValue(false);
            }
        }
    }
}
