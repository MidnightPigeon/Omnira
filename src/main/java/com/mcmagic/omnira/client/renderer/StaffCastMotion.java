package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.registry.ModDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Shared, short forward gesture for successful casts, never for inventory icons. */
public final class StaffCastMotion {
    private StaffCastMotion(){}
    public static void apply(ItemStack stack,ItemDisplayContext context,PoseStack pose,double time){
        if(context!=ItemDisplayContext.FIRST_PERSON_LEFT_HAND&&context!=ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                &&context!=ItemDisplayContext.THIRD_PERSON_LEFT_HAND&&context!=ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)return;
        Long at=stack.get(stack.getItem() instanceof com.mcmagic.omnira.item.IndustrialWheelItem?ModDataComponents.WHEEL_CAST_AT:ModDataComponents.STAFF_CAST_AT);
        if(at==null)return;
        double age=time-at;if(age<0||age>=8)return;
        float wave=(float)Math.sin(Math.PI*age/8);
        pose.translate(0,0,-.06F*wave);
        pose.translate(.5,.5,.5);pose.mulPose(Axis.XP.rotationDegrees(-9*wave));pose.translate(-.5,-.5,-.5);
    }
}
