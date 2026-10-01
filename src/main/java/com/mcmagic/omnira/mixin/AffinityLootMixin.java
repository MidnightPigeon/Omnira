package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mcmagic.omnira.mana.DreamAffinityLoot;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
public abstract class AffinityLootMixin {
    @ModifyReturnValue(method="getItemEnchantmentLevel",at=@At("RETURN"))
    private static int omnira$fortune(int original,Holder<Enchantment> enchantment,ItemStack stack) {
        return original+(enchantment.is(Enchantments.FORTUNE)?DreamAffinityLoot.miningBonus():0);
    }
    @ModifyReturnValue(method="getEnchantmentLevel",at=@At("RETURN"))
    private static int omnira$looting(int original,Holder<Enchantment> enchantment,LivingEntity entity) {
        return original+(enchantment.is(Enchantments.LOOTING)?DreamAffinityLoot.lootBonus(entity):0);
    }
}
