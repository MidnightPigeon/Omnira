package com.mcmagic.omnira.alchemy.catalysis;

import com.google.gson.*;
import com.mcmagic.omnira.mixin.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.storage.loot.LootTable;
import java.util.*;

/** Enumerates native loot and trunk providers without rolling loot or growing preview trees. */
@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class CatalysisProducts {
    private static final Map<BlockState,List<ItemStack>> CACHE=new java.util.concurrent.ConcurrentHashMap<>();
    public static List<ItemStack> possible(ServerLevel level,CatalysisGrowth.Plan plan,ItemStack seed) {
        if(!plan.product().isEmpty())return List.of(plan.product());
        var state=plan.yield()==CatalysisGrowth.Yield.FRUIT?
                (plan.plant().is(Blocks.MELON_STEM)?Blocks.MELON:Blocks.PUMPKIN).defaultBlockState():plan.plant();
        var products=CACHE.computeIfAbsent(state,s->enumerate(level,s,plan.yield()==CatalysisGrowth.Yield.TREE));
        return CatalysisGrowth.products(products,seed,1);
    }
    private static List<ItemStack> enumerate(ServerLevel level,BlockState state,boolean tree) {
        var items=new LinkedHashSet<Item>();var ops=level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        if(tree) {
            var grower=state.getBlock() instanceof SaplingBlock sapling?((CatalysisSaplingAccessor)sapling).omnira$grower():
                    state.is(Blocks.AZALEA)||state.is(Blocks.FLOWERING_AZALEA)?net.minecraft.world.level.block.grower.TreeGrower.AZALEA:null;
            if(grower!=null) {
                var access=(CatalysisTreeGrowerAccessor)(Object)grower;
                for(var key:List.of(access.omnira$primaryTree(),access.omnira$secondaryTree(),access.omnira$primaryMegaTree(),
                        access.omnira$secondaryMegaTree(),access.omnira$flowerTree(),access.omnira$secondaryFlowerTree())) {
                    if(key.isEmpty())continue;
                    var feature=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(key.get());
                    if(feature!=null)ConfiguredFeature.DIRECT_CODEC.encodeStart(ops,feature).result().ifPresent(json->walk(json,true,items));
                }
            }
        }else {
            var table=level.getServer().reloadableRegistries().getLootTable(state.getBlock().getLootTable());
            LootTable.DIRECT_CODEC.encodeStart(ops,table).result().ifPresent(json->walk(json,false,items));
        }
        items.remove(Items.AIR);
        return items.stream().map(ItemStack::new).toList();
    }
    private static void walk(JsonElement json,boolean tree,Set<Item> items) {
        if(json.isJsonArray()){json.getAsJsonArray().forEach(e->walk(e,tree,items));return;}
        if(!json.isJsonObject())return;var object=json.getAsJsonObject();
        if(tree&&object.has("Name")) {
            var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse(object.get("Name").getAsString()));
            if(block.defaultBlockState().is(BlockTags.LOGS))items.add(block.asItem());
        }else if(!tree&&object.has("type")&&object.has("name")) {
            var id=ResourceLocation.parse(object.get("name").getAsString());
            if(object.get("type").getAsString().equals("minecraft:item"))items.add(BuiltInRegistries.ITEM.get(id));
            else if(object.get("type").getAsString().equals("minecraft:tag"))
                BuiltInRegistries.ITEM.getTag(net.minecraft.tags.TagKey.create(Registries.ITEM,id)).ifPresent(tag->tag.forEach(holder->items.add(holder.value())));
        }
        object.entrySet().forEach(entry->walk(entry.getValue(),tree,items));
    }
    @net.neoforged.bus.api.SubscribeEvent public static void reload(net.neoforged.neoforge.event.TagsUpdatedEvent event){CACHE.clear();}
    @net.neoforged.bus.api.SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event){CACHE.clear();}
    private CatalysisProducts(){}
}
