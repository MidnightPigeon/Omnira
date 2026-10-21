package com.mcmagic.omnira.storage;

import com.mcmagic.omnira.block.entity.OrbUpgrades;
import com.mcmagic.omnira.item.OrbUpgradeItem;
import com.mcmagic.omnira.item.SpeedUpgrade;
import net.neoforged.neoforge.items.IItemHandler;

/** Shared transport rates; processing and energy transfer do not use this budget. */
public final class LogisticsRates {
    public static final int ITEMS=4,FLUID=2000,LAVA=200;
    private static final ThreadLocal<Boolean> INTERNAL=ThreadLocal.withInitial(()->false);
    public static boolean internal(){return INTERNAL.get();}
    /** Carry the initiating transport's independent quota through adjacent-face proxies. */
    public static void transfer(Runnable action){boolean previous=INTERNAL.get();INTERNAL.set(true);try{action.run();}finally{INTERNAL.set(previous);}}
    public static int multiplier(IItemHandler upgrades){
        for(int i=0;i<upgrades.getSlots();i++)if(OrbUpgrades.kind(upgrades.getStackInSlot(i))==OrbUpgradeItem.Kind.SPEED)
            return SpeedUpgrade.multiplier(upgrades.getStackInSlot(i));
        return 1;
    }
    public static final class Budget {
        private long time=Long.MIN_VALUE;
        private int credit;
        public int available(long now,int rate){
            long second=Math.floorDiv(now,20);
            if(time!=second){time=second;credit=rate;}else credit=Math.min(credit,rate);
            return credit;
        }
        public void spend(int amount){credit=Math.max(0,credit-Math.max(0,amount));}
    }
    private LogisticsRates(){}
}
