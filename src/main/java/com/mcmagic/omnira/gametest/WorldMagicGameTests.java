package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.world.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.*;

@GameTestHolder("omnira_world_magic")
@PrefixGameTestTemplate(false)
public final class WorldMagicGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"world-magic-test"));p.moveTo(h.absolutePos(new BlockPos(5,3,5)).getCenter());return p;}
    @GameTest(template="spell_arena") public static void pickupKeepsUnacceptedItems(GameTestHelper h){
        var p=player(h);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,62));
        var drop=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),p.getX()+10,p.getY()+3,p.getZ(),new ItemStack(Items.DIAMOND,7));
        drop.setDeltaMovement(1,1,1);
        WorldRotation.collect(drop,p);h.assertTrue(!drop.isRemoved()&&drop.getItem().getCount()==5&&p.getInventory().getItem(0).getCount()==64,"Pickup lost excess items");
        h.assertTrue(drop.position().distanceToSqr(p.position())<.000001&&drop.getDeltaMovement().equals(net.minecraft.world.phys.Vec3.ZERO),"Excess items not gathered at feet");
        drop.teleportTo(p.getX()+10,p.getY(),p.getZ());
        WorldRotation.collect(drop,p);h.assertTrue(drop.getItem().getCount()==5&&drop.position().distanceToSqr(p.position())<.000001,"Full inventory did not gather intact items");h.succeed();
    }
    @GameTest(template="spell_arena") public static void reloadComposesAndIgnoresDelayAndPower(GameTestHelper h){
        var slots=new net.minecraft.world.SimpleContainer(9);slots.setItem(4,new ItemStack(com.mcmagic.omnira.myriad.WorldSeedContent.ACTIVE.get()));
        slots.setItem(2,new ItemStack(Items.CLOCK));slots.setItem(3,new ItemStack(Items.IRON_BLOCK));
        var effect=com.mcmagic.omnira.menu.CrystalProcessingTableMenu.composeEffects(slots).getFirst();
        h.assertTrue(effect.operation().equals("reload")&&effect.delay()==0&&effect.enhancement()==1&&effect.physicalDamage(1000)==0,"Reload composed ordinary damage or delay");
        effect=com.mcmagic.omnira.item.staff.StaffEnhancements.apply(new SpellPayload(30,0,List.of(effect)),List.of("delay")).effects().getFirst();
        h.assertTrue(effect.delay()==0,"Staff reintroduced delay");h.succeed();
    }
    @GameTest(template="spell_arena") public static void reloadPreservesAirAndIncompleteBlocks(GameTestHelper h){
        var p=player(h);var effect=SpellEffect.utility("reload",0,0,0);
        for(var block:List.of(Blocks.AIR,Blocks.OAK_STAIRS,Blocks.CHEST,ModBlocks.CRYSTAL_PROCESSING_TABLE.get())){
            var at=new BlockPos(3,3,3);h.setBlock(at,block);var before=h.getBlockState(at);
            h.assertTrue(!ReloadEffect.block(effect,p,h.absolutePos(at))&&h.getBlockState(at).equals(before),"Reload affected air, incomplete geometry or container");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void baseReloadReplacesBlockOnlyWithVanilla(GameTestHelper h){
        var p=player(h);var at=new BlockPos(4,4,4);h.setBlock(at,Blocks.STONE);
        for(int i=0;i<32;i++){
            var before=h.getBlockState(at).getBlock();h.assertTrue(ReloadEffect.block(SpellEffect.utility("reload",0,0,0),p,h.absolutePos(at)),"Full block reload rejected");
            var state=h.getBlockState(at);h.assertTrue(state.getBlock()!=before&&net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("minecraft"),"Base reload used non-vanilla or same block");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void reloadKeepsCreatureFamilies(GameTestHelper h){
        var cow=h.spawn(EntityType.COW,new BlockPos(3,4,3));
        h.assertTrue(ReloadEffect.living(SpellEffect.utility("reload",0,0,0),cow)&&cow.isRemoved(),"Friendly reload failed");
        var zombie=h.spawn(EntityType.ZOMBIE,new BlockPos(8,4,8));
        h.assertTrue(!ReloadEffect.living(SpellEffect.utility("reload",0,0,0),zombie)&&!zombie.isRemoved(),"Base reload affected hostile");
        h.assertTrue(ReloadEffect.living(SpellEffect.utility("reload",1,0,0),zombie)&&zombie.isRemoved(),"Enhanced hostile reload failed");
        var bee=h.spawn(EntityType.BEE,new BlockPos(9,6,3));h.assertTrue(WorldEntityKinds.kind(bee)==WorldEntityKinds.Kind.NEUTRAL,"Neutral misclassified");
        var stand=new net.minecraft.world.entity.decoration.ArmorStand(EntityType.ARMOR_STAND,h.getLevel());
        h.assertTrue(!ReloadEffect.living(SpellEffect.utility("reload",1,0,1),stand),"Decoration transformed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rotationRestoresPlayerAndCannotDiscountCooldown(GameTestHelper h)throws Exception{
        var p=player(h);p.setHealth(2);p.getFoodData().setFoodLevel(1);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100));
        var pick=new ItemStack(Items.IRON_PICKAXE);pick.setDamageValue(100);p.getInventory().setItem(0,pick);
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(11,100));
        p.getAttribute(ModAttributes.COOLDOWN_REDUCTION).setBaseValue(.5);
        h.assertTrue(WorldRotation.cast(p),"Rotation rejected");
        h.assertTrue(p.getHealth()==p.getMaxHealth()&&p.getFoodData().getFoodLevel()==20&&p.getActiveEffects().isEmpty()&&pick.getDamageValue()==0,"Player restoration incomplete");
        h.assertTrue(p.getData(ModAttachments.MANA).current()==0,"All-mana debit was discounted");
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(100,100));
        h.assertTrue(!WorldRotation.cast(p)&&p.getData(ModAttachments.MANA).current()==100,"Cooldown bypass consumed mana");
        h.assertTrue(p.getData(ModAttachments.WORLD_ROTATION_READY)-p.server.overworld().getGameTime()==1200,"Fixed cooldown changed");
        WorldRotation.cancel(p.getUUID());h.succeed();
    }
    @GameTest(template="spell_arena") public static void rotationErasesHostilesWithoutLootAndHalvesBoss(GameTestHelper h){
        var p=player(h);var zombie=h.spawn(EntityType.ZOMBIE,new BlockPos(3,5,3));WorldRotation.affect(zombie,p);h.assertTrue(zombie.isRemoved(),"Hostile not erased");
        var wither=h.spawn(EntityType.WITHER,new BlockPos(8,9,8));float maximum=wither.getMaxHealth();WorldRotation.affect(wither,p);
        h.assertTrue(!wither.isRemoved()&&wither.getHealth()==maximum/2,"Boss did not lose half maximum health");
        WorldRotation.affect(wither,p);h.assertTrue(wither.isRemoved(),"Half-health boss not erased");
        var pig=h.spawn(EntityType.PIG,new BlockPos(6,4,6));WorldRotation.affect(pig,p);h.assertTrue(pig.isInLove()&&!pig.isRemoved(),"Passive animal not put in love");
        var calm=h.spawn(EntityType.PIGLIN,new BlockPos(10,4,6));WorldRotation.affect(calm,p);h.assertTrue(!calm.isRemoved(),"Calm neutral classified as hostile");
        var spider=h.spawn(EntityType.SPIDER,new BlockPos(11,4,7));WorldRotation.affect(spider,p);h.assertTrue(!spider.isRemoved(),"Calm spider classified as hostile");
        var angry=h.spawn(EntityType.BEE,new BlockPos(9,6,3));angry.setRemainingPersistentAngerTime(100);WorldRotation.affect(angry,p);h.assertTrue(angry.isRemoved(),"Angry neutral survived");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rotationMaturesCropCrystalAndColumn(GameTestHelper h){
        var at=new BlockPos(4,3,4);h.setBlock(at.below(),Blocks.FARMLAND);h.setBlock(at,Blocks.WHEAT);WorldGrowth.mature(h.getLevel(),h.absolutePos(at));
        h.assertTrue(((CropBlock)Blocks.WHEAT).isMaxAge(h.getBlockState(at)),"Crop not mature");
        var wart=new BlockPos(4,3,6);h.setBlock(wart.below(),Blocks.SOUL_SAND);h.setBlock(wart,Blocks.NETHER_WART);WorldGrowth.mature(h.getLevel(),h.absolutePos(wart));
        h.assertTrue(h.getBlockState(wart).getValue(NetherWartBlock.AGE)==3,"Non-bonemealable crop not mature");
        at=new BlockPos(8,3,4);h.setBlock(at,Blocks.BUDDING_AMETHYST);WorldGrowth.mature(h.getLevel(),h.absolutePos(at));
        h.assertTrue(h.getBlockState(at.above()).is(Blocks.AMETHYST_CLUSTER),"Crystal not mature");
        at=new BlockPos(4,3,8);h.setBlock(at.below(),Blocks.SAND);h.setBlock(at.below().east(),Blocks.WATER);h.setBlock(at,Blocks.SUGAR_CANE);WorldGrowth.mature(h.getLevel(),h.absolutePos(at));
        h.assertTrue(h.getBlockState(at.above(3)).is(Blocks.SUGAR_CANE)&&!h.getBlockState(at.above(4)).is(Blocks.SUGAR_CANE),"Column height not four");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rotationFinishesOnlyCurrentFurnaceItem(GameTestHelper h){
        var at=new BlockPos(5,3,5);h.setBlock(at,Blocks.FURNACE);var furnace=(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)h.getBlockEntity(at);
        furnace.setItem(0,new ItemStack(Items.IRON_ORE,2));furnace.setItem(1,new ItemStack(Items.COAL));
        net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(h.getLevel(),h.absolutePos(at),h.getBlockState(at),furnace);
        WorldRotationHooks.machine(furnace,player(h));
        h.assertTrue(furnace.getItem(0).getCount()==1&&furnace.getItem(2).is(Items.IRON_INGOT)&&furnace.getItem(2).getCount()==1,"Furnace did not finish exactly once");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rotationFillsStandardEnergyEvenWhenReceiveLimited(GameTestHelper h){
        var energy=new net.neoforged.neoforge.energy.EnergyStorage(100000,1,1,0);
        ((com.mcmagic.omnira.mixin.WorldRotationEnergyAccessor)energy).omnira$energy(energy.getMaxEnergyStored());h.assertTrue(energy.getEnergyStored()==100000,"Battery remained receive-rate limited");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rotationCompletesReversalAndLeavesEnergyFull(GameTestHelper h){
        var at=new BlockPos(5,3,5);h.setBlock(at,com.mcmagic.omnira.reversal.ReversalContent.MACHINE.get());
        var be=(com.mcmagic.omnira.reversal.ReversalBlockEntity)h.getBlockEntity(at);
        be.setItem(0,new ItemStack(ModItems.TIME_WARP_POINT.get(),2));be.setItem(2,new ItemStack(ModItems.SPACE_MICROCORE.get()));be.tick();
        h.assertTrue(be.progress()>0,"Reversal job did not start");WorldRotationHooks.machine(be,player(h));
        h.assertTrue(be.getItem(1).is(ModItems.TIME_MICROCORE.get())&&be.energy()==64,"Completion left stored energy below capacity");h.succeed();
    }
    @GameTest(template="spell_arena") public static void omniWritingReservesAllEightAndOnlyAdvancedRing(GameTestHelper h){
        var core=new ItemStack(ModItems.OMNI_MICROCORE.get());var inventory=new net.minecraft.world.SimpleContainer(12);inventory.setItem(0,core);
        h.assertTrue(!com.mcmagic.omnira.menu.CrystalProcessingTableMenu.isTargetMicrocore(core),"Ordinary table accepts ultimate core");
        for(int i=1;i<8;i++)h.assertTrue(!com.mcmagic.omnira.aggregation.AggregationRingBlockEntity.compatible(inventory,i,new ItemStack(Items.IRON_BLOCK)),"Ultimate core did not reserve node "+i);
        var crystal=new ItemStack(ModItems.WORLD_ROTATION_CRYSTAL.get());h.assertTrue(crystal.get(ModDataComponents.SPELL_PATTERN).worldRotation(),"Special crystal missing fixed pattern");h.succeed();
    }
    @GameTest(template="spell_arena") public static void advancedRingProducesUltimateCrystal(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));var p=AdvancedForgeGameTests.player(h,pos);h.getLevel().addFreshEntity(p);
        try{
            com.mcmagic.omnira.aggregation.AggregationStructure.assemble(h.getLevel(),pos,Direction.NORTH);
            var ring=(com.mcmagic.omnira.aggregation.AggregationRingBlockEntity)h.getLevel().getBlockEntity(pos);
            ring.setItem(0,new ItemStack(ModItems.OMNI_MICROCORE.get()));ring.setItem(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
            ring.setItem(9,new ItemStack(ModItems.SPELL_INK.get()));ring.setItem(10,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
            h.assertTrue(ring.start(p),"Ultimate writing not accepted");for(int i=0;i<160;i++)ring.tick();
            h.assertTrue(ring.getItem(11).is(ModItems.WORLD_ROTATION_CRYSTAL.get())&&ring.getItem(0).isEmpty()&&ring.getItem(9).getDamageValue()==1,"Ultimate writing recipe consumed incorrect materials");
        }finally{p.discard();}h.succeed();
    }
}
