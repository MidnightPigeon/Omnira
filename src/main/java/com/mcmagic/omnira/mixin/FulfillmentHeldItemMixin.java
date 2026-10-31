package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The active star is drawn above the player instead of being duplicated in the hand. */
@Mixin(ItemInHandLayer.class)
public abstract class FulfillmentHeldItemMixin {
    @Inject(method="renderArmWithItem",at=@At("HEAD"),cancellable=true)
    private void omnira$levitatingStar(LivingEntity entity,ItemStack stack,ItemDisplayContext context,HumanoidArm arm,
                                       PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){
        if(!(entity instanceof Player player)||!stack.is(ModItems.FULFILLMENT_STAR.get()))return;
        int timer=player.getData(ModAttachments.FULFILLMENT_ACTIVATION);
        if(timer!=0&&arm==(timer>0?player.getMainArm():player.getMainArm().getOpposite()))ci.cancel();
    }
}
