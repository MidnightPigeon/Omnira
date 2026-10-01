package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.spell.ElementType;
import com.mcmagic.omnira.spell.SpellPattern;
import com.mcmagic.omnira.spell.SpellCasting;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class LowTierMagicCrystalItem extends Item {
    public LowTierMagicCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.get(ModDataComponents.SPELL_PATTERN.get()) == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("tooltip.omnira.low_tier_magic_crystal.unwritten"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!SpellCasting.cast(player, stack.get(ModDataComponents.SPELL_PATTERN.get()), com.mcmagic.omnira.spell.SpellPayload.of(stack), player.getAttributeValue(com.mcmagic.omnira.registry.ModAttributes.SPELL_POWER))) {
                player.displayClientMessage(Component.translatable("message.omnira.crystal.invalid_target"), true);
                return InteractionResultHolder.fail(stack);
            }
            player.getCooldowns().addCooldown(this, com.mcmagic.omnira.spell.SpellCooldowns.ticks(player,20,1));
            player.awardStat(Stats.ITEM_USED.get(this));
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        SpellPattern pattern = stack.get(ModDataComponents.SPELL_PATTERN.get());
        if (pattern == null) {
            tooltipComponents.add(Component.translatable("tooltip.omnira.low_tier_magic_crystal.unwritten")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        var payload=com.mcmagic.omnira.spell.SpellPayload.of(stack);
        var form=pattern.composite()?keyword("spell_composite",pattern.compositeName(),pattern.targetElement()):
                keyword("spell_target_keyword",pattern.targetKeyword().getSerializedName(),pattern.targetElement());
        if(pattern.shapeElement()!=null)form.append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
                .append(keyword("spell_shape_keyword",pattern.shapeKeyword().getSerializedName(),pattern.shapeElement()));
        tooltipComponents.add(form);
        if(!payload.keywords().isEmpty()) {
            var line=Component.empty();
            for(String key:payload.keywords()) {
                if(!line.getSiblings().isEmpty()) line.append(", ");
                line.append(Component.translatable("spell_component.omnira."+key));
            }
            tooltipComponents.add(line.withStyle(ChatFormatting.GRAY));
        } else {
            // Pre-keyword crystals retain known elements; never invent their historical materials.
            for(var effect:payload.effects())
                tooltipComponents.add(Component.translatable("spell_component.omnira."+ (effect.utility()?effect.operation():effect.healing()?"healing":"harm"))
                        .withStyle(ChatFormatting.GRAY));
        }
    }

    private static MutableComponent keyword(String prefix, String keyword, ElementType sourceElement) {
        return Component.translatable(prefix + ".omnira." + keyword)
                .withStyle(style -> style.withColor(sourceElement.tooltipColor()));
    }
}
