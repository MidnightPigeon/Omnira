package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class UnorderedClusterItem extends Item {
    public UnorderedClusterItem(Properties properties) { super(properties); }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> lines, TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.unordered_element_cluster"));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            var shot = new ThrownUnorderedCluster(ModEntityTypes.THROWN_UNORDERED_CLUSTER.get(), level);
            shot.setOwner(player);
            shot.setPos(player.getEyePosition());
            var aim = player.getLookAngle();
            shot.shoot(aim.x, aim.y, aim.z, 1.5F, 0);
            if (!server.addFreshEntity(shot)) return InteractionResultHolder.fail(stack);
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
