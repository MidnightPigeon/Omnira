package com.mcmagic.omnira.item.staff;

import com.mcmagic.omnira.item.StaffItem;
import com.mcmagic.omnira.spell.CastAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Immutable values for one explicit implement; never changes the player's live attributes. */
public record StaffCastContext(StaffAssembly.Stats stats, double power, double reduction, double cooldownReduction, java.util.List<String> enhancements) {
    public StaffCastContext { enhancements=java.util.List.copyOf(enhancements); }
    public static StaffCastContext capture(LivingEntity player, ItemStack staff) {
        if (!(staff.getItem() instanceof StaffItem)) throw new IllegalArgumentException("Casting item is not a staff");
        var stats = StaffAssembly.of(staff).stats();
        return new StaffCastContext(stats,
                CastAttributes.power(player,stats.power()),
                CastAttributes.reduction(player,stats.reduction()),
                CastAttributes.cooldownReduction(player,stats.cooldownReduction()),StaffEnhancements.keywords(staff));
    }
}
