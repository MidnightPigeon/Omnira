package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.registry.ModDataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Cooldown for a spell built into one durable implement, not shared by its item type. */
public final class IndependentSpellCooldown {
    public static boolean ready(Player player,ItemStack stack) {
        return player.level().getGameTime()>=stack.getOrDefault(ModDataComponents.SPELL_READY_AT,0L);
    }
    public static void start(Player player,ItemStack stack,int ticks) {
        stack.set(ModDataComponents.SPELL_READY_AT,player.level().getGameTime()+ticks);
    }
    private IndependentSpellCooldown() {}
}
