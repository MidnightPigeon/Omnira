package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.ancient.*;
import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.reversal.ReversalContent;
import com.mcmagic.omnira.time.TemporalCreatureTags;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_ancient")
@PrefixGameTestTemplate(false)
public final class AncientCompanionGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var profile=new GameProfile(UUID.randomUUID(),"companion-test");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(8,5,8))));
        h.getLevel().addNewPlayer(p);
        return p;
    }
    @GameTest(template="spell_arena") public static void statsAndTimeImmunity(GameTestHelper h) {
        var bird=ModEntityTypes.ARCHAEOPTERYX_SPIRIT.get().create(h.getLevel());
        var raptor=ModEntityTypes.VELOCIRAPTOR.get().create(h.getLevel());
        h.assertTrue(bird.getMaxHealth()==16 && raptor.getMaxHealth()==20,"Incorrect maximum health");
        h.assertTrue(bird.getAttributeValue(Attributes.ATTACK_DAMAGE)==6 && raptor.getAttributeValue(Attributes.ATTACK_DAMAGE)==6,"Incorrect attack damage");
        h.assertTrue(TemporalCreatureTags.timeImmune(bird) && TemporalCreatureTags.timeImmune(raptor),"Missing time-creature immunity");
        h.assertTrue(!bird.canMate(raptor) && !raptor.isFood(new ItemStack(Items.BEEF)),"Breeding unexpectedly enabled");
        h.assertTrue(ReversalContent.EGG.get() instanceof AncientSummonItem && ReversalContent.SEED.get() instanceof AncientSummonItem
                && !(ReversalContent.INACTIVE_EGG.get() instanceof AncientSummonItem) && !(ReversalContent.INACTIVE_SEED.get() instanceof AncientSummonItem),"Inactive items can summon");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void minuteRecoveryAndPersistence(GameTestHelper h) {
        for(var type:java.util.List.of(ModEntityTypes.ARCHAEOPTERYX_SPIRIT.get(),ModEntityTypes.VELOCIRAPTOR.get())) {
            AncientCompanion pet=(AncientCompanion)type.create(h.getLevel());
            pet.setNoAi(true);pet.moveTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3))));
            var tag=new CompoundTag();pet.addAdditionalSaveData(tag);tag.putInt("RecoveryTicks",1198);pet.readAdditionalSaveData(tag);
            pet.setHealth(1);pet.tick();h.assertTrue(pet.getHealth()==1,"Recovery happened early");pet.tick();
            h.assertTrue(pet.getHealth()==1+pet.getMaxHealth()/2,"Minute recovery amount incorrect");
            tag=new CompoundTag();pet.addAdditionalSaveData(tag);h.assertTrue(tag.getInt("RecoveryTicks")==0,"Recovery clock did not reset");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void ownerModesAndRiding(GameTestHelper h) {
        var owner=player(h);var stranger=player(h);
        var pet=ModEntityTypes.VELOCIRAPTOR.get().create(h.getLevel());pet.tame(owner);
        stranger.setShiftKeyDown(true);pet.mobInteract(stranger,InteractionHand.MAIN_HAND);
        h.assertTrue(!pet.isOrderedToSit(),"Non-owner changed mode");
        owner.setShiftKeyDown(true);pet.mobInteract(owner,InteractionHand.MAIN_HAND);
        h.assertTrue(pet.isOrderedToSit(),"Stay toggle failed");
        var saved=new CompoundTag();pet.addAdditionalSaveData(saved);
        var restored=ModEntityTypes.VELOCIRAPTOR.get().create(h.getLevel());restored.readAdditionalSaveData(saved);
        h.assertTrue(restored.isOwnedBy(owner)&&restored.isOrderedToSit(),"Ownership or mode lost on reload");
        h.assertTrue(!stranger.startRiding(pet),"Non-owner mounted");
        owner.setShiftKeyDown(false);pet.mobInteract(owner,InteractionHand.MAIN_HAND);
        h.assertTrue(owner.getVehicle()==pet && pet.getControllingPassenger()==owner && !pet.isOrderedToSit(),"Saddle-free owner riding failed");
        owner.stopRiding();h.succeed();
    }
    @GameTest(template="spell_arena") public static void summonConsumesOnlyOnSuccess(GameTestHelper h) {
        var player=player(h);h.setBlock(3,1,3,Blocks.STONE);
        BlockPos pos=h.absolutePos(new BlockPos(3,1,3));
        for(var item:java.util.List.of(ReversalContent.SEED.get(),ReversalContent.EGG.get())) {
            var stack=new ItemStack(item,2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
            var result=item.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            h.assertTrue(result.consumesAction()&&stack.getCount()==1,"Summoning failed or did not consume exactly one");
            var pets=h.getLevel().getEntitiesOfClass(AncientCompanion.class,new AABB(pos).inflate(3));
            h.assertTrue(!pets.isEmpty()&&pets.stream().allMatch(p->p.isOwnedBy(player)&&!p.isOrderedToSit()),"Summoned pet not bound and following");
            pets.forEach(net.minecraft.world.entity.Entity::discard);
        }
        h.setBlock(3,2,3,Blocks.STONE);h.setBlock(3,3,3,Blocks.STONE);
        var stack=new ItemStack(ReversalContent.EGG.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        h.assertTrue(!stack.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction()&&stack.getCount()==2,"Blocked summon consumed item");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void flightAndStay(GameTestHelper h) {
        var bird=ModEntityTypes.ARCHAEOPTERYX_SPIRIT.get().create(h.getLevel());
        var p=player(h);bird.tame(p);
        Vec3 origin=Vec3.atCenterOf(h.absolutePos(new BlockPos(4,5,4)));bird.setPos(origin);
        p.setPos(origin);
        bird.getMoveControl().setWantedPosition(origin.x,origin.y+2,origin.z+6,1);
        for(int i=0;i<20;i++){bird.tickCount++;bird.tick();}
        h.assertTrue(bird.getZ()>origin.z+1&&bird.getY()>origin.y+.2,"Spirit does not sustain flight");
        p.setShiftKeyDown(true);bird.mobInteract(p,InteractionHand.MAIN_HAND);
        Vec3 stay=bird.position();for(int i=0;i<20;i++){bird.tickCount++;bird.tick();}
        h.assertTrue(bird.position().distanceTo(stay)<.01,"Staying spirit drifted");
        bird.mobInteract(p,InteractionHand.MAIN_HAND);
        h.assertTrue(!bird.isOrderedToSit(),"Spirit cannot resume following");h.succeed();
    }
    @GameTest(template="spell_arena") public static void biteAndChargeDamage(GameTestHelper h) {
        var cow=net.minecraft.world.entity.EntityType.COW.create(h.getLevel());
        cow.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4,3,4))));
        var raptor=ModEntityTypes.VELOCIRAPTOR.get().create(h.getLevel());raptor.setPos(cow.position());
        h.assertTrue(raptor.doHurtTarget(cow)&&cow.getHealth()==4,"Raptor bite did not deal six damage");
        cow=net.minecraft.world.entity.EntityType.COW.create(h.getLevel());cow.setPos(raptor.position());
        var bird=ModEntityTypes.ARCHAEOPTERYX_SPIRIT.get().create(h.getLevel());bird.setPos(cow.position());bird.setTarget(cow);
        bird.tickCount++;bird.tick();
        h.assertTrue(cow.getHealth()==4,"Spirit contact charge did not deal six damage");
        bird.tickCount++;bird.tick();h.assertTrue(cow.getHealth()==4,"Charge cooldown not respected");h.succeed();
    }
    @GameTest(template="spell_arena") public static void riddenSteeringAndJump(GameTestHelper h) {
        var p=player(h);var pet=ModEntityTypes.VELOCIRAPTOR.get().create(h.getLevel());pet.tame(p);
        pet.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4,3,4))));pet.setNoAi(true);
        h.assertTrue(p.startRiding(pet),"Owner cannot mount");
        p.setYRot(90);p.zza=1;pet.setOnGround(true);pet.hasImpulse=false;pet.handleStartJump(0);pet.aiStep();
        // Remote riders send their position from the client; the server clears velocity after the jump impulse.
        h.assertTrue(Math.abs(pet.getYRot()-90)<.01 && pet.hasImpulse,"Server riding does not steer or accept jump input");
        h.assertTrue(pet.getAttributeValue(Attributes.MOVEMENT_SPEED)==.3,"Riding changed base movement speed");
        p.stopRiding();h.succeed();
    }
}
