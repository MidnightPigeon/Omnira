package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.time.*;
import com.mcmagic.omnira.mire.MireCycle;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_temporal_volumes")
@PrefixGameTestTemplate(false)
public final class TemporalBiomeVolumeGameTests {
    private static BlockPos layers(GameTestHelper h,ResourceKey<Biome> upper){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(8197,2,8197));
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var top=registry.getHolderOrThrow(upper);var bottom=registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME,net.minecraft.resources.ResourceLocation.parse("omnira:folded_caverns")));
        for(int x=(p.getX()>>4)-1;x<=(p.getX()>>4)+1;x++)for(int z=(p.getZ()>>4)-1;z<=(p.getZ()>>4)+1;z++)
            level.getChunk(x,z).fillBiomesFromNoise((a,y,c,s)->y*4>=112?top:bottom,level.getChunkSource().randomState().sampler());
        return new BlockPos(p.getX(),160,p.getZ());
    }
    @GameTest(template="spell_arena") public static void gardenDoesNotAffectCaveBelow(GameTestHelper h){
        var top=layers(h,RecurrenceGarden.BIOME);var low=top.below(96);var l=h.getLevel();
        for(var p:new BlockPos[]{top,low})l.setBlockAndUpdate(p,DreamContent.TEMPORAL_SILT.get().defaultBlockState());
        RecurrenceGarden.pulseChunk(l,top.getX()>>4,top.getZ()>>4);
        h.assertTrue(l.getBlockState(top).is(TimeNatureContent.SILT.get()),"Elevated garden did not cycle");
        h.assertTrue(l.getBlockState(low).is(DreamContent.TEMPORAL_SILT.get()),"Garden leaked into cave");h.succeed();
    }
    @GameTest(template="spell_arena") public static void localAccelerationAndFreeze(GameTestHelper h){
        var top=layers(h,FleetingTime.BIOME);var l=h.getLevel();
        h.assertTrue(FleetingTime.accelerated(l,top)&&!FleetingTime.accelerated(l,top.below(96)),"Acceleration leaked vertically");
        top=layers(h,StilledTime.BIOME);
        h.assertTrue(StilledTime.stopped(l,top)&&!StilledTime.stopped(l,top.below(96)),"Freeze leaked vertically");h.succeed();
    }
    @GameTest(template="spell_arena") public static void treeFootprintCannotCrossBiomes(GameTestHelper h){
        var root=layers(h,MireCycle.BIOME);var outside=root.below(96);var l=h.getLevel();
        l.setBlockAndUpdate(root,TimeNatureContent.LOG.get().defaultBlockState());l.setBlockAndUpdate(outside,TimeNatureContent.LEAVES.get().defaultBlockState());
        var trees=com.mcmagic.omnira.mire.MireTrees.get(l);trees.register(root,java.util.List.of(root,outside));trees.pulse(l,6000);
        h.assertTrue(l.getBlockState(root).is(TimeNatureContent.LOG.get())&&l.getBlockState(outside).is(TimeNatureContent.LEAVES.get()),"Cross-boundary tree was partially removed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void mireRecordsOnlyItsVolume(GameTestHelper h){
        var top=layers(h,MireCycle.BIOME);var low=top.below(96);
        var a=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(4,3,4));var b=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(6,3,6));
        a.setNoAi(true);b.setNoAi(true);a.moveTo(top.getX()+.5,top.getY(),top.getZ()+.5);b.moveTo(low.getX()+.5,low.getY(),low.getZ()+.5);
        MireCycle.update(a,4800);MireCycle.update(b,4800);
        a.setPos(a.getX()+2,a.getY(),a.getZ());b.setPos(b.getX()+2,b.getY(),b.getZ());
        MireCycle.update(a,6000);MireCycle.update(b,6000);
        h.assertTrue(Math.abs(a.getX()-top.getX()-.5)<.01&&a.isBaby(),"Mire rewind missing");
        h.assertTrue(Math.abs(b.getX()-low.getX()-2.5)<.01&&!b.isBaby(),"Mire affected cave occupant");a.discard();b.discard();h.succeed();
    }
}
