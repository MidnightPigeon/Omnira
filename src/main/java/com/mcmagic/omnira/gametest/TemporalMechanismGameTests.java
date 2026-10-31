package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.spacetime.TimeWarpEffects;
import com.mcmagic.omnira.time.StilledTime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@GameTestHolder("omnira_temporal_mechanisms")
@PrefixGameTestTemplate(false)
public final class TemporalMechanismGameTests {
    @GameTest(template="spell_arena",timeoutTicks=140)
    public static void nativeWarpHazardAndRegrowthSurviveBiomeFreeze(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,5,4));
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(StilledTime.BIOME);
        level.getChunkAt(p).fillBiomesFromNoise((x,y,z,s)->biome,level.getChunkSource().randomState().sampler());
        level.setBlockAndUpdate(p,ModBlocks.TIME_WARP_POINT.get().defaultBlockState());
        var mote=p.above(2);level.setBlockAndUpdate(mote,ModBlocks.TEMPORAL_MOTE.get().defaultBlockState());
        // onPlace already schedules 6000 ticks; replace that test fixture entry, not the game rule.
        level.getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(mote));
        level.scheduleTick(mote,ModBlocks.TEMPORAL_MOTE.get(),1);
        var cow=EntityType.COW.create(level);cow.moveTo(p.getCenter().add(1,0,0));cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);
        double original=cow.getAttributeValue(Attributes.MOVEMENT_SPEED);
        h.runAfterDelay(85,()->{
            h.assertTrue(level.getBlockState(mote).is(ModBlocks.TIME_WARP_POINT.get()),"Native temporal mote never regrew in its own biome");
            h.assertTrue(Math.abs(cow.getAttributeValue(Attributes.MOVEMENT_SPEED)/original-.1)<.001,"Native warp hazard did not reach its 10% floor");
            level.removeBlock(p,false);level.removeBlock(mote,false);
        });
        h.runAfterDelay(90,()->{
            h.assertTrue(Math.abs(cow.getAttributeValue(Attributes.MOVEMENT_SPEED)-original)<.001,"Warp modifiers remained after source removal");
            h.assertTrue(StilledTime.stopped(level,p),"Fix disabled the biome's ordinary machine stasis");h.succeed();
        });
    }
    @GameTest(template="spell_arena")
    public static void repeatedSlowdownPreservesPhysicalVelocity(GameTestHelper h){
        var level=h.getLevel();var p=h.absoluteVec(new Vec3(4,8,4));
        var item=new ItemEntity(level,p.x,p.y,p.z,new ItemStack(Items.STICK));
        item.setDeltaMovement(.3,.1,-.2);var original=item.getDeltaMovement();long now=level.getGameTime();
        TimeWarpEffects.touch(item,now-72);TimeWarpEffects.touch(item,now);
        for(int i=0;i<100;i++)TimeWarpEffects.tick(new EntityTickEvent.Post(item));
        h.assertTrue(item.getDeltaMovement().equals(original),"Warp repeatedly multiplied physical velocity toward zero");
        h.assertTrue(Math.abs(TimeWarpEffects.tickFactor(item)-.1)<.001,"Non-living slowdown lost its 10% floor");
        item.setDeltaMovement(1,0,0);TimeWarpEffects.tick(new EntityTickEvent.Post(item));
        h.assertTrue(item.getDeltaMovement().equals(new Vec3(1,0,0)),"Warp erased an external impulse");
        TimeWarpEffects.touch(item,now-2);
        h.assertTrue(TimeWarpEffects.tickFactor(item)==1,"Expired exposure left simulation throttled");h.succeed();
    }
}
