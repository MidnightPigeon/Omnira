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
    @GameTest(template="spell_arena") public static void missingTickerPositionUsesNormalTime(GameTestHelper h){
        var level=h.getLevel();
        h.assertFalse(StilledTime.blockTickStopped(level,null),"A ticker without a position was frozen");
        h.assertFalse(FleetingTime.accelerated(level,null),"A ticker without a position received an extra tick");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void machineSamplesFollowBiomeEdits(GameTestHelper h){
        var level=h.getLevel();
        var samples=new MachineBiomeSamples(level.getBiomeManager());
        var top=layers(h,FleetingTime.BIOME);
        h.assertTrue(samples.matches(level,top,FleetingTime.BIOME),"Initial sample missed the woods");
        // Include the blended boundary rather than testing only uniform chunk interiors.
        for(int y=108;y<116;y++)for(int x=-6;x<=6;x++){
            var pos=new BlockPos(top.getX()+x,y,top.getZ());
            for(int pass=0;pass<2;pass++)
                h.assertTrue(samples.matches(level,pos,FleetingTime.BIOME)==level.getBiome(pos).is(FleetingTime.BIOME),"Sample disagreed with vanilla");
        }
        layers(h,StilledTime.BIOME);
        h.assertTrue(samples.matches(level,top,StilledTime.BIOME)&&!samples.matches(level,top,FleetingTime.BIOME),"Biome edit left stale machine time");
        h.assertFalse(samples.matches(level,null,StilledTime.BIOME),"Missing ticker position matched a biome");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void machineTimeRegionsFollowLifetime(GameTestHelper h){
        var level=h.getLevel();
        var pos=h.absolutePos(new BlockPos(2051,4,2051));
        var chunk=level.getChunkAt(pos);
        var regions=((MachineTimeRegions.Owner)level).omnira$timeRegions();
        var section=chunk.getSection(chunk.getSectionIndex(pos.getY()));
        var sampler=level.getChunkSource().randomState().sampler();
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var woods=registry.getHolderOrThrow(FleetingTime.BIOME);
        var plains=registry.getHolderOrThrow(Biomes.PLAINS);
        h.assertFalse(regions.nearby(pos),"Ordinary machines would enter exact sampling");
        section.fillBiomesFromNoise((x,y,z,s)->woods,sampler,0,0,0);
        h.assertTrue(regions.nearby(pos)&&regions.nearby(pos.offset(16,16,16)),"Missing edited region or diagonal boundary");
        h.assertFalse(regions.nearby(pos.offset(32,0,0)),"Distant machines would enter exact sampling");
        MachineTimeRegions.track(chunk,null);
        h.assertFalse(regions.nearby(pos),"Unloaded region remained active");
        MachineTimeRegions.track(chunk,regions);
        h.assertTrue(regions.nearby(pos),"Reloaded region missed its biome");
        section.fillBiomesFromNoise((x,y,z,s)->plains,sampler,0,0,0);
        h.assertFalse(regions.nearby(pos),"Restoring ordinary biome left machines active");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void machineCanChangeItsBiomeDuringTick(GameTestHelper h){
        var level=h.getLevel();
        var origin=h.absolutePos(BlockPos.ZERO);
        var center=new BlockPos((origin.getX()&~15)+8,(origin.getY()&~15)+8,(origin.getZ()&~15)+8);
        var chunk=level.getChunkAt(center);
        var section=chunk.getSection(chunk.getSectionIndex(center.getY()));
        var original=section.getBiomes();
        var sampler=level.getChunkSource().randomState().sampler();
        var woods=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(FleetingTime.BIOME);
        var times=new java.util.ArrayList<Long>();
        level.addBlockEntityTicker(new net.minecraft.world.level.block.entity.TickingBlockEntity(){
            public void tick(){
                times.add(level.getGameTime());
                if(times.size()==1)section.fillBiomesFromNoise((x,y,z,s)->woods,sampler,0,0,0);
                if(times.size()==2)section.fillBiomesFromNoise((x,y,z,s)->original.get(x&3,y&3,z&3),sampler,0,0,0);
            }
            public boolean isRemoved(){return times.size()>=4;}
            public BlockPos getPos(){return center;}
            public String getType(){return "omnira:machine_time_test";}
        });
        h.runAfterDelay(8,()->{
            h.assertTrue(times.size()==4 && times.get(0).equals(times.get(1)) && times.get(2)>times.get(1) && times.get(3)>times.get(2),
                    "Ticker did not immediately enter and leave accelerated time: "+times);
            h.succeed();
        });
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
