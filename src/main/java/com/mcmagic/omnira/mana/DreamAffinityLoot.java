package com.mcmagic.omnira.mana;

import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Context-only bonuses: no enchantments are written to the player's equipment. */
public final class DreamAffinityLoot {
    private static final ThreadLocal<Integer> MINING=ThreadLocal.withInitial(()->0);
    private DreamAffinityLoot() {}
    public static boolean dream(Entity entity) {
        return entity instanceof Player player && player.getData(ModAttachments.AFFINITY).active()==Affinity.DREAM;
    }
    public static boolean mining(){return MINING.get()>0;}
    public static int miningBonus(){return MINING.get();}
    public static int lootBonus(Entity entity){return (dream(entity)?1:0)+com.mcmagic.omnira.archaeology.ArchaeologyCurio.lootBonus(entity)
            +com.mcmagic.omnira.time.WonderlandPokerStandBlockEntity.clubBonus(entity);}
    public static <T> T mining(Entity miner,java.util.function.Supplier<T> operation) {
        int before=MINING.get();MINING.set(lootBonus(miner));
        try{return operation.get();}finally{if(before!=0)MINING.set(before);else MINING.remove();}
    }
}
