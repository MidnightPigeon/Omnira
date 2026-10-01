package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.mire.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.time.TimeSaplingBlock;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("omnira_mire_trees")
@PrefixGameTestTemplate(false)
public final class MireTreeTrackingGameTests {
    private static BlockPos setup(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,3,5));
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(MireCycle.BIOME);
        level.getChunkAt(p).fillBiomesFromNoise((x,y,z,s)->biome,level.getChunkSource().randomState().sampler());
        return p;
    }
    private static boolean tracked(GameTestHelper h,BlockPos p){
        var tag=MireTrees.get(h.getLevel()).save(new CompoundTag(),h.getLevel().registryAccess());
        return tag.getList("Trees",10).stream().anyMatch(t->((CompoundTag)t).getLong("Root")==p.asLong());
    }
    @GameTest(template="spell_arena") public static void alteredTreesAndBuildings(GameTestHelper h){
        var l=h.getLevel();var p=setup(h);var data=MireTrees.get(l);
        for(var block:List.of(TimeNatureContent.LOG.get(),TimeNatureContent.LEAVES.get())){
            l.setBlockAndUpdate(p,TimeNatureContent.LOG.get().defaultBlockState());
            l.setBlockAndUpdate(p.above(),block.defaultBlockState());data.register(p,List.of(p,p.above()));
            l.setBlockAndUpdate(p.above(),Blocks.AIR.defaultBlockState());
            l.setBlockAndUpdate(p.above(),block.defaultBlockState());
            h.assertTrue(!tracked(h,p),"Broken member did not revoke whole tree");
            data.pulse(l,6000);h.assertTrue(l.getBlockState(p).is(TimeNatureContent.LOG.get()),"Rebuilt house rewound");
        }
        var house=p.offset(3,0,0);l.setBlockAndUpdate(house,TimeNatureContent.LOG.get().defaultBlockState());
        l.setBlockAndUpdate(house.above(),TimeNatureContent.LEAVES.get().defaultBlockState());
        h.assertTrue(!tracked(h,house),"Player building registered as tree");h.succeed();
    }
    @GameTest(template="spell_arena") public static void internalChangesAndPersistence(GameTestHelper h){
        var l=h.getLevel();var p=setup(h);var data=MireTrees.get(l);
        l.setBlockAndUpdate(p,TimeNatureContent.LOG.get().defaultBlockState());
        l.setBlockAndUpdate(p.above(),TimeNatureContent.LEAVES.get().defaultBlockState());data.register(p,List.of(p,p.above()));
        data.pulse(l,6000);h.assertTrue(tracked(h,p)&&data.rewinding(p),"Internal rewind revoked tree");
        data.pulse(l,7200);h.assertTrue(tracked(h,p)&&l.getBlockState(p).is(TimeNatureContent.LOG.get()),"Tree did not restore");
        var reloaded=MireTrees.load(data.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
        reloaded.changed(l,p.above());
        h.assertTrue(reloaded.save(new CompoundTag(),l.registryAccess()).getList("Trees",10).stream()
                .noneMatch(t->((CompoundTag)t).getLong("Root")==p.asLong()),"Reloaded position index failed");
        data.pulse(l,12000);l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        l.setBlockAndUpdate(p,TimeNatureContent.SAPLING.get().defaultBlockState());data.pulse(l,13200);
        h.assertTrue(l.getBlockState(p).is(TimeNatureContent.SAPLING.get())&&!l.getBlockState(p).getValue(TimeSaplingBlock.REWINDING),"Replacement sapling resurrected tree");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=120) public static void vanillaAndShadowFeatures(GameTestHelper h){
        var l=h.getLevel();var p=setup(h);
        var features=l.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        l.setBlockAndUpdate(p.below(),Blocks.GRASS_BLOCK.defaultBlockState());
        var oak=features.getHolderOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.parse("minecraft:oak"))).value();
        h.assertTrue(oak.place(l,l.getChunkSource().getGenerator(),l.random,p),"Oak feature failed");
        h.runAfterDelay(2,()->{
            h.assertTrue(tracked(h,p),"Vanilla feature not registered");
            var data=MireTrees.get(l);data.pulse(l,6000);
            h.assertTrue(l.getBlockState(p).is(Blocks.OAK_SAPLING),"Wrong vanilla sapling");
            ((SaplingBlock)Blocks.OAK_SAPLING).advanceTree(l,p,l.getBlockState(p),l.random);
            h.assertTrue(l.getBlockState(p).is(Blocks.OAK_SAPLING),"Rewound sapling grew early");
            data.pulse(l,7200);h.assertTrue(l.getBlockState(p).is(Blocks.OAK_LOG),"Oak not restored");
            l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
            var shadowRoot=p.offset(10,0,0);
            var biome=l.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(MireCycle.BIOME);
            l.getChunkAt(shadowRoot).fillBiomesFromNoise((x,y,z,s)->biome,l.getChunkSource().randomState().sampler());
            l.setBlockAndUpdate(shadowRoot.below(),Blocks.GRASS_BLOCK.defaultBlockState());
            var shadow=features.getHolderOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.parse("omnira:shadow_tree"))).value();
            h.assertTrue(shadow.place(l,l.getChunkSource().getGenerator(),l.random,shadowRoot),"Shadow feature failed");
            h.runAfterDelay(2,()->{
                h.assertTrue(tracked(h,shadowRoot),"Shadow feature not registered");data.pulse(l,12000);
                h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(l.getBlockState(shadowRoot).getBlock()).getPath().equals("shadow_sapling"),"Shadow restored to wrong sapling");
                data.pulse(l,13200);h.assertTrue(l.getBlockState(shadowRoot).is(DreamContent.SHADOW_LOG.get()),"Shadow not restored");h.succeed();
            });
        });
    }
}
