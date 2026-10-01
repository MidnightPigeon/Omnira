package com.mcmagic.omnira.mana;

import com.mcmagic.omnira.registry.ModAttributes;
import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.item.StaffItem;
import com.mcmagic.omnira.item.staff.StaffAssembly;
import com.mcmagic.omnira.spell.CastAttributes;
import net.minecraft.world.entity.player.Player;

public final class ManaCosts {
    private ManaCosts() {}
    public static double cost(Player player,double base) {
        return cost(player,base,1);
    }
    /** Percentage costs affect the authored base first; flat reductions are applied exactly once afterward. */
    public static double cost(Player player,double base,double multiplier) {
        // Main-hand staff bonuses already belong to equipment attributes; use the offhand only as fallback.
        if(!(player.getMainHandItem().getItem() instanceof StaffItem) && player.getOffhandItem().getItem() instanceof StaffItem)
            return cost(base,multiplier,CastAttributes.reduction(player,StaffAssembly.of(player.getOffhandItem()).stats().reduction()));
        return cost(base,multiplier,player.getAttributeValue(ModAttributes.COST_REDUCTION));
    }
    public static double cost(double base,double multiplier,double reduction) {
        return Math.max(0,base*multiplier-reduction);
    }
    public static boolean canSpend(Player player,double base) {
        return player.getData(ModAttachments.MANA).canSpend(cost(player,base));
    }
}
