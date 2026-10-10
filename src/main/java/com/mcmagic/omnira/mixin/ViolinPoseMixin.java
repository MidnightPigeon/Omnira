package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.music.ViolinClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class ViolinPoseMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void omnira$perform(LivingEntity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci){
        if(!(entity instanceof Player p)||!ViolinClient.active(p))return;
        boolean right=p.getMainArm()==HumanoidArm.RIGHT;
        var bow=right?rightArm:leftArm;var violin=right?leftArm:rightArm;float side=right?1:-1;
        violin.xRot=-1.77F;violin.yRot=-side*.255F;violin.zRot=0;
        bow.xRot=-1.85F;bow.yRot=side*(-.55F+(float)Math.sin(age*.18)*.18F);bow.zRot=0;
    }
}
