package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.mire.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.time.*;
import com.mcmagic.omnira.spacetime.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_mire")
@PrefixGameTestTemplate(false)
public final class ReversionMireGameTests {
    private static void biome(GameTestHelper h,BlockPos pos,ResourceLocation id){
        var l=h.getLevel();var value=l.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME,id));
        l.getChunkAt(pos).fillBiomesFromNoise((x,y,z,s)->value,l.getChunkSource().randomState().sampler());
    }
    @GameTest(template="spell_arena",templateNamespace="omnira_landscape")
    public static void terrainAndContent(GameTestHelper h){
        int ponds=0,land=0;int[] regions=new int[6];
        for(int cx=-6;cx<6;cx++)for(int cz=-6;cz<6;cz++)regions[StrataBiomeSource.region(cx*256,cz*256)]++;
        for(int count:regions)h.assertTrue(count>10&&count<40,"Unequal biome shares");
        for(int x=-512;x<512;x+=8)for(int z=-512;z<512;z+=8)if(StrataBiomeSource.reversionMire(x,z)){
            var c=MireTerrain.column(x,z);h.assertTrue(c.floor()>=220&&c.floor()<=225,"Mire elevation escaped band");
            if(c.pond()){
                ponds++;h.assertTrue(CorridorLayout.block(x,223,z).is(MireContent.LIQUID.get()),"Missing pond surface");
                h.assertTrue(CorridorLayout.block(x,c.floor(),z).is(MireContent.SILT.get()),"Missing rotted pond bed");
            }else land++;
        }
        h.assertTrue(ponds>100&&land>100,"Missing flooded lowlands or dry islands");
        h.assertTrue(MireContent.SILT.get().defaultBlockState().is(TemporalSoils.SILTS),"Rotted soil tag missing");
        h.assertTrue(MireContent.SOURCE.get().defaultFluidState().is(net.minecraft.tags.FluidTags.WATER),"Fish/fishing water behavior missing");
        var level=h.getLevel();var regs=level.registryAccess().registryOrThrow(Registries.BIOME);
        var source=new StrataBiomeSource(regs.getHolderOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:stilled_wastes"))),regs.getHolderOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:temporal_continent"))),regs.getHolderOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:spacetime_corridor"))),java.util.Optional.of(regs.getHolderOrThrow(FleetingTime.BIOME)),java.util.Optional.of(regs.getHolderOrThrow(MireCycle.BIOME)));
        h.assertTrue(source.possibleBiomes().stream().anyMatch(b->b.is(MireCycle.BIOME)),"Biome source omitted mire");
        var mb=regs.getHolderOrThrow(MireCycle.BIOME).value();for(var category:MobCategory.values())h.assertTrue(mb.getMobSettings().getMobs(category).isEmpty(),"Unexpected vanilla spawn entries");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void soilAndLily(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,3,4));biome(h,p,MireCycle.BIOME.location());
        for(boolean living:new boolean[]{false,true})for(var face:Direction.values()){
            for(var d:Direction.values())l.setBlockAndUpdate(p.relative(d),Blocks.AIR.defaultBlockState());
            l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p.relative(face),MireContent.LIQUID.get().defaultBlockState());
            var raw=(living?DreamContent.LIVING_TEMPORAL_SILT:DreamContent.TEMPORAL_SILT).get().defaultBlockState();l.setBlockAndUpdate(p,raw);raw.tick(l,p,l.random);
            var sediment=l.getBlockState(p);h.assertTrue(sediment.is((living?TimeNatureContent.LIVING_SILT:TimeNatureContent.SILT).get()),"Skipped sediment stage");
            h.assertTrue(l.getBlockTicks().hasScheduledTick(p,sediment.getBlock()),"Decay not scheduled on contact");
            var ticks=(net.minecraft.world.ticks.LevelChunkTicks<net.minecraft.world.level.block.Block>)l.getChunkAt(p).getBlockTicks();
            h.assertTrue(ticks.getAll().anyMatch(t->t.pos().equals(p)&&t.type()==sediment.getBlock()&&t.triggerTick()-l.getGameTime()==6000),"Decay not five minutes");
            sediment.tick(l,p,l.random);h.assertTrue(l.getBlockState(p).is(MireContent.SILT.get()),"Face contact did not decay");
        }
        l.setBlockAndUpdate(p,MireContent.LIQUID.get().defaultBlockState());var top=p.above();
        var decay=MireContent.DECAYED.get().defaultBlockState();l.setBlockAndUpdate(top,decay);
        h.assertTrue(decay.canSurvive(l,top),"Lily cannot float");
        MireLilyBlock.updateNatural(l,top,decay,6000);h.assertTrue(l.getBlockState(top).is(MireContent.REBORN.get()),"Decayed lily did not reverse");
        var fresh=l.getBlockState(top);MireLilyBlock.updateNatural(l,top,fresh,7200);h.assertTrue(l.getBlockState(top).is(MireContent.DECAYED.get()),"Natural lily did not decay again");
        l.setBlockAndUpdate(top,MireContent.REBORN.get().defaultBlockState());MireLilyBlock.updateNatural(l,top,l.getBlockState(top),7200);h.assertTrue(l.getBlockState(top).is(MireContent.REBORN.get()),"Harvested fresh lily lost fixed state");
        biome(h,p,ResourceLocation.parse("minecraft:plains"));l.setBlockAndUpdate(top,decay);MireLilyBlock.updateNatural(l,top,decay,6000);h.assertTrue(l.getBlockState(top).is(MireContent.DECAYED.get()),"Lily reversed outside mire");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void entityReversal(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,3,4));biome(h,p,MireCycle.BIOME.location());
        var cow=EntityType.COW.create(l);cow.moveTo(p.getCenter());cow.setAge(0);l.addFreshEntity(cow);MireCycle.update(cow,4800);
        var away=p.offset(64,0,0);biome(h,away,ResourceLocation.parse("minecraft:plains"));cow.teleportTo(away.getX(),away.getY(),away.getZ());MireCycle.update(cow,6000);
        h.assertTrue(cow.position().distanceTo(p.getCenter())<.01&&cow.isBaby(),"Departed entity not pulled back and de-aged");
        MireCycle.update(cow,7199);h.assertTrue(cow.isBaby(),"Baby state ended early");MireCycle.update(cow,7200);h.assertTrue(!cow.isBaby(),"Adult state not restored");
        var child=EntityType.COW.create(l);child.moveTo(p.getCenter());child.setAge(-14000);MireCycle.update(child,6100);MireCycle.update(child,7200);h.assertTrue(child.isBaby()&&child.getAge()==-14000,"New baby was grown by reversal");
        var zombie=EntityType.ZOMBIE.create(l);zombie.moveTo(p.getCenter());MireCycle.update(zombie,6000);h.assertTrue(zombie.isBaby(),"Zombie did not become baby");MireCycle.update(zombie,7200);h.assertTrue(!zombie.isBaby(),"Zombie remained baby");
        var eel=MireContent.EEL.get().create(l);eel.moveTo(p.getCenter());MireCycle.update(eel,4800);eel.moveTo(away.getCenter());MireCycle.update(eel,6000);h.assertTrue(eel.position().distanceTo(away.getCenter())<.01&&!eel.getPersistentData().contains("OmniraMirePosition"),"Eel immunity failed");
        var item=new net.minecraft.world.entity.item.ItemEntity(l,p.getX(),p.getY(),p.getZ(),new ItemStack(Items.STICK));MireCycle.update(item,4800);item.moveTo(away.getCenter());MireCycle.update(item,6000);h.assertTrue(item.blockPosition().equals(p),"Nonliving entity did not rewind");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void treesRestoreOnlyOriginalSapling(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,3,4));biome(h,p,MireCycle.BIOME.location());
        var shape=java.util.List.of(p,p.above(),p.above(2));var trees=new MireTrees();
        for(int attempt=0;attempt<3;attempt++){
            l.setBlockAndUpdate(p.below(),MireContent.SILT.get().defaultBlockState());for(var at:shape)l.setBlockAndUpdate(at,TimeNatureContent.LOG.get().defaultBlockState());
            trees.register(p,shape);trees.pulse(l,6000);
            h.assertTrue(l.getBlockState(p).is(TimeNatureContent.SAPLING.get())&&l.getBlockState(p).getValue(TimeSaplingBlock.REWINDING)&&l.isEmptyBlock(p.above()),"Tree not reduced to marked sapling");
            if(attempt>0)l.setBlockAndUpdate(p,attempt==1?Blocks.AIR.defaultBlockState():TimeNatureContent.SAPLING.get().defaultBlockState());
            trees=MireTrees.load(trees.save(new net.minecraft.nbt.CompoundTag(),l.registryAccess()),l.registryAccess());trees.pulse(l,7200);
            h.assertTrue(attempt==0?l.getBlockState(p).is(TimeNatureContent.LOG.get()):!l.getBlockState(p).is(TimeNatureContent.LOG.get()),"Destroyed/replaced sapling restored old tree");
        }h.succeed();
    }
    @GameTest(template="spell_arena",templateNamespace="omnira_mire_fishing",timeoutTicks=300)
    public static void fishingPoolsAndFish(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,3,4));
        var key=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("omnira:fishing/reversion_mire"));var table=l.getServer().reloadableRegistries().getLootTable(key);
        int[] treasures=new int[2];
        var accepted=java.util.Set.of("timeflow_eel","infused_spiritual_crystal","dream_crystal_shard","time_seed","time_sapling","life_ender","time_warp_point","dream_spell_core","time_microcore","space_microcore","enchanted_book");
        for(int luck=0;luck<=3;luck+=3)for(int i=0;i<1500;i++){
            var params=new LootParams.Builder(l).withParameter(LootContextParams.ORIGIN,p.getCenter()).withParameter(LootContextParams.TOOL,new ItemStack(Items.FISHING_ROD)).withLuck(luck).create(LootContextParamSets.FISHING);
            var drops=table.getRandomItems(params,i+100L);h.assertTrue(drops.size()==1,"Fishing must yield one item");
            String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(drops.getFirst().getItem()).getPath();h.assertTrue(accepted.contains(id),"Unexpected catch: "+id);
            if(java.util.Set.of("time_warp_point","enchanted_book","dream_spell_core","time_microcore","space_microcore").contains(id))treasures[luck/3]++;
        }
        h.assertTrue(treasures[1]>treasures[0],"Sea luck did not increase treasure");
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<3;y++)l.setBlockAndUpdate(p.offset(x,y,z),MireContent.LIQUID.get().defaultBlockState());
        var eel=MireContent.EEL.get().create(l);eel.moveTo(p.getCenter().add(0,.5,0));l.addFreshEntity(eel);eel.tick();
        h.assertTrue(eel.isInWater()&&eel.getBucketItemStack().is(MireContent.EEL_BUCKET.get()),"Eel water AI/bucket incorrect");
        var player=new net.neoforged.neoforge.common.util.FakePlayer(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"mire-fisher"));player.moveTo(p.getCenter().add(1,2,1));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.FISHING_ROD));
        var hook=new net.minecraft.world.entity.projectile.FishingHook(player,l,3,0);hook.moveTo(p.getCenter().add(0,.5,0));l.addFreshEntity(hook);
        try{var nibble=net.minecraft.world.entity.projectile.FishingHook.class.getDeclaredField("nibble");nibble.setAccessible(true);nibble.setInt(hook,20);}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
        player.getAbilities().instabuild=false;
        player.getMainHandItem().use(l,player,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().getDamageValue()==3,"Mire fishing did not apply three durability points");
        var catches=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(p).inflate(4));
        h.assertTrue(catches.stream().anyMatch(e->accepted.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getPath())),"Real fishing hook did not use mire table");
        h.succeed();
    }
    @GameTest(template="spell_arena",templateNamespace="omnira_mire_fluid")
    public static void fluidHazards(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,3,4));
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<3;y++)l.setBlockAndUpdate(p.offset(x,y,z),MireContent.LIQUID.get().defaultBlockState());
        var cow=EntityType.COW.create(l);cow.moveTo(p.getCenter());l.addFreshEntity(cow);cow.updateFluidHeightAndDoFluidPushing();
        h.assertTrue(MireFluidEffects.affected(cow),"Cow not exposed to fluid");
        var start=cow.position();cow.move(MoverType.SELF,new net.minecraft.world.phys.Vec3(.4,0,0));
        h.assertTrue(Math.abs(cow.getX()-start.x-.2)<.0001,"Fluid movement must be exactly half");
        cow.tickCount=40;float health=cow.getHealth();MireFluidEffects.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(cow));
        h.assertTrue(cow.getHealth()==health-1,"Fluid did not apply wither damage");
        for(var type:java.util.List.of(MireContent.EEL.get(),ModEntityTypes.DREAM_MIRROR.get(),ModEntityTypes.LIGHT_SPIRIT.get(),ModEntityTypes.SHADOW_GHOST.get())){
            var mob=(LivingEntity)type.create(l);mob.moveTo(p.getCenter());mob.updateFluidHeightAndDoFluidPushing();mob.tickCount=40;
            float hp=mob.getHealth();h.assertTrue(!MireFluidEffects.affected(mob),"Immune creature slowed: "+type);
            MireFluidEffects.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(mob));h.assertTrue(mob.getHealth()==hp,"Immune creature damaged: "+type);
        }
        var item=new net.minecraft.world.entity.item.ItemEntity(l,p.getX()+.5,p.getY()+1,p.getZ()+.5,new ItemStack(Items.STICK));
        item.updateFluidHeightAndDoFluidPushing();h.assertTrue(MireFluidEffects.affected(item),"Dropped item not slowed");
        cow.moveTo(p.getCenter().add(10,0,0));cow.baseTick();
        h.assertTrue(!MireFluidEffects.affected(cow),"Slowdown remains outside fluid");
        h.succeed();
    }
}
