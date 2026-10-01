package com.mcmagic.omnira.shop;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.*;

/** A menu session references shared stock, but remains bound to its own customer and access point. */
public final class MarisaMerchant implements Merchant {
    private final MarisaOrbBlockEntity orb;
    private final Player owner;
    private final ServerLevel level;
    private final MarisaTradeLedger ledger;
    private final MarisaTradeLedger.Stock stock;
    private AbstractContainerMenu menu;
    private boolean closed;
    private MarisaRemoteAccess remoteAccess;
    public void remoteAccess(MarisaRemoteAccess access){remoteAccess=access;}
    private void release(){if(remoteAccess!=null)remoteAccess.close();}
    MarisaMerchant(MarisaOrbBlockEntity orb,Player owner){
        this.orb=orb;this.owner=owner;this.level=(ServerLevel)orb.getLevel();
        ledger=MarisaTradeLedger.get(level);stock=ledger.stock(level,owner.getUUID());
    }
    public void bindMenu(AbstractContainerMenu menu){this.menu=menu;}
    public void close(){
        if(closed)return;
        closed=true;release();orb.forget(this);
        if(menu!=null&&owner.containerMenu==menu)owner.closeContainer();
    }
    @Override public Player getTradingPlayer(){
        return !closed&&!orb.isRemoved()&&owner.isAlive()
                &&level.getBlockEntity(orb.getBlockPos())==orb&&stock.day()==MarisaTradeLedger.day(level)
                &&(remoteAccess!=null?remoteAccess.valid():owner.level()==level&&owner.distanceToSqr(orb.getBlockPos().getCenter())<=64)?owner:null;
    }
    @Override public void setTradingPlayer(Player player){if(player==null){closed=true;release();orb.forget(this);}}
    @Override public MerchantOffers getOffers(){return stock.offers();}
    @Override public void overrideOffers(MerchantOffers offers){}
    @Override public void notifyTrade(MerchantOffer offer){
        offer.increaseUses();ledger.setDirty();
        level.playSound(null,orb.getBlockPos(),getNotifyTradeSound(),SoundSource.BLOCKS,.6F,1.3F);
    }
    @Override public void notifyTradeUpdated(ItemStack stack){}
    @Override public int getVillagerXp(){return 0;}
    @Override public void overrideXp(int xp){}
    @Override public boolean showProgressBar(){return false;}
    @Override public boolean canRestock(){return true;}
    @Override public SoundEvent getNotifyTradeSound(){return SoundEvents.AMETHYST_BLOCK_CHIME;}
    @Override public boolean isClientSide(){return false;}
}
