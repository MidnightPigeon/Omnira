package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.config.OmniraLootConfig;
import com.mcmagic.omnira.mire.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.time.TimePlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_mill_update")
@PrefixGameTestTemplate(false)
public final class MillUpdateGameTests {
    @GameTest(template="spell_arena")
    public static void dryPlantSites(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,5,5));
        for(var soil:List.of(TimeNatureContent.SILT.get(),MireContent.SILT.get(),MireContent.LIQUID.get(),Blocks.WATER)){
            l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p.below(),soil.defaultBlockState());
            boolean dry=soil==TimeNatureContent.SILT.get()||soil==MireContent.SILT.get();
            h.assertTrue(TimePlantBlock.canGrowAt(l,p)==dry,"Grass site admitted liquid below");
            h.assertTrue(TimeNatureContent.GRASS.get().defaultBlockState().canSurvive(l,p)==dry,"Grass survival admitted liquid below");
        }
        l.setBlockAndUpdate(p.below(),TimeNatureContent.SILT.get().defaultBlockState());l.setBlockAndUpdate(p,MireContent.LIQUID.get().defaultBlockState());
        h.assertTrue(!TimePlantBlock.canGrowAt(l,p),"Grass replaced liquid");h.succeed();
    }
    private static void set(FishingHook hook,String name,int value)throws Exception{var f=FishingHook.class.getDeclaredField(name);f.setAccessible(true);f.setInt(hook,value);}
    private static int get(FishingHook hook,String name)throws Exception{var f=FishingHook.class.getDeclaredField(name);f.setAccessible(true);return f.getInt(hook);}
    @GameTest(template="spell_arena")
    public static void timeflowFishingStages(GameTestHelper h)throws Exception{
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(8,4,8));
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)l.setBlockAndUpdate(p.offset(x,0,z),MireContent.LIQUID.get().defaultBlockState());
        var player=new net.neoforged.neoforge.common.util.FakePlayer(l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ripple-check"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.FISHING_ROD));player.moveTo(p.getCenter().add(1,1,1));
        var hook=new FishingHook(player,l,0,0);hook.moveTo(p.getX()+.5,p.getY()+.7,p.getZ()+.5);hook.tickCount=16;
        var method=FishingHook.class.getDeclaredMethod("catchingFish",BlockPos.class);method.setAccessible(true);
        set(hook,"timeUntilLured",200);method.invoke(hook,p);h.assertTrue(get(hook,"timeUntilLured")<=200&&get(hook,"timeUntilLured")>=198,"Waiting timer changed");
        set(hook,"timeUntilLured",0);set(hook,"timeUntilHooked",30);method.invoke(hook,p);
        h.assertTrue(get(hook,"timeUntilHooked")>0&&get(hook,"timeUntilHooked")<=30,"Approach timer changed");
        set(hook,"timeUntilHooked",1);for(int i=0;i<32&&get(hook,"nibble")==0;i++)method.invoke(hook,p);
        h.assertTrue(get(hook,"nibble")>=20&&get(hook,"nibble")<=40,"Bite window changed");
        h.assertTrue(MireFishingEffects.surface(MireContent.LIQUID.get().defaultBlockState())&&MireFishingEffects.surface(Blocks.WATER.defaultBlockState()),"Surface check broken");
        h.assertTrue(!MireFishingEffects.surface(Blocks.STONE.defaultBlockState()),"Trail allowed on land");
        h.assertTrue(MireFishingEffects.particle(ParticleTypes.BUBBLE)==ModParticles.TIMEFLOW_BUBBLE.get(),"Bubble not replaced");
        h.assertTrue(MireFishingEffects.particle(ParticleTypes.SPLASH)==ModParticles.TIMEFLOW_RIPPLE.get()&&MireFishingEffects.particle(ParticleTypes.FISHING)==ModParticles.TIMEFLOW_RIPPLE.get(),"Wake/splash not replaced");
        hook.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void millNamesLootAndLegacy(GameTestHelper h)throws Exception{
        var config=new JsonObject();var customized=new JsonObject();customized.addProperty("customized",true);config.add("omnira:chests/reversion_mill_cellar",customized);
        h.assertTrue(OmniraLootConfig.migrateTableIds(config)&&config.get("omnira:chests/rusty_lake_mill_cellar").equals(customized),"Legacy config lost custom values");
        JsonObject plan;try(var stream=Objects.requireNonNull(MillUpdateGameTests.class.getResourceAsStream("/data/omnira/structures/rusty_lake_mill_plan.json"));var reader=new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)){plan=JsonParser.parseReader(reader).getAsJsonObject();}
        h.assertTrue(plan.get("id").getAsString().equals("omnira:rusty_lake_mill"),"Structure ID not renamed");
        var cells=new HashMap<BlockPos,JsonObject>();for(var v:plan.getAsJsonArray("cells")){var c=v.getAsJsonObject();cells.put(new BlockPos(c.get("x").getAsInt(),c.get("y").getAsInt(),c.get("z").getAsInt()),c);}
        for(var e:cells.entrySet())if(e.getValue().get("name").getAsString().equals("omnira:time_grass")){
            var below=cells.get(e.getKey().below());h.assertTrue(below!=null&&below.get("name").getAsString().endsWith("temporal_silt"),"Template has floating grass");
        }
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(5,4,5));l.setBlockAndUpdate(pos,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        var ball=(CrystalBallBlockEntity)l.getBlockEntity(pos);var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for(String suffix:List.of("cellar","workshop")){
            var tag=new CompoundTag();tag.putString("LootTable","omnira:chests/reversion_mill_"+suffix);ball.loadWithComponents(tag,l.registryAccess());
            h.assertTrue(ball.hasPendingLoot()&&ball.getLootTable().location().getPath().equals("chests/rusty_lake_mill_"+suffix),"Legacy ball missing mist or table");
            ball.createMenu(1,player.getInventory(),player);h.assertTrue(!ball.hasPendingLoot()&&!ball.isEmpty(),"Renamed treasure cannot be opened");
        }
        h.succeed();
    }
}
