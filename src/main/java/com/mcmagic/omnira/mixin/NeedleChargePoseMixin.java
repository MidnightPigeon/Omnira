package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.client.SwordChargeInput;
import com.mcmagic.omnira.item.RitualSwordItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class NeedleChargePoseMixin {
    @Shadow public abstract void renderItem(net.minecraft.world.entity.LivingEntity entity,ItemStack stack,
            net.minecraft.world.item.ItemDisplayContext context,boolean left,PoseStack pose,MultiBufferSource buffers,int light);
    @Inject(method="renderArmWithItem",at=@At("HEAD"),cancellable=true)
    private void omnira$chargeNeedle(AbstractClientPlayer player,float partial,float pitch,InteractionHand hand,float swing,
                                    ItemStack stack,float equip,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci) {
        if(hand!=InteractionHand.MAIN_HAND || !(stack.getItem() instanceof RitualSwordItem sword) || sword.kind!=RitualSwordItem.Kind.NEEDLE)return;
        float charge=SwordChargeInput.progress(partial);
        if(charge<=0)return;
        double time=player.tickCount+partial;
        boolean left=player.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT;
        var frame=com.mcmagic.omnira.client.renderer.NeedleChargePose.frame(left,charge,time);
        pose.pushPose();
        pose.translate(frame.x(),frame.y(),frame.z());pose.mulPose(frame.rotation());pose.scale(.7F,.7F,.7F);
        // NONE keeps JSON hand transforms from rotating the already aimed blade a second time.
        // The model grip is (8,7.5,8), half a model pixel below its centered origin.
        pose.translate(0,.5/16,0);
        renderItem(player,stack,net.minecraft.world.item.ItemDisplayContext.NONE,false,pose,buffers,light);
        pose.popPose();ci.cancel();
    }
}
