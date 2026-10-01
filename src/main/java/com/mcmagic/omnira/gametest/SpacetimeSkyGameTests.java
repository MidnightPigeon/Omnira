package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spacetime.SpacetimeSky;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_sky")
@PrefixGameTestTemplate(false)
public final class SpacetimeSkyGameTests {
    @GameTest(template="spell_arena",timeoutTicks=400)
    public static void constantSkyAndOcclusion(GameTestHelper h) {
        var level=h.getLevel().getServer().getLevel(ModDimensions.SPACETIME_CORRIDOR);
        h.assertTrue(level!=null,"Missing test dimension (requires dreamTestData)");
        h.assertTrue(level.dimensionType().hasSkyLight(),"Skylight disabled");
        h.assertTrue(SpacetimeSky.applies(level),"Wrong effects registration");
        long before=level.getDayTime();
        for(long time:new long[]{0,6000,12000,18000,42000}) {
            level.setDayTime(time);level.updateSkyBrightness();
            h.assertTrue(level.getSkyDarken()==5,"Time changed effective sky light");
        }
        level.setDayTime(before);
        // No player keeps this secondary dimension loaded during a headless test.
        level.setChunkForced(0,0,true);
        level.getChunk(0,0);
        BlockPos open=new BlockPos(8,254,8),covered=new BlockPos(8,250,8);
        for(int x=1;x<=15;x++)for(int z=1;z<=15;z++)level.setBlockAndUpdate(new BlockPos(x,251,z),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(covered,Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(open,Blocks.AIR.defaultBlockState());
        var lightEngine=level.getChunkSource().getLightEngine();
        var pending=new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<?>>(lightEngine.waitForPendingTasks(0,0));
        lightEngine.tryScheduleUpdate();
        h.startSequence().thenWaitUntil(()->h.assertTrue(pending.get().isDone(),"Waiting for roof lighting"))
                .thenExecute(()->{
            h.assertTrue(level.getBlockState(covered.above()).is(Blocks.STONE),"Test roof was not retained");
            h.assertTrue(level.getMaxLocalRawBrightness(open)==10,"Exposed effective sky light is not 10");
            int sky=level.getBrightness(net.minecraft.world.level.LightLayer.SKY,covered);
            h.assertTrue(SpacetimeSky.effectiveLight(sky)<10,"Roof does not block sky light: raw="+sky);
            level.setBlockAndUpdate(covered,Blocks.GLOWSTONE.defaultBlockState());
            pending.set(lightEngine.waitForPendingTasks(0,0));lightEngine.tryScheduleUpdate();
        }).thenWaitUntil(()->h.assertTrue(pending.get().isDone(),"Waiting for emitted block light"))
                .thenExecute(()->{
            int emitted=level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,covered);
            h.assertTrue(emitted==15,"Expected glowstone block light 15, actual="+emitted);
            h.assertTrue(level.getMaxLocalRawBrightness(covered)==15,"Block lights were capped");
            level.setChunkForced(0,0,false);
        }).thenSucceed();
    }

    @GameTest(template="spell_arena")
    public static void oldChunkLightingMigration(GameTestHelper h) {
        var tag=new CompoundTag();tag.putBoolean("isLightOn",true);
        var sections=new ListTag();var section=new CompoundTag();section.putString("block_states","preserved");
        section.putByteArray("BlockLight",new byte[]{1});sections.add(section);tag.put("sections",sections);
        tag.putString("block_entities","preserved");
        SpacetimeSky.prepareChunkLighting(tag);
        h.assertTrue(!tag.getBoolean("isLightOn")&&!section.contains("BlockLight"),"Old lighting still trusted");
        h.assertTrue(section.getString("block_states").equals("preserved")&&tag.getString("block_entities").equals("preserved"),"Migration altered terrain or contents");
        tag.putInt("omnira:sky_lighting_version",1);tag.putBoolean("isLightOn",true);
        SpacetimeSky.prepareChunkLighting(tag);
        h.assertTrue(tag.getBoolean("isLightOn"),"Migrated chunk unnecessarily invalidated");
        h.assertTrue(SpacetimeSky.effectiveLight(15)==10&&SpacetimeSky.effectiveLight(0)==0,"Bad light range");h.succeed();
    }
}
