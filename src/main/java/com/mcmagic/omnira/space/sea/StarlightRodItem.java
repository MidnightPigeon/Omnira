package com.mcmagic.omnira.space.sea;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.neoforge.common.*;

public final class StarlightRodItem extends FishingRodItem {
    public StarlightRodItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON).component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2F)));
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600), attacker);
        return true;
    }

    @Override public boolean isEnchantable(ItemStack stack) { return true; }

    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        boolean casting = player.fishing == null;
        var result = super.use(level, player, hand);
        if (casting && !level.isClientSide && player.fishing != null) ((StarlightFloat)player.fishing).omnira$castWith(player.getItemInHand(hand));
        return result;
    }

    private boolean allowed(Holder<Enchantment> enchantment) {
        return !enchantment.is(Enchantments.UNBREAKING) && !enchantment.is(Enchantments.MENDING);
    }

    @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return allowed(enchantment) && (Items.IRON_SWORD.getDefaultInstance().supportsEnchantment(enchantment)
                || Items.FISHING_ROD.getDefaultInstance().supportsEnchantment(enchantment));
    }

    @Override public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return allowed(enchantment) && (Items.IRON_SWORD.getDefaultInstance().isPrimaryItemFor(enchantment)
                || Items.FISHING_ROD.getDefaultInstance().isPrimaryItemFor(enchantment));
    }

    @Override public boolean canPerformAction(ItemStack stack, ItemAbility action) {
        return action == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, action);
    }
}
