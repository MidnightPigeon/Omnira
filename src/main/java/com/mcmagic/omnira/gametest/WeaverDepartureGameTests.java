package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.entity.WeaverSpirit;
import com.mcmagic.omnira.myriad.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.weaver.WeaverDeparture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_weaver_departure") @PrefixGameTestTemplate(false)
public final class WeaverDepartureGameTests {
    private static WeaverSpirit boss(GameTestHelper h){
        for(int x=2;x<9;x++)for(int z=2;z<9;z++)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        return h.spawn(ModEntityTypes.WEAVER_SPIRIT.get(),new BlockPos(5,3,5));
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,WeaverSpirit boss){
        var p=BranchAdvancementGameTests.player(h);p.moveTo(boss.position().add(4,0,0));h.getLevel().addFreshEntity(p);return p;
    }
    private static ItemStack cloud(int count){return new ItemStack(MiniatureWorldContent.PROBABILITY_CLOUD.get(),count);}
    private static int drops(GameTestHelper h,WeaverSpirit b,Item item){return h.getLevel().getEntitiesOfClass(ItemEntity.class,b.getBoundingBox().inflate(6),e->e.getItem().is(item)).stream().mapToInt(e->e.getItem().getCount()).sum();}
    private static int experience(GameTestHelper h,WeaverSpirit b){return h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,b.getBoundingBox().inflate(6)).stream().mapToInt(e->{var tag=new CompoundTag();e.saveWithoutId(tag);return e.getValue()*Math.max(1,tag.getInt("Count"));}).sum();}
    private static void assertFinalDropPosition(GameTestHelper h,WeaverSpirit b,net.minecraft.world.phys.Vec3 origin){
        var expected=origin.add(0,1.5+WeaverDeparture.deathRise(WeaverDeparture.DEATH_TICKS),0);
        var rewards=h.getLevel().getEntitiesOfClass(ItemEntity.class,b.getBoundingBox().inflate(6),e->e.getItem().is(ModItems.FOUNDER_APPROVAL.get()));
        h.assertTrue(rewards.size()==1&&rewards.getFirst().position().distanceToSqr(expected)<.0001,"Defeat loot did not spawn at the final airborne core");
    }
    private static void cleanup(GameTestHelper h,WeaverSpirit b,Entity p){h.getLevel().getEntitiesOfClass(ItemEntity.class,b.getBoundingBox().inflate(6)).forEach(Entity::discard);h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,b.getBoundingBox().inflate(6)).forEach(Entity::discard);b.discard();p.discard();}
    @GameTest(template="spell_arena") public static void cloudDetectionIsFlatInventoryAndSpherical(GameTestHelper h){
        var b=boss(h);var p=player(h,b);
        try{
            p.getInventory().clearContent();p.setItemInHand(InteractionHand.OFF_HAND,cloud(1));
            h.assertTrue(!WeaverSpirit.carriesCloud(p),"Offhand counted as inventory cloud");
            var container=new ItemStack(Items.CHEST);container.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(cloud(1))));p.getInventory().setItem(0,container);
            h.assertTrue(!WeaverSpirit.carriesCloud(p),"Nested container was searched");
            for(int slot:new int[]{0,9,35}){p.getInventory().clearContent();p.getInventory().setItem(slot,cloud(1));h.assertTrue(WeaverSpirit.carriesCloud(p),"Ordinary inventory slot missed");}
            for(int i=0;i<10;i++)b.tick();h.assertTrue(b.cloudNear(),"Nearby cloud not detected");
            p.moveTo(b.position().add(5.1,0,0));for(int i=0;i<10;i++)b.tick();h.assertTrue(!b.cloudNear(),"Cloud outside five-block sphere detected");
            h.assertTrue(!b.active()&&b.motionClock(.75F)==b.motionClock(.25F)&&b.edgeClock(.75F)>b.edgeClock(.25F),"Idle ambient and mechanical clocks are not independent");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void peacefulDissolveReturnsCloudThenFourSeedsOnce(GameTestHelper h){
        var b=boss(h);var p=player(h,b);
        try{
            var offered=cloud(3);offered.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("returned-cloud"));
            p.setItemInHand(InteractionHand.MAIN_HAND,offered);h.assertTrue(b.interact(p,InteractionHand.MAIN_HAND).consumesAction(),"Cloud interaction rejected");
            h.assertTrue(b.departure()==WeaverDeparture.DISSOLVE&&p.getMainHandItem().getCount()==2&&!b.active(),"Cloud not consumed exactly once");
            b.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(p.getMainHandItem().getCount()==2&&!b.hurt(b.damageSources().magic(),20),"Repeated interaction/damage interrupted dissolve");
            for(int i=0;i<11;i++)b.tick();h.assertTrue(drops(h,b,MiniatureWorldContent.PROBABILITY_CLOUD.get())==0,"Cloud returned before first stage");b.tick();
            h.assertTrue(drops(h,b,MiniatureWorldContent.PROBABILITY_CLOUD.get())==1&&drops(h,b,WorldSeedContent.ACTIVE.get())+drops(h,b,WorldSeedContent.DORMANT.get())==0,"Cloud was not first return");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,b.getBoundingBox().inflate(6)).stream().anyMatch(e->ItemStack.matches(e.getItem(),offered.copyWithCount(1))),"Returned cloud lost components");
            for(int i=12;i<WeaverDeparture.DISSOLVE_TICKS-1;i++)b.tick();
            h.assertTrue(drops(h,b,ModItems.FOUNDER_APPROVAL.get())==0&&experience(h,b)==0,"Peaceful final rewards arrived before core vanished");b.tick();
            h.assertTrue(b.isRemoved()&&drops(h,b,WorldSeedContent.ACTIVE.get())+drops(h,b,WorldSeedContent.DORMANT.get())==4&&drops(h,b,ModItems.FOUNDER_APPROVAL.get())==1&&experience(h,b)==1024,"Peaceful final rewards incorrect");
            h.assertTrue(b.getHealth()==1024&&b.deathTime==0,"Peaceful dissolve used death animation");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void dissolveReloadDoesNotDuplicateReturnedMaterials(GameTestHelper h){
        var b=boss(h);var p=player(h,b);WeaverSpirit restored=null;
        try{
            p.setItemInHand(InteractionHand.MAIN_HAND,cloud(1));b.interact(p,InteractionHand.MAIN_HAND);for(int i=0;i<55;i++)b.tick();
            h.assertTrue(drops(h,b,MiniatureWorldContent.PROBABILITY_CLOUD.get())==1,"Missing pre-save return");
            var tag=new CompoundTag();b.saveWithoutId(tag);b.discard();restored=ModEntityTypes.WEAVER_SPIRIT.get().create(h.getLevel());restored.load(tag);
            for(int i=55;i<120;i++)restored.tick();
            h.assertTrue(restored.isRemoved()&&drops(h,restored,MiniatureWorldContent.PROBABILITY_CLOUD.get())==1&&drops(h,restored,WorldSeedContent.ACTIVE.get())+drops(h,restored,WorldSeedContent.DORMANT.get())==4&&drops(h,restored,ModItems.FOUNDER_APPROVAL.get())==1&&experience(h,restored)==1024,"Reload duplicated rewards or reset phase");
            restored.tick();h.assertTrue(drops(h,restored,ModItems.FOUNDER_APPROVAL.get())==1&&experience(h,restored)==1024,"Completed dissolve repeated rewards");
        }finally{if(restored!=null)restored.discard();cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void hostileBossRejectsCloudAndDefeatLastsFullAnimation(GameTestHelper h){
        var b=boss(h);var p=player(h,b);
        try{
            b.attacked(b.damageSources().playerAttack(p));p.setItemInHand(InteractionHand.MAIN_HAND,cloud(1));b.interact(p,InteractionHand.MAIN_HAND);
            h.assertTrue(b.active()&&b.departure()==0&&p.getMainHandItem().getCount()==1,"Hostile boss accepted peaceful dissolve");
            com.mcmagic.omnira.combat.OmniraDamage.execute(b,b.damageSources().genericKill());
            h.assertTrue(b.departure()==WeaverDeparture.DEFEATED&&drops(h,b,ModItems.FOUNDER_APPROVAL.get())==0&&experience(h,b)==0,"Defeat rewards arrived before explosions");
            for(int i=0;i<139;i++){b.tick();h.assertTrue(drops(h,b,ModItems.FOUNDER_APPROVAL.get())==0&&experience(h,b)==0,"Reward arrived during explosions");}
            h.assertTrue(!b.isRemoved(),"Boss vanished at native twenty-tick death timeout");b.tick();
            h.assertTrue(b.isRemoved()&&drops(h,b,ModItems.FOUNDER_APPROVAL.get())==1&&experience(h,b)==1024&&drops(h,b,WorldSeedContent.ACTIVE.get())+drops(h,b,WorldSeedContent.DORMANT.get())==0,"Defeat final rewards incorrect");
        }finally{cleanup(h,b,p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void deathBurstsAreVisualOnly(GameTestHelper h){
        var b=boss(h);var p=player(h,b);var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(5,6,5));
        try{
            var origin=b.position();
            h.assertTrue(Math.abs(WeaverDeparture.DEATH_RISE-(140*.018F+2))<.0001&&WeaverDeparture.deathRise(0)==0
                    &&WeaverDeparture.deathRise(280)==WeaverDeparture.DEATH_RISE,"Death rise must add exactly two blocks and stop at the finale");
            h.assertTrue(WeaverDeparture.BLAST_MIN_RADIUS>.35F&&WeaverDeparture.BLAST_MAX_RADIUS>.75F,"Death explosions were not enlarged");
            cow.setNoAi(true);cow.setNoGravity(true);float health=cow.getHealth();
            h.setBlock(new BlockPos(5,7,5),Blocks.GLASS);
            h.assertTrue(!b.active(),"Neutral defeat fixture entered combat");
            com.mcmagic.omnira.combat.OmniraDamage.execute(b,b.damageSources().genericKill());
            h.assertTrue(b.departure()==WeaverDeparture.DEFEATED,"Neutral lethal damage did not start rising explosions");
            p.setItemInHand(InteractionHand.MAIN_HAND,cloud(1));b.interact(p,InteractionHand.MAIN_HAND);
            h.assertTrue(p.getMainHandItem().getCount()==1&&b.departure()==WeaverDeparture.DEFEATED,"Dead neutral boss accepted probability cloud");
            for(int i=0;i<140;i++)b.tick();
            assertFinalDropPosition(h,b,origin);
            h.assertTrue(cow.getHealth()==health&&h.getBlockState(new BlockPos(5,7,5)).is(Blocks.GLASS),"Cosmetic death bursts damaged a creature or terrain");
            h.assertTrue(drops(h,b,WorldSeedContent.ACTIVE.get())+drops(h,b,WorldSeedContent.DORMANT.get())==0,"Neutral defeat used peaceful returns");
        }finally{cow.discard();cleanup(h,b,p);}h.succeed();
    }
    private WeaverDepartureGameTests(){}
    @GameTest(template="spell_arena") public static void defeatReloadPreservesDelayedLootAndExperience(GameTestHelper h){
        var b=boss(h);var p=player(h,b);WeaverSpirit restored=null;
        try{
            var origin=b.position();
            com.mcmagic.omnira.combat.OmniraDamage.execute(b,b.damageSources().genericKill());
            for(int i=0;i<80;i++)b.tick();
            var tag=new CompoundTag();b.saveWithoutId(tag);b.discard();
            restored=ModEntityTypes.WEAVER_SPIRIT.get().create(h.getLevel());restored.load(tag);
            for(int i=80;i<139;i++)restored.tick();
            h.assertTrue(!restored.isRemoved()&&drops(h,restored,ModItems.FOUNDER_APPROVAL.get())==0&&experience(h,restored)==0,"Reload shortened death or paid early");
            restored.tick();restored.tick();
            assertFinalDropPosition(h,restored,origin);
            h.assertTrue(restored.isRemoved()&&drops(h,restored,ModItems.FOUNDER_APPROVAL.get())==1&&experience(h,restored)==1024,"Reload lost or duplicated death rewards");
        }finally{if(restored!=null)restored.discard();cleanup(h,b,p);}h.succeed();
    }
}
