package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mcmagic.omnira.mire.MireContent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingRodItem.class)
abstract class MireFishingRodMixin {
    @WrapOperation(method="use",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/projectile/FishingHook;retrieve(Lnet/minecraft/world/item/ItemStack;)I"))
    private int omnira$mireCatch(FishingHook hook,ItemStack rod,Operation<Integer> original,@Share("mireCatch") LocalBooleanRef mire){
        mire.set(MireContent.fluid(hook.level().getFluidState(hook.blockPosition())));
        int damage=original.call(hook,rod);mire.set(mire.get()&&damage==1);return damage;
    }
    @WrapOperation(method="use",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void omnira$threeWearChecks(ItemStack rod,int damage,LivingEntity owner,EquipmentSlot slot,Operation<Void> original,@Share("mireCatch") LocalBooleanRef mire){
        if(mire.get()){for(int i=0;i<3&&!rod.isEmpty();i++)original.call(rod,1,owner,slot);}
        else original.call(rod,damage,owner,slot);
    }
}
