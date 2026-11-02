package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.fml.ModList;

/** Keep optional Lootr types outside classes loaded in a standalone installation. */
public final class LootrOrbs {
    private LootrOrbs() {}
    public static boolean manage(CrystalBallBlockEntity orb) {
        if(orb.virtualStorage || orb.getLevel()==null || orb.getLevel().isClientSide
                || (!orb.lootrManaged && !orb.hasPendingLoot()))return false;
        if(!ModList.get().isLoaded("lootr")){
            if(orb.lootrManaged){orb.lootrManaged=false;orb.setChanged();}
            return false;
        }
        return LootrOrbStorage.manage(orb);
    }
    public static boolean appearance(CrystalBallBlockEntity orb){return orb!=null && ModList.get().isLoaded("lootr") && (orb.lootrManaged||orb.lootrPersonalView);}
    public static boolean blocksAutomation(net.minecraft.world.level.block.entity.BlockEntity be){
        if(be==null||!ModList.get().isLoaded("lootr"))return false;
        if(be instanceof CrystalBallBlockEntity orb)return orb.lootrPersonalView||manage(orb);
        return LootrOrbStorage.isLootr(be);
    }
    public static AbstractContainerMenu menu(int id,Inventory inventory,ServerPlayer player,CrystalBallBlockEntity orb){
        return LootrOrbStorage.menu(id,inventory,player,orb);
    }
    public static boolean mayBreak(net.minecraft.world.entity.player.Player player){
        return !ModList.get().isLoaded("lootr")||LootrOrbStorage.mayBreak(player);
    }
    public static void warnBreak(net.minecraft.world.entity.player.Player player){
        if(ModList.get().isLoaded("lootr"))LootrOrbStorage.warnBreak(player);
    }
    public static boolean opened(CrystalBallBlockEntity orb,ServerPlayer player){
        return ModList.get().isLoaded("lootr")&&LootrOrbStorage.opened(orb,player);
    }
    public static void sync(CrystalBallBlockEntity orb,ServerPlayer player){
        if(manage(orb))LootrOrbStorage.sync(orb,player);
    }
    public static void clear(){if(ModList.get().isLoaded("lootr"))LootrOrbStorage.clear();}
}
