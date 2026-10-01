package com.mcmagic.omnira.shop;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.*;
import java.util.*;

public final class MarisaTrades {
    private static final ResourceLocation TABLE=ResourceLocation.fromNamespaceAndPath("omnira","marisa_trades/default.json");
    private enum Tier {
        COMMON("common",3,3,null),RARE("rare",2,2,"omnira:dream_crystal_shard"),EPIC("epic",1,1,"omnira:spatial_crystal_shard");
        final String id,extra;final int count,limit;
        Tier(String id,int count,int limit,String extra){this.id=id;this.count=count;this.limit=limit;this.extra=extra;}
    }
    private MarisaTrades(){}
    public static MerchantOffers roll(ServerLevel level,long seed,long day){
        var offers=new MerchantOffers();
        try(var reader=level.getServer().getResourceManager().getResourceOrThrow(TABLE).openAsReader()){
            var json=JsonParser.parseReader(reader).getAsJsonObject();var random=new Random(seed^day*0x9E3779B97F4A7C15L);
            for(var tier:Tier.values()){
                List<MerchantOffer> pool=new ArrayList<>();Set<String> products=new HashSet<>();
                for(var entry:json.getAsJsonArray(tier.id)){
                    var row=entry.getAsJsonObject();
                    if(!products.add(row.get("item").getAsString()))throw new IllegalArgumentException("Duplicate product in "+tier.id);
                    pool.add(offer(row,tier));
                }
                if(pool.size()<tier.count)throw new IllegalArgumentException("Too few products in "+tier.id);
                Collections.shuffle(pool,random);offers.addAll(pool.subList(0,tier.count));
            }
        }catch(java.io.IOException ex){throw new IllegalStateException("Missing Marisa trade table",ex);}
        return offers;
    }
    private static ItemStack stack(String id,int count){
        var item=BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElseThrow(()->new IllegalArgumentException("Unknown trade item: "+id));
        var result=new ItemStack(item,count);
        if(result.isEmpty()||count>result.getMaxStackSize())throw new IllegalArgumentException("Invalid trade count: "+id+" x"+count);
        return result;
    }
    private static ItemCost cost(String id,int count){var stack=stack(id,count);return new ItemCost(stack.getItem(),stack.getCount());}
    private static MerchantOffer offer(JsonObject row,Tier tier){
        return new MerchantOffer(cost("omnira:infused_spiritual_crystal",row.get("infused").getAsInt()),
                tier.extra==null?Optional.empty():Optional.of(cost(tier.extra,row.get("extra").getAsInt())),
                stack(row.get("item").getAsString(),row.get("count").getAsInt()),tier.limit,0,0);
    }
}
