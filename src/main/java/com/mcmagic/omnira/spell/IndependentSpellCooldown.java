package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.registry.ModDataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Cooldown for a spell built into one durable implement, not shared by its item type. */
public final class IndependentSpellCooldown {
    public static boolean ready(Player player,ItemStack stack) {
        if (player.level().isClientSide()) return true;
        var timers = WeaponCooldowns.get(player.getServer());
        var id = stack.get(ModDataComponents.SPELL_COOLDOWN_ID);
        if (id != null) {
            if (timers.active(player.getUUID(), id)) return false;
            stack.remove(ModDataComponents.SPELL_COOLDOWN_ID);
        }
        // Convert a valid pre-migration deadline once, when the item is next used.
        Long deadline = stack.remove(ModDataComponents.SPELL_READY_AT);
        if (deadline == null) return true;
        int duration = stack.getOrDefault(ModDataComponents.SPELL_COOLDOWN_DURATION, 660);
        stack.remove(ModDataComponents.SPELL_COOLDOWN_DURATION);
        long now = player.level().getGameTime();
        if (!com.mcmagic.omnira.util.GameTimeCooldown.active(now, deadline, duration)) return true;
        start(player, stack, (int)(deadline - now));
        return false;
    }

    public static void start(Player player, ItemStack stack, int ticks) {
        var id = WeaponCooldowns.get(player.getServer()).start(player.getUUID(), ticks);
        stack.set(ModDataComponents.SPELL_COOLDOWN_ID, id);
    }

    private IndependentSpellCooldown() {}
}
