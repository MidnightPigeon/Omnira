package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.item.ArcaneArquebusItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class ArquebusPoseMixin {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void omnira$holdArquebus(LivingEntity entity,float limbSwing,float limbAmount,float age,float yaw,float pitch,CallbackInfo ci) {
        if(!(entity instanceof Player) || !(entity.getMainHandItem().getItem() instanceof ArcaneArquebusItem)
                || entity.isUsingItem() && entity.getUsedItemHand()==InteractionHand.OFF_HAND)return;
        boolean right=entity.getMainArm()==HumanoidArm.RIGHT;
        var arm=right?rightArm:leftArm;
        boolean firing=entity.swinging && entity.swingingArm==InteractionHand.MAIN_HAND;
        arm.xRot=-(float)Math.PI/2+(firing?head.xRot:.38F);
        arm.yRot=firing?head.yRot:0;
        arm.zRot=0;
        if(entity.getOffhandItem().isEmpty()) {
            var support=right?leftArm:rightArm;
            support.xRot=arm.xRot+.15F;support.yRot=arm.yRot+(right?.5F:-.5F);support.zRot=0;
        }
    }
}
