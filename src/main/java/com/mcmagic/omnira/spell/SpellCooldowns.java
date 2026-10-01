package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.registry.ModAttributes;
import com.mcmagic.omnira.item.staff.StaffEvents;
import net.minecraft.world.entity.LivingEntity;

public final class SpellCooldowns {
    public static final int MIN_BASE_TICKS=20;
    public static final double MAX_REDUCTION=.5;
    private SpellCooldowns() {}
    public static int ticks(LivingEntity caster,int baseTicks,int shots) {
        double reduction=caster.getAttributeValue(ModAttributes.COOLDOWN_REDUCTION);
        return ticks(baseTicks,reduction,shots);
    }
    public static int ticks(int baseTicks,double reduction,int shots) {
        int minimum=(Math.clamp(shots,1,64)-1)*StaffEvents.SHOT_INTERVAL+1;
        return (int)Math.clamp(Math.round(Math.max(MIN_BASE_TICKS,baseTicks)*(1-Math.clamp(reduction,-10,MAX_REDUCTION))),minimum,72000);
    }
}
