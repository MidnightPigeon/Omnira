package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.menu.CrystalProcessingTableMenu;
import com.mcmagic.omnira.mana.ManaState;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_chaos") @PrefixGameTestTemplate(false)
public final class ChaosSpellGameTests {
    private static void advance(FakePlayer p,int ticks){
        for(int i=0;i<ticks;i++){p.tickCount++;ChaosBeam.tick(new PlayerTickEvent.Post(p));}
    }
    private static ItemStack crystal(boolean theory){
        var input=new SimpleContainer(9);
        input.setItem(1,new ItemStack(ModItems.UNORDERED_ELEMENT_CLUSTER.get()));
        if(theory)input.setItem(0,input.getItem(1).copy());
        else input.setItem(4,new ItemStack(ModItems.SHARP_BREATH.get()));
        input.setItem(2,new ItemStack(ModItems.AMPLIFICATION_STAR.get()));
        return CrystalProcessingTableMenu.previewCrystal(input);
    }
    private static FakePlayer caster(GameTestHelper h,double mana){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"chaos-test"));
        p.setPos(h.absoluteVec(new Vec3(5.5,4,5.5)));p.setXRot(0);p.setYRot(0);
        p.setData(ModAttachments.MANA,new ManaState(mana,500));return p;
    }
    @GameTest(template="spell_arena") public static void writing(GameTestHelper h){
        var rng=net.minecraft.util.RandomSource.create(200);
        var combinations=new HashSet<SpellPattern>();
        for(int i=0;i<512;i++)combinations.add(SpellCasting.rollUnordered(rng));
        h.assertTrue(combinations.size()==16,"The two independent draws must allow all 16 pairs");
        var random=crystal(false);
        h.assertTrue(random.get(ModDataComponents.SPELL_PATTERN).unordered() &&
                Collections.frequency(SpellPayload.of(random).keywords(),"enhancement")==2 &&
                SpellPayload.of(random).effects().getFirst().amplifier()==3,"Innate and written enhancement must both apply");
        var materials=com.mcmagic.omnira.guidance.UniversalUnderstanding.materials(random);
        h.assertTrue(materials.stream().filter(s->s.is(ModItems.AMPLIFICATION_STAR.get())).count()==1 &&
                materials.stream().anyMatch(s->s.is(ModItems.UNORDERED_ELEMENT_CLUSTER.get())),"Refunded a phantom material");
        var beam=crystal(true);
        h.assertTrue(beam.get(ModDataComponents.SPELL_PATTERN).chaosTheory() && SpellPayload.of(beam).baseCost()==30 &&
                SpellPayload.of(beam).keywords().contains("error"),"Incorrect theory crystal");
        var slots=new SimpleContainer(9);slots.setItem(0,new ItemStack(ModItems.UNORDERED_ELEMENT_CLUSTER.get()));
        h.assertTrue(!CrystalProcessingTableMenu.validChaosSlot(slots,4,new ItemStack(ModItems.SHARP_BREATH.get())) &&
                !com.mcmagic.omnira.aggregation.AggregationRingBlockEntity.compatible(slots,2,new ItemStack(ModItems.SHARP_BREATH.get())),"Element insertion accepted");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void crystalHeldUntilRelease(GameTestHelper h){
        for(boolean creative:new boolean[]{false,true}){
            var p=caster(h,0);
            p.getAbilities().instabuild=creative;
            var stack=crystal(true);stack.setCount(2);
            p.setItemInHand(InteractionHand.OFF_HAND,stack);
            stack.use(h.getLevel(),p,InteractionHand.OFF_HAND);
            h.assertTrue(!ChaosBeam.active(p) && stack.getCount()==2,"Unfunded cast consumed a crystal");
            p.setData(ModAttachments.MANA,new ManaState(100,500));
            stack.use(h.getLevel(),p,InteractionHand.OFF_HAND);
            h.assertTrue(ChaosBeam.active(p) && stack.getCount()==2,"Crystal disappeared before release");
            ChaosBeam.release(p,p.tickCount);
            ChaosBeam.release(p,p.tickCount);
            h.assertTrue(!ChaosBeam.active(p) && stack.getCount()==(creative?2:1),"Incorrect release consumption");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void channelAndFailure(GameTestHelper h){
        var p=caster(h,140);
        var target=h.spawn(EntityType.IRON_GOLEM,5,4,10);target.setNoAi(true);
        var crystal=crystal(true);p.setItemInHand(InteractionHand.MAIN_HAND,crystal);
        crystal.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        int started=p.tickCount;
        advance(p,ChaosBeam.TRAVEL_TICKS-1);
        h.assertTrue(p.getHealth()==20 && target.getHealth()==100 && p.getData(ModAttachments.MANA).current()==110,"Effect arrived before the ring");
        advance(p,1);
        h.assertTrue(ChaosBeam.active(p) && p.getHealth()==18 && target.getHealth()==92 &&
                Math.abs(p.getData(ModAttachments.MANA).current()-99)<.0001,"First pulse, cost or backlash incorrect");
        for(int i=0;i<20;i++){p.tickCount++;ChaosBeam.tick(new PlayerTickEvent.Post(p));}
        h.assertTrue(p.getHealth()==16 && target.getHealth()==84 && p.getData(ModAttachments.MANA).current()==59,"Second pulse or minimum backlash incorrect");
        ChaosBeam.release(p,started);
        h.assertTrue(!ChaosBeam.active(p),"Release did not stop beam");h.succeed();
    }
    @GameTest(template="spell_arena") public static void turnAndReleaseDuringPulse(GameTestHelper h){
        var p=caster(h,200);
        p.setPos(p.position().add(0,80,0));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        var level=h.getLevel();
        var front=net.minecraft.core.BlockPos.containing(p.getEyePosition().add(0,0,6));
        var back=net.minecraft.core.BlockPos.containing(p.getEyePosition().add(0,0,-6));
        level.setBlockAndUpdate(front,Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(back,Blocks.STONE.defaultBlockState());
        int started=p.tickCount;
        try{
            ChaosBeam.start(p,InteractionHand.MAIN_HAND,ItemStack.EMPTY,new SpellPayload(20,0),1,20);
            advance(p,ChaosBeam.TRAVEL_TICKS-1);
            p.setYRot(180);advance(p,1);
            h.assertTrue(level.isEmptyBlock(back) && !level.isEmptyBlock(front),"Pulse hit the launch target instead of the arrival target");
            p.setYRot(0);
            advance(p,20-ChaosBeam.TRAVEL_TICKS+1);
            ChaosBeam.release(p,started);
            advance(p,20);
            h.assertTrue(!level.isEmptyBlock(front) && p.getData(ModAttachments.MANA).current()==160,"Released pulse still applied or charged again");
        }finally{
            ChaosBeam.stop(p);level.removeBlock(front,false);level.removeBlock(back,false);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void infusionDiameter(GameTestHelper h){
        var p=caster(h,500);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        var level=h.getLevel();
        var origin=net.minecraft.core.BlockPos.containing(p.position()).above(60);
        try{
            for(int layers=0;layers<=3;layers++){
                // Shoot the north face near a corner; measure width, height, depth and the outer boundary.
                p.setPos(origin.getX()+.04,origin.getY()+.96-p.getEyeHeight(),origin.getZ()-8);
                for(int dx=-4;dx<=4;dx++)for(int dy=-4;dy<=4;dy++)for(int dz=0;dz<=4;dz++)
                    level.setBlock(origin.offset(dx,dy,dz),Blocks.STONE.defaultBlockState(),2);
                var payload=new SpellPayload(20,0,List.of(),Collections.nCopies(layers,"infusion"));
                ChaosBeam.start(p,InteractionHand.MAIN_HAND,ItemStack.EMPTY,payload,1,20);
                advance(p,ChaosBeam.TRAVEL_TICKS);
                for(int dx=-4;dx<=4;dx++)for(int dy=-4;dy<=4;dy++)for(int dz=0;dz<=4;dz++){
                    boolean inside=dx*dx+dy*dy+dz*dz<=(layers+.5)*(layers+.5);
                    h.assertTrue(level.isEmptyBlock(origin.offset(dx,dy,dz))==inside,"Incorrect sphere at "+layers+" infusions: "+dx+","+dy+","+dz);
                }
                ChaosBeam.stop(p);
            }
        }finally{
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-4,-4,0),origin.offset(4,4,4)))
                level.removeBlock(pos,false);
            ChaosBeam.stop(p);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void infusionAndTier(GameTestHelper h){
        var p=caster(h,300);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        var pos=net.minecraft.core.BlockPos.containing(p.getEyePosition().add(0,0,6));
        var level=h.getLevel();
        level.setBlockAndUpdate(pos,Blocks.OBSIDIAN.defaultBlockState());
        level.setBlockAndUpdate(pos.above(),Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(pos.below(),Blocks.BEDROCK.defaultBlockState());
        var payload=new SpellPayload(40,0,List.of(),List.of("infusion","infusion"));
        ChaosBeam.start(p,InteractionHand.MAIN_HAND,ItemStack.EMPTY,payload,1.25,40);
        advance(p,ChaosBeam.TRAVEL_TICKS);
        h.assertTrue(level.isEmptyBlock(pos),"25% extra power failed to reach diamond tier");
        h.assertTrue(p.getHealth()==16 && Math.abs(p.getData(ModAttachments.MANA).current()-210.6)<.001,
                "Two surviving bedrock targets must each trigger backlash, mana="+p.getData(ModAttachments.MANA).current());
        ChaosBeam.stop(p);h.succeed();
    }
    @GameTest(template="spell_arena") public static void staffHoldAndReach(GameTestHelper h){
        var p=caster(h,200);
        p.setPos(p.position().add(0,80,0));p.setXRot(-90);
        var staff=com.mcmagic.omnira.item.staff.StaffAssembly.basic().create();
        p.setItemInHand(InteractionHand.MAIN_HAND,staff);
        var inventory=new SimpleContainer(9);
        inventory.setItem(0,new ItemStack(ModItems.UNORDERED_ELEMENT_CLUSTER.get()));
        inventory.setItem(1,inventory.getItem(0).copy());
        inventory.setItem(2,new ItemStack(ModItems.LINGERING_THREAD.get()));
        var grid=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());
        grid.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(
                List.of(CrystalProcessingTableMenu.previewCrystal(inventory))));
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("crystal_grid",0,grid);
        var near=net.minecraft.core.BlockPos.containing(p.getEyePosition().add(0,42,0));
        var far=near.above(4);
        h.getLevel().setBlockAndUpdate(near,Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(far,Blocks.STONE.defaultBlockState());
        try{
            int started=p.tickCount;
            ((com.mcmagic.omnira.item.StaffItem)staff.getItem()).castHeld(p,staff);
            advance(p,ChaosBeam.TRAVEL_TICKS);
            h.assertTrue(ChaosBeam.active(p) && h.getLevel().isEmptyBlock(near) && !h.getLevel().isEmptyBlock(far),"Delayed staff ray must stop at first block beyond 32");
            p.hurt(p.damageSources().generic(),1);
            advance(p,20);
            h.assertTrue(ChaosBeam.active(p) && h.getLevel().isEmptyBlock(far),"Damage interrupted staff channel");
            ChaosBeam.release(p,started);
            h.assertTrue(!ChaosBeam.active(p),"Staff release failed");
        }finally{
            h.getLevel().removeBlock(near,false);h.getLevel().removeBlock(far,false);ChaosBeam.stop(p);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void wheelToggle(GameTestHelper h){
        if(!com.mcmagic.omnira.item.IndustrialWheelItem.available()){h.succeed();return;}
        var p=caster(h,100);p.setPos(p.position().add(0,80,0));p.setXRot(-90);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.INDUSTRIAL_WHEEL.get()));
        var grid=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());
        grid.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(crystal(true))));
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("crystal_grid",0,grid);
        com.mcmagic.omnira.item.IndustrialWheelItem.toggle(p,InteractionHand.MAIN_HAND);
        double first=p.getData(ModAttachments.MANA).current();
        ChaosBeam.release(p,p.tickCount);
        for(int i=0;i<40;i++){
            p.tickCount++;p.getCooldowns().tick();
            ChaosBeam.tick(new PlayerTickEvent.Post(p));
            com.mcmagic.omnira.item.IndustrialWheelItem.tick(new PlayerTickEvent.Post(p));
        }
        h.assertTrue(ChaosBeam.active(p) && first==80 && p.getData(ModAttachments.MANA).current()==40,"Wheel upkeep or toggle incorrect");
        com.mcmagic.omnira.item.IndustrialWheelItem.toggle(p,InteractionHand.MAIN_HAND);
        h.assertTrue(!ChaosBeam.active(p) && !com.mcmagic.omnira.item.IndustrialWheelItem.active(p),"Second click must stop wheel");h.succeed();
    }
    @GameTest(template="spell_arena") public static void missedUpkeepAndOverdraw(GameTestHelper h){
        var p=caster(h,30);p.setPos(p.position().add(0,80,0));p.setXRot(-90);
        var crystal=crystal(true);p.setItemInHand(InteractionHand.MAIN_HAND,crystal);crystal.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(ChaosBeam.active(p) && p.isAlive() && p.getData(ModAttachments.MANA).current()==0,"First empty-space pulse: hp="+p.getHealth()+" mana="+p.getData(ModAttachments.MANA).current());
        p.tickCount+=20;ChaosBeam.tick(new PlayerTickEvent.Post(p));
        h.assertTrue(!ChaosBeam.active(p) && p.getHealth()==20 && p.getData(ModAttachments.MANA).current()==0,"Unpaid upkeep: active="+ChaosBeam.active(p)+" hp="+p.getHealth()+" mana="+p.getData(ModAttachments.MANA).current());
        p.setData(ModAttachments.MANA,new ManaState(10,500));ChaosBeam.backlash(p);
        h.assertTrue(p.isAlive() && p.getData(ModAttachments.MANA).current()==0,"Exact zero triggered overdraw");
        ChaosBeam.backlash(p);h.assertTrue(!p.isAlive(),"Negative mana did not trigger overdraw");h.succeed();
    }

    private static ItemStack equipBeam(FakePlayer p, ItemStack staff, List<ItemStack> crystals) {
        p.setItemInHand(InteractionHand.MAIN_HAND,staff);
        var grid=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());
        grid.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(crystals));
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("crystal_grid",0,grid);
        return grid;
    }
    @GameTest(template="spell_arena") public static void observersDoNotConsumeCrystals(GameTestHelper h){
        for(boolean creative:new boolean[]{false,true}){
            var p=caster(h,200);p.getAbilities().instabuild=creative;
            var stack=crystal(true);stack.setCount(2);
            p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            stack.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
            try{
                for(int i=0;i<3;i++)ChaosBeam.tracking(new net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking(caster(h,200),p));
                h.assertTrue(stack.getCount()==2,"Observers consumed the casting crystal");
                ChaosBeam.release(p,p.tickCount);ChaosBeam.stop(p);
                h.assertTrue(stack.getCount()==(creative?2:1),"Ending consumed incorrect number of crystals");
            }finally{ChaosBeam.stop(p);}
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void wideRuneExtendsBeamAndInfusedSphere(GameTestHelper h){
        for(boolean wheel:new boolean[]{false,true}){
            if(wheel&&!com.mcmagic.omnira.item.IndustrialWheelItem.available())continue;
            var p=caster(h,500);p.setPos(p.position().add(0,80,0));p.setXRot(-90);
            var staff=com.mcmagic.omnira.item.staff.StaffAssembly.basic().withUpgrades(List.of(new ItemStack(ModItems.WIDE_AREA_RUNE.get()))).orElseThrow().create();
            if(wheel){
                staff=new ItemStack(ModItems.INDUSTRIAL_WHEEL.get());
                staff.set(ModDataComponents.WHEEL_RUNE,net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(ModItems.WIDE_AREA_RUNE.get()))));
            }
            var input=new SimpleContainer(9);
            input.setItem(0,new ItemStack(ModItems.UNORDERED_ELEMENT_CLUSTER.get()));input.setItem(1,input.getItem(0).copy());
            input.setItem(2,new ItemStack(ModItems.INFUSION_BUD.get()));
            equipBeam(p,staff,List.of(CrystalProcessingTableMenu.previewCrystal(input)));
            var pos=net.minecraft.core.BlockPos.containing(p.getEyePosition().add(0,40,0));
            var edge=pos.offset(2,0,0);var outside=pos.offset(3,0,0);
            for(var b:List.of(pos,edge,outside))h.getLevel().setBlockAndUpdate(b,Blocks.STONE.defaultBlockState());
            try{
                if(wheel)com.mcmagic.omnira.item.IndustrialWheelItem.toggle(p,InteractionHand.MAIN_HAND);
                else ((com.mcmagic.omnira.item.StaffItem)staff.getItem()).castHeld(p,staff);
                advance(p,ChaosBeam.TRAVEL_TICKS);
                h.assertTrue(ChaosBeam.active(p)&&h.getLevel().isEmptyBlock(pos)&&h.getLevel().isEmptyBlock(edge)&&!h.getLevel().isEmptyBlock(outside),"Wide rune failed range or sphere boundary for wheel="+wheel);
            }finally{
                ChaosBeam.stop(p);com.mcmagic.omnira.item.IndustrialWheelItem.stop(p);
                for(var b:List.of(pos,edge,outside))h.getLevel().removeBlock(b,false);
            }
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void abyssBeamAppliesEffectEachDamagingPulse(GameTestHelper h){
        var curse=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceLocation.parse("cataclysm:abyssal_curse"));
        var fear=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceLocation.parse("cataclysm:abyssal_fear"));
        if(curse.isEmpty()||fear.isEmpty()){h.succeed();return;}
        var p=caster(h,500);var base=com.mcmagic.omnira.item.staff.StaffAssembly.basic();
        var staff=new com.mcmagic.omnira.item.staff.StaffAssembly(base.shaft(),base.reinforcement(),new ItemStack(ModItems.ABYSS_TIP.get()),List.of()).create();
        equipBeam(p,staff,List.of(crystal(true)));
        var target=h.spawn(EntityType.IRON_GOLEM,5,4,10);target.setNoAi(true);
        try{
            ((com.mcmagic.omnira.item.StaffItem)staff.getItem()).castHeld(p,staff);
            for(int i=0;i<2;i++){
                target.removeAllEffects();
                advance(p,i==0?ChaosBeam.TRAVEL_TICKS:20);
                var c=target.getEffect(curse.get());var f=target.getEffect(fear.get());
                h.assertTrue(target.getHealth()<100 && (c!=null)!=(f!=null),"Beam must apply exactly one abyss effect");
                var effect=c!=null?c:f;
                h.assertTrue(effect.getDuration()==600&&effect.getAmplifier()==(c!=null?1:0),"Wrong abyss effect strength");
            }
        }finally{ChaosBeam.stop(p);target.discard();}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void secondBurstChannelsOnceAndStopsRemainingShots(GameTestHelper h){
        var p=caster(h,200);var base=com.mcmagic.omnira.item.staff.StaffAssembly.basic();
        var staff=new com.mcmagic.omnira.item.staff.StaffAssembly(base.shaft(),base.reinforcement(),new ItemStack(ModItems.MULTI_ARCANE_TIP.get()),List.of()).create();
        var first=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
        first.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(ElementType.WATER,ElementType.WATER));
        first.set(ModDataComponents.SPELL_PAYLOAD,SpellPayload.EMPTY);
        var grid=equipBeam(p,staff,List.of(first,crystal(true),first.copy()));
        try{
            ((com.mcmagic.omnira.item.StaffItem)staff.getItem()).castHeld(p,staff);
            p.tickCount+=com.mcmagic.omnira.item.staff.StaffEvents.SHOT_INTERVAL;
            com.mcmagic.omnira.item.staff.StaffEvents.tick(new PlayerTickEvent.Post(p));
            h.assertTrue(ChaosBeam.active(p)&&grid.getOrDefault(ModDataComponents.GRID_CURSOR,0)==2,"Second burst failed to channel or advanced twice");
            double mana=p.getData(ModAttachments.MANA).current();
            ChaosBeam.release(p,p.tickCount);
            p.tickCount+=com.mcmagic.omnira.item.staff.StaffEvents.SHOT_INTERVAL;
            com.mcmagic.omnira.item.staff.StaffEvents.tick(new PlayerTickEvent.Post(p));
            h.assertTrue(!ChaosBeam.active(p)&&grid.getOrDefault(ModDataComponents.GRID_CURSOR,0)==2&&p.getData(ModAttachments.MANA).current()==mana,"Remaining burst fired after channel release");
        }finally{ChaosBeam.stop(p);}
        h.succeed();
    }
}
