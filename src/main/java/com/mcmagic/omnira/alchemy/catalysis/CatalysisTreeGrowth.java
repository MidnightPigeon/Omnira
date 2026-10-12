package com.mcmagic.omnira.alchemy.catalysis;

import com.mcmagic.omnira.mixin.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import java.lang.reflect.*;
import java.util.*;

/** A bounded, write-isolated feature view. Never grows a temporary tree in the player's world. */
public final class CatalysisTreeGrowth {
    public static List<ItemStack> harvest(ServerLevel level,BlockState sapling,BlockState soil){
        ResourceKey<ConfiguredFeature<?,?>> key;
        if(sapling.is(Blocks.CRIMSON_FUNGUS))key=TreeFeatures.CRIMSON_FUNGUS_PLANTED;
        else if(sapling.is(Blocks.WARPED_FUNGUS))key=TreeFeatures.WARPED_FUNGUS_PLANTED;
        else {
            var grower=sapling.getBlock() instanceof SaplingBlock s?((CatalysisSaplingAccessor)s).omnira$grower():
                    sapling.is(Blocks.AZALEA)||sapling.is(Blocks.FLOWERING_AZALEA)?net.minecraft.world.level.block.grower.TreeGrower.AZALEA:null;
            if(grower==null)return List.of();
            var access=(CatalysisTreeGrowerAccessor)(Object)grower;
            key=access.omnira$tree(level.random,false);if(key==null)key=access.omnira$megaTree(level.random);if(key==null)return List.of();
        }
        var feature=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(key);if(feature==null)return List.of();
        var origin=new BlockPos(0,64,0);var blocks=new HashMap<BlockPos,BlockState>();
        java.util.function.Function<BlockPos,BlockState> state=p->blocks.getOrDefault(p,p.getY()<64?soil:Blocks.AIR.defaultBlockState());
        var view=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},(proxy,method,args)->{
            String name=method.getName();
            return switch(name){
                case "getBlockState"->state.apply((BlockPos)args[0]);
                case "getFluidState"->state.apply((BlockPos)args[0]).getFluidState();
                case "isEmptyBlock"->state.apply((BlockPos)args[0]).isAir();
                case "isStateAtPosition"->((java.util.function.Predicate<BlockState>)args[1]).test(state.apply((BlockPos)args[0]));
                case "isFluidAtPosition"->((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>)args[1]).test(state.apply((BlockPos)args[0]).getFluidState());
                case "setBlock","destroyBlock","removeBlock"->{
                    var p=(BlockPos)args[0];if(Math.abs(p.getX())>48||Math.abs(p.getZ())>48||p.getY()<0||p.getY()>190||blocks.size()>50000)yield false;
                    blocks.put(p.immutable(),name.equals("setBlock")?(BlockState)args[1]:Blocks.AIR.defaultBlockState());yield true;
                }
                case "getBlockEntity"->args.length==2?Optional.empty():null;
                case "getMinBuildHeight"->0;
                case "getMaxBuildHeight","getHeight"->args==null||args.length==0?256:64;
                case "getHeightmapPos"->((BlockPos)args[1]).atY(64);
                case "isOutsideBuildHeight"->false;
                case "ensureCanWrite","hasChunk","hasChunkAt","hasChunksAt","isLoaded","canSeeSky"->true;
                case "isClientSide"->false;
                case "getRandom"->level.random;
                case "getSeed"->level.getSeed();
                case "registryAccess"->level.registryAccess();
                case "getBiome"->level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
                case "getBrightness","getRawBrightness","getMaxLocalRawBrightness","getMaxLightLevel"->15;
                case "setCurrentlyGenerating","blockUpdated","scheduleTick","levelEvent","gameEvent"->null;
                case "getEntities"->List.of();
                case "toString"->"Omnira isolated catalysis tree";
                default->{if(method.isDefault())yield InvocationHandler.invokeDefault(proxy,method,args);throw new UnsupportedOperationException("Unsupported isolated growth operation: "+name);}
            };
        });
        try{
            if(!feature.place(view,level.getChunkSource().getGenerator(),level.random,origin))return List.of();
        }catch(RuntimeException e){
            if(WARNED.add(key.location().toString()))com.mcmagic.omnira.Omnira.LOGGER.warn("Catalysis tree {} cannot be simulated: {}",key.location(),e.getMessage());
            return List.of();
        }
        var counts=new LinkedHashMap<net.minecraft.world.item.Item,Integer>();
        blocks.values().stream().filter(s->s.is(BlockTags.LOGS)).forEach(s->counts.merge(s.getBlock().asItem(),1,Integer::sum));
        var result=new ArrayList<ItemStack>();counts.forEach((item,count)->{while(count>0){int n=Math.min(count,item.getDefaultMaxStackSize());result.add(new ItemStack(item,n));count-=n;}});return result;
    }
    private static final Set<String> WARNED=new HashSet<>();
    private CatalysisTreeGrowth(){}
}
