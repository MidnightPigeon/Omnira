package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.spell.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class WorldRotationCrystalItem extends LowTierMagicCrystalItem {
    public WorldRotationCrystalItem(Properties properties){super(properties
            .component(ModDataComponents.SPELL_PATTERN.get(),new SpellPattern(ElementType.OMNI,null))
            .component(ModDataComponents.SPELL_PAYLOAD.get(),new SpellPayload(0,0)));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!level.isClientSide){
            if(!(player instanceof net.minecraft.server.level.ServerPlayer server)||!com.mcmagic.omnira.spell.world.WorldRotation.cast(server))return InteractionResultHolder.fail(stack);
            player.getCooldowns().addCooldown(this,1200);stack.consume(1,player);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.omnira.world_rotation").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
    }
}
