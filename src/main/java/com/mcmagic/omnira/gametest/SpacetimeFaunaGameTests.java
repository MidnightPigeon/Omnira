package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.entity.spacetime.*;
import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.weaver.WeaverCombat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spacetime_fauna") @PrefixGameTestTemplate(false)
public final class SpacetimeFaunaGameTests {
    private static void floor(GameTestHelper h){for(int x=1;x<23;x++)for(int z=1;z<23;z++)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);}
    private static net.minecraft.world.entity.monster.Zombie victim(GameTestHelper h,int x,int z){
        var v=h.spawn(EntityType.ZOMBIE,x,3,z);v.setNoAi(true);v.setNoGravity(true);v.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);v.setHealth(100);v.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));return v;
    }
    @GameTest(template="spell_arena") public static void snailRetaliatesOnceAndOnlyAtAttacker(GameTestHelper h){
        var snail=h.spawn(ModEntityTypes.AGECOIL_SNAIL.get(),4,3,4);snail.setNoAi(true);snail.setNoGravity(true);
        var attacker=victim(h,6,4);var bystander=victim(h,5,5);var helmet=new ItemStack(Items.IRON_HELMET);attacker.setItemSlot(EquipmentSlot.HEAD,helmet);
        snail.hurt(snail.damageSources().mobAttack(attacker),1);
        h.assertTrue(!snail.isPickable()&&!snail.isAttackable()&&!snail.hurt(snail.damageSources().generic(),1000),"Sheltered snail was selectable or vulnerable");
        for(int i=0;i<7;i++)snail.tick();
        var saved=snail.saveWithoutId(new CompoundTag());
        snail.discard();
        snail=ModEntityTypes.AGECOIL_SNAIL.get().create(h.getLevel());
        snail.load(saved);
        h.assertTrue(snail.shelterTicks()==13,"Reload lost remaining shelter time");
        for(int i=0;i<12;i++)snail.tick();h.assertTrue(attacker.getHealth()==100,"Ray fired before one second");
        snail.tick();h.assertTrue(snail.isPickable()&&snail.isAttackable(),"Snail stayed untargetable");
        for(int i=0;i<60;i++)snail.tick();
        h.assertTrue(attacker.getHealth()==99&&bystander.getHealth()==100,"Ray damage or target isolation incorrect");
        h.assertTrue(helmet.getDamageValue()==(int)Math.ceil(helmet.getMaxDamage()*.05),"Expected exactly one 5% wear settlement");
        h.assertTrue(snail.getTarget()==null&&snail.shelterTicks()==0,"Snail did not return neutral");
        h.assertTrue(com.mcmagic.omnira.time.TemporalCreatureTags.timeImmune(snail),"Snail is not immune to time biome hazards");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rayKeepsBossWearAndIgnoresUnbreakableEquipment(GameTestHelper h){
        var snail=h.spawn(ModEntityTypes.AGECOIL_SNAIL.get(),4,3,4);var v=victim(h,6,4);
        var sword=new ItemStack(Items.IRON_SWORD);v.setItemSlot(EquipmentSlot.MAINHAND,sword);
        WeaverCombat.rayHit(snail,v);h.assertTrue(v.getHealth()==97&&sword.getDamageValue()==25,"Existing boss/reward ray changed");
        var rod=new ItemStack(Items.FISHING_ROD);rod.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,new net.minecraft.world.item.component.Unbreakable(true));v.setItemSlot(EquipmentSlot.OFFHAND,rod);
        WeaverCombat.lockedRay(snail,v,1,.05F);h.assertTrue(rod.getDamageValue()==0,"Unbreakable equipment eroded");h.succeed();
    }
    @GameTest(template="spell_arena") public static void deerRemembersThroughOcclusionAndExpires(GameTestHelper h){
        var deer=h.spawn(ModEntityTypes.RIFTSTEP_DEER.get(),4,3,4);deer.setNoAi(true);deer.setNoGravity(true);var v=victim(h,6,4);
        deer.hurt(deer.damageSources().mobAttack(v),1);
        h.setBlock(new BlockPos(5,3,4),Blocks.STONE);h.setBlock(new BlockPos(5,4,4),Blocks.STONE);h.setBlock(new BlockPos(5,5,4),Blocks.STONE);
        for(int i=0;i<120;i++)deer.tick();
        h.assertTrue(deer.getTarget()==v&&deer.angerTicks()==1080,"Occlusion cleared anger");
        var tag=new CompoundTag();deer.addAdditionalSaveData(tag);h.assertTrue(tag.hasUUID("AngerTarget")&&tag.getInt("AngerTicks")==1080,"Anger not saved");
        tag.putInt("AngerTicks",1);deer.readAdditionalSaveData(tag);deer.tick();h.assertTrue(deer.getTarget()==null&&deer.angerTicks()==0,"Anger did not expire");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void deerLowersHeadBeforeSingleRam(GameTestHelper h){
        floor(h);var deer=h.spawn(ModEntityTypes.RIFTSTEP_DEER.get(),4,3,4);var target=victim(h,6,4);
        deer.hurt(deer.damageSources().mobAttack(target),1);
        h.assertTrue(deer.angry(),"Anger not synchronized for antler glow");
        long[] start={-1};
        h.onEachTick(()->{
            if(start[0]<0&&deer.ramming())start[0]=h.getTick();
            if(start[0]<0)return;
            long elapsed=h.getTick()-start[0];
            if(elapsed==3){
                h.assertTrue(deer.ramming()&&deer.headLowering(1)>0,"No lowered-head windup");
                h.assertTrue(target.getHealth()==100,"Ram hit before windup completed");
            }
            if(elapsed==17){
                h.assertTrue(target.getHealth()==90,"Ram must hit once for 10 true damage: "+target.getHealth());
                target.setPos(deer.position().add(33,0,0));
            }
            if(elapsed==19){
                h.assertTrue(!deer.angry()&&!deer.ramming(),"Anger/charge visual state survived disengagement");h.succeed();
            }
        });
        h.runAfterDelay(75,()->h.assertTrue(start[0]>=0,"Ram never began; distance="+deer.distanceTo(target)+" target="+(deer.getTarget()==target)+" health="+target.getHealth()));
    }
    @GameTest(template="spell_arena") public static void deerPursuesBehindCover(GameTestHelper h){
        floor(h);var deer=h.spawn(ModEntityTypes.RIFTSTEP_DEER.get(),4,3,4);deer.setNoAi(true);deer.setNoGravity(true);
        var target=victim(h,10,4);
        for(int y=3;y<=6;y++)for(int z=2;z<=6;z++)h.setBlock(new BlockPos(6,y,z),Blocks.STONE);
        deer.hurt(deer.damageSources().mobAttack(target),1);
        h.assertTrue(!deer.hasLineOfSight(target),"Cover did not obstruct initial view");
        var start=deer.position();for(int i=0;i<10;i++)deer.tick();
        h.assertTrue(deer.getTarget()==target&&deer.angry(),"Cover cleared target or anger");
        h.assertTrue(deer.position().distanceToSqr(start)>1&&deer.distanceTo(target)<6,"Did not teleport after obscured target");h.succeed();
    }
    @GameTest(template="spell_arena") public static void deerTrueDamageKnockbackAndRange(GameTestHelper h){
        var deer=h.spawn(ModEntityTypes.RIFTSTEP_DEER.get(),4,3,4);deer.setNoAi(true);deer.setNoGravity(true);var v=victim(h,6,4);v.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20);v.setAbsorptionAmount(20);
        deer.hurt(deer.damageSources().mobAttack(v),1);deer.doHurtTarget(v);
        h.assertTrue(v.getHealth()==90&&v.getAbsorptionAmount()==20&&v.getDeltaMovement().lengthSqr()>0,"Antlers health="+v.getHealth()+" absorption="+v.getAbsorptionAmount()+" motion="+v.getDeltaMovement());
        v.setPos(deer.position().add(32.1,0,0));deer.tick();h.assertTrue(deer.getTarget()==null,"Target beyond 32 retained");
        h.assertTrue(deer.getAttributeValue(Attributes.MOVEMENT_SPEED)>.3&&deer.getAttributeValue(Attributes.FOLLOW_RANGE)==32,"Pursuit attributes incorrect");h.succeed();
    }
    @GameTest(template="spell_arena") public static void deerPursuesAndShortHops(GameTestHelper h){
        floor(h);var deer=h.spawn(ModEntityTypes.RIFTSTEP_DEER.get(),3,3,3);deer.setNoAi(true);deer.setNoGravity(true);var v=victim(h,20,3);
        deer.hurt(deer.damageSources().mobAttack(v),1);Vec3 start=deer.position();
        for(int i=0;i<10;i++)deer.tick();double moved=deer.position().distanceTo(start);
        h.assertTrue(moved>=8&&moved<=16,"Long pursuit step outside 8–16");
        h.assertTrue(deer.distanceTo(v)<5,"Did not approach aggressor");
        v.setPos(deer.position().add(-5,0,0));start=deer.position();for(int i=0;i<40;i++)deer.tick();
        h.assertTrue(deer.position().distanceTo(start)>0&&deer.position().distanceTo(start)<8,"Short pursuit hop unavailable");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=80) public static void mantisMustTelegraphTeleportAndRetreat(GameTestHelper h){
        floor(h);var m=h.spawn(ModEntityTypes.RIFT_MANTIS.get(),2,3,2);var v=victim(h,20,20);m.setTarget(v);
        h.assertTrue(m.beginStrike(v)&&m.windup()==20,"Could not begin fixed-distance attack");
        h.assertTrue(v.getHealth()==100&&!m.doHurtTarget(v),"Mantis dealt contact damage before teleport");
        Vec3 start=m.position();
        h.runAfterDelay(10,()->h.assertTrue(v.getHealth()==100,"Early health="+v.getHealth()+" windup="+m.windup()));
        h.runAfterDelay(21,()->{h.assertTrue(v.getHealth()==85,"Strike failed or wrong damage");h.assertTrue(Math.abs(m.position().distanceTo(start)-8)<1,"Expected 24 forward then 16 backward");h.succeed();});
    }
    @GameTest(template="spell_arena") public static void unsafeTeleportsCannotCrossIntoSolidOrLiquids(GameTestHelper h){
        floor(h);var deer=h.spawn(ModEntityTypes.RIFTSTEP_DEER.get(),3,3,3);var destination=h.absoluteVec(new Vec3(7.5,3,3.5));
        h.setBlock(new BlockPos(7,3,3),Blocks.STONE);h.assertTrue(!SpatialSteps.teleport(deer,destination,16),"Solid destination allowed");
        h.setBlock(new BlockPos(7,3,3),Blocks.WATER);h.assertTrue(!SpatialSteps.teleport(deer,destination,16),"Liquid destination allowed");
        h.setBlock(new BlockPos(7,3,3),Blocks.AIR);h.assertTrue(SpatialSteps.teleport(deer,destination,16),"Safe platform refused");
        h.assertTrue(!SpatialSteps.teleport(deer,destination.add(17,0,0),16),"Over-range teleport allowed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void mothFlockAndHabitatData(GameTestHelper h){
        var m=h.spawn(ModEntityTypes.RIFT_MOTH.get(),4,4,4);var id=java.util.UUID.randomUUID();m.setFlock(id);var tag=new CompoundTag();m.addAdditionalSaveData(tag);
        var other=h.spawn(ModEntityTypes.RIFT_MOTH.get(),5,4,4);other.readAdditionalSaveData(tag);h.assertTrue(id.equals(other.flock()),"Flock lost on reload");
        var attacker=victim(h,2,4);
        double initialDistance=other.distanceTo(attacker);
        m.hurt(m.damageSources().mobAttack(attacker),1);
        for(int i=0;i<28;i++)h.getLevel().tickNonPassenger(other);
        h.assertTrue(other.distanceTo(attacker)>initialDistance+1,"Unhurt flock member did not flee");
        var registry=h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var time=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.parse("omnira:temporal_continent")));
        var space=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.parse("omnira:folded_caverns")));
        h.assertTrue(SpacetimeFaunaSpawning.habitat(time,ModEntityTypes.AGECOIL_SNAIL.get())&&!SpacetimeFaunaSpawning.habitat(space,ModEntityTypes.AGECOIL_SNAIL.get()),"Snail habitat incorrect");
        h.assertTrue(SpacetimeFaunaSpawning.habitat(space,ModEntityTypes.RIFTSTEP_DEER.get())&&SpacetimeFaunaSpawning.habitat(time,ModEntityTypes.RIFT_MOTH.get())&&SpacetimeFaunaSpawning.habitat(space,ModEntityTypes.RIFT_MOTH.get()),"Space or moth habitat incorrect");h.succeed();
    }
    @GameTest(template="spell_arena") public static void deerPrefersUpperPlatform(GameTestHelper h){
        floor(h);var deer=h.spawn(ModEntityTypes.RIFTSTEP_DEER.get(),12,3,12);deer.setNoAi(true);
        h.setBlock(new BlockPos(14,3,12),Blocks.STONE);
        var destination=SpatialSteps.platform(deer,16);
        h.assertTrue(destination!=null&&destination.y>deer.getY(),"Upper platform not preferred");
        h.assertTrue(SpatialSteps.teleport(deer,destination,16),"Cannot use selected upper platform");
        h.assertTrue(deer.getAttributeValue(Attributes.STEP_HEIGHT)==.6,"Half-block stepping unavailable");h.succeed();
    }
    @GameTest(template="spell_arena") public static void faunaDropsCorrectPointsAndMothDropsNothing(GameTestHelper h){
        var types=java.util.List.of(ModEntityTypes.AGECOIL_SNAIL.get(),ModEntityTypes.RIFTSTEP_DEER.get(),ModEntityTypes.RIFT_MANTIS.get(),ModEntityTypes.RIFT_MOTH.get());
        for(int i=0;i<types.size();i++){
            var mob=h.spawn(types.get(i),4+i*4,3,4);mob.setNoAi(true);var bounds=mob.getBoundingBox().inflate(1.5);
            mob.hurt(mob.damageSources().generic(),1000);
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds);
            if(i==3)h.assertTrue(drops.isEmpty(),"Moth has drops");
            else{
                var item=i==0?com.mcmagic.omnira.registry.ModItems.TIME_WARP_POINT.get():com.mcmagic.omnira.registry.ModItems.SPACE_WARP_POINT.get();
                int count=drops.stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
                h.assertTrue(count>=3&&count<=5,"Incorrect point drop for "+types.get(i)+": "+count);
            }
            drops.forEach(Entity::discard);
        }h.succeed();
    }

    @GameTest(template="spell_arena") public static void deerWalksSlabsButTeleportsFullSteps(GameTestHelper h){
        var deer=h.spawn(ModEntityTypes.RIFTSTEP_DEER.get(),5,3,5);deer.setNoAi(true);
        var next=h.absolutePos(new net.minecraft.core.BlockPos(6,4,5));
        h.getLevel().setBlock(next.below(),net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState(),3);
        h.assertTrue(!deer.requiresHeightStep(next),"Half slab requested teleport");
        h.getLevel().setBlock(next.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        h.assertTrue(deer.requiresHeightStep(next),"Full block did not request teleport");h.succeed();
    }
}
