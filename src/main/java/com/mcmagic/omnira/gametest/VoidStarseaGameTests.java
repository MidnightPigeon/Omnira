package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.sea.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.time.TemporalCreatureTags;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_void_starsea") @PrefixGameTestTemplate(false)
public final class VoidStarseaGameTests {
    @GameTest(template="spell_arena") public static void heldSpatialCrystalAttractsManta(GameTestHelper h){
        var p=player(h);var manta=VoidSeaContent.MANTA.get().create(h.getLevel());
        manta.setPos(p.position().add(4,0,0));
        h.assertTrue(manta.attraction()==null,"Empty hands attracted manta");
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(TimeNatureContent.SPATIAL_SHARD.get()));
        h.assertTrue(manta.attraction()!=null,"Offhand spatial crystal did not attract manta");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TimeNatureContent.SPATIAL_SHARD.get()));
        h.assertTrue(manta.attraction()!=null,"Mainhand spatial crystal did not attract manta");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.AMETHYST_SHARD));
        h.assertTrue(manta.attraction()==null,"Unrelated crystal attracted manta");
        p.discard();h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"star-test");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),profile).connection;p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(h.absoluteVec(new Vec3(5,4,5)));h.getLevel().addNewPlayer(p);return p;
    }
    @GameTest(template="spell_arena") public static void speciesTagsAndIndependentTimeHazards(GameTestHelper h){
        var jelly=VoidSeaContent.JELLYFISH.get().create(h.getLevel());var manta=VoidSeaContent.MANTA.get().create(h.getLevel());
        h.assertTrue(jelly.getMaxHealth()==6&&manta.getMaxHealth()==40,"Species health changed");
        for(var e:java.util.List.of(jelly,manta)){
            h.assertTrue(SpatialImmunity.fissure(e)&&SpatialImmunity.starflow(e),"Space immunity absent");
            h.assertTrue(!TemporalCreatureTags.timeImmune(e)&&!com.mcmagic.omnira.mire.MireFluidEffects.immune(e),"Spatial species wrongly immune to time hazards");
            h.assertTrue(e.getTarget()==null&&e.canBreatheUnderwater(),"Initial hostility or drowning");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void minimumPoolAndHazardousWater(GameTestHelper h){
        var e=VoidSeaContent.MANTA.get().create(h.getLevel());var p=h.absolutePos(new BlockPos(5,3,5));
        h.getLevel().setBlockAndUpdate(p,Blocks.WATER.defaultBlockState());
        h.assertTrue(!e.attractingWater(p),"Single bucket attracts creatures");
        for(BlockPos q:BlockPos.betweenClosed(p.offset(-1,0,-1),p.offset(1,1,1)))h.getLevel().setBlock(q,Blocks.WATER.defaultBlockState(),2);
        h.assertTrue(e.attractingWater(p),"3x3x2 water rejected");
        h.getLevel().setBlock(p.offset(1,1,1),Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(!e.attractingWater(p),"Incomplete volume attracts creatures");
        for(BlockPos q:BlockPos.betweenClosed(p.offset(-1,0,-1),p.offset(1,1,1)))h.getLevel().setBlock(q,com.mcmagic.omnira.mire.MireContent.LIQUID.get().defaultBlockState(),2);
        h.assertTrue(!e.attractingWater(p)&&!e.preferredFluid(com.mcmagic.omnira.mire.MireContent.SOURCE.get().defaultFluidState()),"Decayed timeflow attracts creatures");
        for(BlockPos q:BlockPos.betweenClosed(p.offset(-1,0,-1),p.offset(1,1,1)))h.getLevel().setBlock(q,VoidSeaContent.LIQUID.get().defaultBlockState(),2);
        h.assertTrue(e.attractingWater(p),"Starflow rejected");h.succeed();
    }
    @GameTest(template="spell_arena") public static void ridingDoesNotShareFissureImmunity(GameTestHelper h){
        var p=player(h);var manta=VoidSeaContent.MANTA.get().create(h.getLevel());manta.tame(p);
        h.assertTrue(p.startRiding(manta),"Owner cannot ride");
        h.assertTrue(SpatialImmunity.starflow(p)&&!SpatialImmunity.fissure(p)&&SpatialImmunity.fissure(manta),"Rider inherited fissure immunity");
        p.setHealth(19);VoidFissureBlock.affect(p);h.assertTrue(p.getHealth()==9,"Mounted player escaped true damage");
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("charm",0,new ItemStack(ModItems.SPACETIME_ANCHORING_SIGIL.get()));
        VoidFissureBlock.affect(p);h.assertTrue(p.getHealth()==9&&SpatialImmunity.fissure(p),"Worn sigil failed");
        p.stopRiding();h.succeed();
    }
    @GameTest(template="spell_arena") public static void damageBypassesArmorAbsorptionAndInvulnerability(GameTestHelper h){
        var p=EntityType.COW.create(h.getLevel());p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(10);p.setHealth(20);p.setAbsorptionAmount(10);p.invulnerableTime=20;p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.NETHERITE_CHESTPLATE));
        var manta=VoidSeaContent.MANTA.get().create(h.getLevel());
        boolean hit=manta.doHurtTarget(p);
        h.assertTrue(hit&&p.getHealth()==10&&p.getAbsorptionAmount()==10,"Manta true damage: hit="+hit+", health="+p.getHealth()+", absorption="+p.getAbsorptionAmount());
        VoidSeaEffects.trueDamage(p,1,p.damageSources().genericKill());h.assertTrue(p.getHealth()==9,"Fluid true damage hit invulnerability frames");h.succeed();
    }
    @GameTest(template="spell_arena") public static void fissureRoundsRemainingDurability(GameTestHelper h){
        var p=player(h);p.setHealth(19);
        var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(sword.getMaxDamage()-5);p.setItemSlot(EquipmentSlot.MAINHAND,sword);
        var helmet=new ItemStack(Items.IRON_HELMET);helmet.setDamageValue(helmet.getMaxDamage()-1);p.setItemSlot(EquipmentSlot.HEAD,helmet);
        VoidFissureBlock.affect(p);
        h.assertTrue(p.getHealth()==9&&sword.getMaxDamage()-sword.getDamageValue()==2,"Half current amount did not round up");
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"Last durability point did not break");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=80) public static void contactOnlyOnceUntilReentry(GameTestHelper h){
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));l.setBlockAndUpdate(pos,VoidSeaContent.FISSURE.get().defaultBlockState());
        var cow=EntityType.COW.create(l);cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);cow.setHealth(40);cow.moveTo(Vec3.atCenterOf(pos));l.addFreshEntity(cow);
        for(int i=1;i<=18;i++)h.runAfterDelay(i,()->l.getBlockState(pos).entityInside(l,pos,cow));
        h.runAfterDelay(8,()->{h.assertTrue(cow.getHealth()==20,"Contact repeated or absent: "+cow.getHealth());cow.setPos(Vec3.atCenterOf(pos.offset(3,0,0)));});
        h.runAfterDelay(12,()->cow.setPos(Vec3.atCenterOf(pos)));
        h.runAfterDelay(18,()->{h.assertTrue(cow.getHealth()==10,"Reentry did not retrigger");cow.discard();h.succeed();});
    }
    @GameTest(template="spell_arena") public static void liquidOrbExternalInfiniteManualFinite(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(4,3,4));h.getLevel().setBlockAndUpdate(p,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var ball=(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)h.getLevel().getBlockEntity(p);
        var action=net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        ball.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(VoidSeaContent.SOURCE.get(),1000),action);
        h.assertTrue(ball.infinite(),"Starflow external extraction is finite");
        var handler=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,p,Direction.DOWN);
        h.runAfterDelay(20,()->{h.assertTrue(handler.drain(250,action).getAmount()==250&&ball.tank.getFluidAmount()==1000,"External extraction consumed reserve");ball.tank.drain(250,action);h.assertTrue(ball.tank.getFluidAmount()==750,"Manual tank ignores actual volume");h.succeed();});
    }
    @GameTest(template="spell_arena") public static void jellyfishBurstDoesNotHarmItself(GameTestHelper h){
        var l=h.getLevel();var jelly=VoidSeaContent.JELLYFISH.get().create(l);jelly.setNoAi(true);jelly.moveTo(h.absoluteVec(new Vec3(4,4,4)));l.addFreshEntity(jelly);
        var cow=EntityType.COW.create(l);cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);cow.setHealth(40);cow.setNoAi(true);cow.moveTo(jelly.position().add(2,0,0));l.addFreshEntity(cow);
        jelly.burst();h.assertTrue(jelly.getHealth()==6&&jelly.isAlive()&&cow.getHealth()==32,"Incorrect area damage or self damage");jelly.discard();cow.discard();h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void neutralUntilProvoked(GameTestHelper h){
        var l=h.getLevel();var j=VoidSeaContent.JELLYFISH.get().create(l);var m=VoidSeaContent.MANTA.get().create(l);var cow=EntityType.COW.create(l);
        j.moveTo(h.absoluteVec(new Vec3(5,4,5)));m.moveTo(j.position().add(3,0,0));cow.moveTo(j.position().add(0,0,3));cow.setNoAi(true);
        l.addFreshEntity(j);l.addFreshEntity(m);l.addFreshEntity(cow);
        h.runAfterDelay(5,()->{h.assertTrue(j.getTarget()==null&&m.getTarget()==null,"Unprovoked aggression");j.hurt(j.damageSources().indirectMagic(cow,cow),1);});
        h.runAfterDelay(12,()->{h.assertTrue(j.getTarget()==cow,"Jellyfish did not retaliate against attacker");j.discard();m.discard();cow.discard();h.succeed();});
    }
    @GameTest(template="spell_arena",batch="void_migration") public static void relocationPreservesFluidAndBiomeBounds(GameTestHelper h){
        var l=h.getLevel();var old=h.absolutePos(new BlockPos(5,3,5));
        var chunk=l.getChunkAt(old);var original=l.getBiome(old);var biome=l.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(VoidSeaContent.BIOME);
        chunk.fillBiomesFromNoise((x,y,z,s)->biome,l.getChunkSource().randomState().sampler());
        for(var p:BlockPos.betweenClosed(old.offset(-3,0,-3),old.offset(3,2,3)))l.setBlock(p,VoidSeaContent.LIQUID.get().defaultBlockState(),2);
        l.setBlock(old,VoidSeaContent.FISSURE.get().defaultBlockState().setValue(VoidFissureBlock.FLUID_LEVEL,0),2);
        boolean moved=false;var random=net.minecraft.util.RandomSource.create(741);
        for(int i=0;i<80&&!moved;i++)moved=VoidFissureBlock.relocate(l,old,random);
        chunk.fillBiomesFromNoise((x,y,z,s)->original,l.getChunkSource().randomState().sampler());
        h.assertTrue(moved&&l.getBlockState(old).is(VoidSeaContent.LIQUID.get()),"Fissure failed to move or destroyed source fluid");
        h.assertTrue(!VoidFissureBlock.destination(l,old.above(20)),"Destination above surface permitted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void terrainContainsOceanWithoutTouchingUpperStratum(GameTestHelper h){
        boolean found=false;
        for(int x=-1024;x<=1024&&!found;x+=16)for(int z=-512;z<=512&&!found;z+=16){
            if(!VoidSeaTerrain.biome(x,z)||VoidSeaTerrain.floor(x,z)>=VoidSeaTerrain.SEA_LEVEL)continue;
            if(!VoidSeaTerrain.block(x,VoidSeaTerrain.SEA_LEVEL,z).is(VoidSeaContent.LIQUID.get()))continue;
            found=true;
            h.assertTrue(com.mcmagic.omnira.spacetime.CorridorLayout.block(x,VoidSeaTerrain.SEA_LEVEL,z).is(VoidSeaContent.LIQUID.get()),"Generator did not use sea terrain");
            h.assertTrue(VoidSeaTerrain.SEA_LEVEL==40&&VoidSeaTerrain.floor(x,z)>=24,"Ocean outside cave height range");
            h.assertTrue(com.mcmagic.omnira.spacetime.CorridorLayout.block(x,103,z).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Space ceiling breached");
            h.assertTrue(!com.mcmagic.omnira.spacetime.CorridorLayout.block(x,200,z).is(VoidSeaContent.LIQUID.get()),"Sea leaked to upper continent");
        }
        h.assertTrue(found,"No spatial ocean generated");h.succeed();
    }
    @GameTest(template="spell_arena") public static void oldBiomeSourceReceivesStarseaAndLootIsSpatial(GameTestHelper h){
        try(var reader=new java.io.InputStreamReader(VoidStarseaGameTests.class.getResourceAsStream("/data/omnira/dimension/spacetime_corridor.json"),java.nio.charset.StandardCharsets.UTF_8)){
            var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");json.remove("starsea_biome");
            var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,h.getLevel().registryAccess());
            var source=com.mcmagic.omnira.spacetime.StrataBiomeSource.CODEC.codec().parse(ops,json).getOrThrow();
            h.assertTrue(source.possibleBiomes().stream().anyMatch(b->b.is(VoidSeaContent.BIOME)),"Old source silently kept corridor fallback");
            var tables=com.mcmagic.omnira.config.OmniraLootConfig.defaults().getAsJsonObject("treasureTables");
            String fish=tables.get("omnira:fishing/starflow_fish").toString(),treasure=tables.get("omnira:fishing/starflow_treasure").toString();
            h.assertTrue(fish.contains("star_jellyfish")&&!fish.contains("void_manta")&&!fish.contains("timeflow_eel")&&treasure.contains("space_microcore")&&!treasure.contains("time_microcore"),"Incorrect spatial fishing pool");h.succeed();
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
    }
    @GameTest(template="spell_arena") public static void nativeStarflowBucketPreservesJellyfishHealth(GameTestHelper h){
        var p=player(h);var jelly=VoidSeaContent.JELLYFISH.get().create(h.getLevel());jelly.setHealth(3);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(VoidSeaContent.BUCKET.get()));
        h.assertTrue(jelly.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction()&&jelly.isRemoved(),"Starflow bucket did not collect fish");
        var filled=p.getMainHandItem();h.assertTrue(filled.is(VoidSeaContent.JELLY_BUCKET.get()),"Wrong filled bucket");
        var next=VoidSeaContent.JELLYFISH.get().create(h.getLevel());next.loadFromBucketTag(filled.get(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA).copyTag());
        h.assertTrue(next.getHealth()==3,"Bucket reset fish health");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=60) public static void mountContactStillHitsRiderBeforeMigration(GameTestHelper h){
        var p=player(h);var l=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        l.setBlockAndUpdate(pos,VoidSeaContent.FISSURE.get().defaultBlockState());
        var manta=VoidSeaContent.MANTA.get().create(l);manta.setNoAi(true);manta.moveTo(Vec3.atCenterOf(pos));manta.tame(p);l.addFreshEntity(manta);p.startRiding(manta);p.setHealth(19);
        l.getBlockState(pos).entityInside(l,pos,manta);
        h.runAfterDelay(4,()->{h.assertTrue(manta.getHealth()==40&&p.getHealth()==9,"Mount contact shielded rider: "+p.getHealth());p.stopRiding();manta.discard();h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=60) public static void jellyfishPursuitMovesHorizontallyAndVertically(GameTestHelper h){
        var l=h.getLevel();var jelly=VoidSeaContent.JELLYFISH.get().create(l);var cow=EntityType.COW.create(l);
        Vec3 start=h.absoluteVec(new Vec3(3,4,3));jelly.moveTo(start);cow.setNoAi(true);cow.setNoGravity(true);cow.moveTo(start.add(5,3,5));l.addFreshEntity(jelly);l.addFreshEntity(cow);jelly.setTarget(cow);
        h.runAfterDelay(16,()->{h.assertTrue(jelly.getY()>start.y+.1&&jelly.getX()>start.x+.1&&jelly.isNoGravity(),"Flying pursuit did not move on both axes");jelly.discard();cow.discard();h.succeed();});
    }
}
