package com.mcmagic.omnira.alchemy.catalysis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** Server-derived examples use the same eligibility and harvest rules as the chamber. */
@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class CatalysisCatalogue {
    private static List<CatalysisRecipe> cached;

    public static List<CatalysisRecipe> build(ServerLevel level) {
        var water=new FluidStack(Fluids.WATER,500);
        var soils=BuiltInRegistries.BLOCK.stream()
                .filter(b->b.defaultBlockState().is(BlockTags.DIRT)||b==Blocks.FARMLAND)
                .map(ItemStack::new).filter(s->!s.isEmpty()).toList();
        var result=new ArrayList<CatalysisRecipe>();
        for(var item:BuiltInRegistries.ITEM) {
            if(!(item instanceof BlockItem))continue;
            var seed=new ItemStack(item);
            var accepted=new ArrayList<ItemStack>();
            CatalysisGrowth.Plan plan=null;
            for(var soil:soils) {
                var candidate=CatalysisGrowth.resolve(level,soil,seed,water);
                if(candidate==null)continue;
                if(candidate.yield()==CatalysisGrowth.Yield.FIXED&&!seed.is(Items.BAMBOO))continue;
                accepted.add(soil);plan=candidate;
            }
            if(plan==null||accepted.isEmpty())continue;
            var products=new LinkedHashMap<Item,ItemStack>();
            // Examples are representative, not guaranteed yields; never plant in the actual world.
            try {
                for(int attempt=0;attempt<4;attempt++) {
                    for(var product:plan.harvest(level,BlockPos.ZERO,accepted.getFirst(),seed))
                        if(!product.isEmpty())products.putIfAbsent(product.getItem(),product);
                    if(plan.yield()==CatalysisGrowth.Yield.TREE&&!products.isEmpty())break;
                }
            } catch(RuntimeException failure) {
                com.mcmagic.omnira.Omnira.LOGGER.warn("Cannot preview catalysis seed {}: {}",BuiltInRegistries.ITEM.getKey(item),failure.toString());
            }
            for(var product:products.values())result.add(new CatalysisRecipe(
                    Ingredient.of(accepted.stream()),Ingredient.of(seed),water,product,plan.stages()));
        }
        return List.copyOf(result);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void sync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        var server=event.getPlayerList().getServer();
        if(cached==null||event.getPlayer()==null)cached=build(server.overworld());
        var players=event.getPlayer()==null?event.getPlayerList().getPlayers():List.of(event.getPlayer());
        for(var player:players) {
            if(cached.isEmpty())net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new com.mcmagic.omnira.network.CatalysisCataloguePayload(List.of(),true,true));
            for(int start=0;start<cached.size();start+=32) {
                int end=Math.min(start+32,cached.size());
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new com.mcmagic.omnira.network.CatalysisCataloguePayload(cached.subList(start,end),start==0,end==cached.size()));
            }
        }
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){cached=null;}
    private CatalysisCatalogue(){}
}
