package com.mcmagic.omnira.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** One daily offer object per UUID for the whole save, including every dimension. */
public final class MarisaTradeLedger extends SavedData {
    public record Stock(long day,MerchantOffers offers){}
    private final Map<UUID,Stock> players=new HashMap<>();
    public static MarisaTradeLedger get(ServerLevel level){
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MarisaTradeLedger::new,MarisaTradeLedger::load,null),"omnira_marisa_trades");
    }
    public static long day(ServerLevel level){return Math.floorDiv(level.getServer().overworld().getGameTime(),24000L);}
    public Stock stock(ServerLevel level,UUID player){
        long day=day(level);var stock=players.get(player);
        if(stock==null||stock.day()!=day){
            long seed=level.getServer().overworld().getSeed()^player.getMostSignificantBits()^Long.rotateLeft(player.getLeastSignificantBits(),29);
            stock=new Stock(day,MarisaTrades.roll(level,seed,day));players.put(player,stock);setDirty();
        }
        return stock;
    }
    public static MarisaTradeLedger load(CompoundTag tag,HolderLookup.Provider registries){
        var ledger=new MarisaTradeLedger();
        for(var value:tag.getList("Players",Tag.TAG_COMPOUND)){
            var entry=(CompoundTag)value;
            var offers=MerchantOffers.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE),entry.get("Offers")).getOrThrow();
            ledger.players.put(entry.getUUID("Player"),new Stock(entry.getLong("Day"),offers));
        }
        return ledger;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var entries=new ListTag();
        players.forEach((player,stock)->{
            var entry=new CompoundTag();entry.putUUID("Player",player);entry.putLong("Day",stock.day());
            entry.put("Offers",MerchantOffers.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),stock.offers()).getOrThrow());entries.add(entry);
        });
        tag.put("Players",entries);return tag;
    }
}
