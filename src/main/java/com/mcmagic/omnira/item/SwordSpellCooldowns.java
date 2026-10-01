package com.mcmagic.omnira.item;

import com.mcmagic.omnira.spell.IndependentSpellCooldown;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Spell-only cooldown: never blocks melee, charging or healing. */
public final class SwordSpellCooldowns {
    public static final int TICKS=40;
    public static boolean ready(ServerPlayer player,ItemStack sword) {
        return IndependentSpellCooldown.ready(player,sword);
    }
    public static void trigger(ServerPlayer player,ItemStack sword) {
        IndependentSpellCooldown.start(player,sword,TICKS);
    }
    private SwordSpellCooldowns() {}
}
