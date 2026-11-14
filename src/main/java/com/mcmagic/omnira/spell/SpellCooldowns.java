package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.registry.ModAttributes;
import com.mcmagic.omnira.item.staff.StaffEvents;
import net.minecraft.world.entity.LivingEntity;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class SpellCooldowns {
    public static final int MIN_BASE_TICKS=20;
    public static final double MAX_REDUCTION=.5;
    private SpellCooldowns() {}
    // Advance both endpoints on a held tick so vanilla expiry and HUD percentage stay in sync.
    @net.neoforged.bus.api.SubscribeEvent
    public static void pauseRegionalSpells(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event) {
        var player = event.getEntity();
        var entries = ((com.mcmagic.omnira.mixin.ItemCooldownMapAccessor) player.getCooldowns()).omnira$cooldownEntries();
        if (entries.isEmpty() || !com.mcmagic.omnira.time.StilledTime.pauseSpellCooldown(player)) return;
        entries.forEach((item, value) -> {
            if (item instanceof com.mcmagic.omnira.item.StaffItem || item instanceof com.mcmagic.omnira.item.IndustrialWheelItem
                    || item instanceof com.mcmagic.omnira.item.LowTierMagicCrystalItem) {
                var window = (com.mcmagic.omnira.mixin.ItemCooldownWindowAccessor) value;
                window.omnira$start(window.omnira$start() + 1);
                window.omnira$end(window.omnira$end() + 1);
            }
        });
    }

    public static int ticks(LivingEntity caster,int baseTicks,int shots) {
        double reduction=caster.getAttributeValue(ModAttributes.COOLDOWN_REDUCTION);
        return ticks(baseTicks,reduction,shots);
    }
    public static int ticks(int baseTicks,double reduction,int shots) {
        int minimum=(Math.clamp(shots,1,64)-1)*StaffEvents.SHOT_INTERVAL+1;
        return (int)Math.clamp(Math.round(Math.max(MIN_BASE_TICKS,baseTicks)*(1-Math.clamp(reduction,-10,MAX_REDUCTION))),minimum,72000);
    }
}
