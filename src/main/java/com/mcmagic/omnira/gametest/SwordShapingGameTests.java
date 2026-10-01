package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.menu.ArquebusMenu;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.world.dimension.SwordShapingRitual;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_sword_shaping")
@PrefixGameTestTemplate(false)
public final class SwordShapingGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,Item item) {
        var profile=new GameProfile(UUID.randomUUID(),"sword-test");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;
        p.setPos(h.absoluteVec(new Vec3(5.5,2,6.5)));p.setOnGround(true);p.setHealth(1);
        p.getAttribute(ModAttributes.MAX_MANA).setBaseValue(120);
        p.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(100);
        p.setData(ModAttachments.MANA,new ManaState(120,120));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
        return p;
    }
    private static void begin(net.minecraft.server.level.ServerPlayer p) {
        p.startUsingItem(InteractionHand.MAIN_HAND);
        SwordFocus.begin(p,p.getMainHandItem(),((RitualSwordItem)p.getMainHandItem().getItem()).kind);
    }
    @GameTest(template="spell_arena") public static void swordPowerExcludesEnchantmentsAndWideOffhandReach(GameTestHelper h) {
        for(var item:List.of(ModItems.CRYSTALLIZED_NAIL.get(),ModItems.ARCANE_NEEDLE.get())) {
            var p=player(h,item);
            // This un-ticked player has no applied equipment modifiers yet.
            p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(8);
            p.getAttribute(ModAttributes.SPELL_POWER).setBaseValue(1.5);
            var enchant=h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
            p.getMainHandItem().enchant(enchant,1);
            var target=h.spawn(EntityType.COW,8,2,8);target.setNoAi(true);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
            p.setOnGround(false);p.fallDistance=0;
            p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).setBaseValue(1024);
            p.attack(target);
            h.assertTrue(Math.abs(target.getHealth()-87)<.001,"Expected 8 * 1.5 + 1 sharpness damage, got "+(100-target.getHealth()));
            target.discard();
            double reach=p.entityInteractionRange();
            var wide=com.mcmagic.omnira.item.staff.StaffAssembly.basic().withUpgrades(List.of(new ItemStack(ModItems.WIDE_AREA_RUNE.get()))).orElseThrow();
            p.setItemInHand(InteractionHand.OFF_HAND,wide.create());
            h.assertTrue(Math.abs(p.entityInteractionRange()-reach*1.5)<1e-6,"Offhand rune did not extend sword selection reach");
            if(item==ModItems.ARCANE_NEEDLE.get()) {
                var needle=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());
                needle.launch(p,p.getMainHandItem());
                h.assertTrue(needle.flightRange()==15,"Wide rune must extend flying needle to 15 blocks");
                p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                h.assertTrue(needle.flightRange()==15,"Launched needle range changed with equipment");
                var plain=ModEntityTypes.FLYING_NEEDLE.get().create(h.getLevel());
                plain.launch(p,p.getMainHandItem());
                h.assertTrue(plain.flightRange()==10,"Base flying needle range changed");
            }
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            h.assertTrue(p.entityInteractionRange()==reach,"Reach persisted after removing staff");
            p.setItemInHand(InteractionHand.OFF_HAND,wide.create());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
            h.assertTrue(p.entityInteractionRange()==reach,"Rune extended unrelated weapon reach");
        }
        h.succeed();
    }
    private static void advance(net.minecraft.server.level.ServerPlayer p,int from,int to) {for(int t=from;t<=to;t++)SwordFocus.advance(p,p.getMainHandItem(),t);}
    @GameTest(template="spell_arena") public static void nailHealsFourEachSecond(GameTestHelper h) {
        var p=player(h,ModItems.CRYSTALLIZED_NAIL.get());begin(p);advance(p,1,19);
        h.assertTrue(p.getHealth()==1 && p.getData(ModAttachments.MANA).current()==120,"Early nail healing/payment");
        advance(p,20,20);h.assertTrue(p.getHealth()==5 && p.getData(ModAttachments.MANA).current()==110,"Nail first second must heal four for ten mana");
        advance(p,20,20);h.assertTrue(p.getHealth()==5,"Same elapsed tick healed twice");
        advance(p,21,40);h.assertTrue(p.getHealth()==9,"Nail second segment wrong");
        advance(p,41,60);h.assertTrue(p.getHealth()==13 && p.getData(ModAttachments.MANA).current()==90,"Full nail focus must heal twelve for 25% maximum mana");
        SwordFocus.stop(p);h.succeed();
    }
    @GameTest(template="spell_arena") public static void needleHealsOnlyAfterThreeSeconds(GameTestHelper h) {
        var p=player(h,ModItems.ARCANE_NEEDLE.get());p.setOnGround(false);begin(p);advance(p,1,59);
        h.assertTrue(p.getHealth()==1 && p.getData(ModAttachments.MANA).current()==90.5,"Needle must charge progressively without partial healing");
        advance(p,60,60);h.assertTrue(p.getHealth()==13 && p.getData(ModAttachments.MANA).current()==90,"Needle final twelve-point heal wrong");
        SwordFocus.stop(p);h.succeed();
    }
    @GameTest(template="spell_arena") public static void exactManaBudgetAndRealUseTiming(GameTestHelper h) {
        for(var item:List.of(ModItems.CRYSTALLIZED_NAIL.get(),ModItems.ARCANE_NEEDLE.get())) {
            var p=player(h,item);p.getAttribute(ModAttributes.MAX_MANA).setBaseValue(100);p.setData(ModAttachments.MANA,new ManaState(25,100));begin(p);
            for(int remaining=60;remaining>=1;remaining--)item.onUseTick(h.getLevel(),p,p.getMainHandItem(),remaining);
            h.assertTrue(p.getHealth()==13 && p.getData(ModAttachments.MANA).current()<1E-7,"Exact fractional mana budget or use duration failed");
            item.finishUsingItem(p.getMainHandItem(),h.getLevel(),p);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void focusCancellationAndRestrictions(GameTestHelper h) {
        var p=player(h,ModItems.CRYSTALLIZED_NAIL.get());begin(p);advance(p,1,20);p.setOnGround(false);advance(p,21,60);
        h.assertTrue(p.getHealth()==5 && p.getData(ModAttachments.MANA).current()==110,"Jumping must preserve completed nail segments only");
        h.assertTrue(!SwordFocus.canBegin(p,RitualSwordItem.Kind.NAIL),"Airborne nail allowed");
        var n=player(h,ModItems.ARCANE_NEEDLE.get());begin(n);advance(n,1,30);SwordFocus.stop(n);advance(n,31,60);
        h.assertTrue(n.getHealth()==1 && n.getData(ModAttachments.MANA).current()==105,"Interrupted needle refunded or healed");
        var boat=h.spawn(EntityType.BOAT,4,2,4);h.assertTrue(n.startRiding(boat,true),"Fixture could not mount boat");
        h.assertTrue(!SwordFocus.canBegin(n,RitualSwordItem.Kind.NEEDLE),"Needle usable while riding");h.succeed();
    }
    @GameTest(template="spell_arena") public static void needleHoversAndReleasesWithoutFlags(GameTestHelper h) {
        var p=player(h,ModItems.ARCANE_NEEDLE.get());p.setOnGround(false);var origin=p.position();begin(p);
        p.setPos(origin.add(1,-1,1));p.setDeltaMovement(1,-1,1);
        SwordFocus.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(p.position().equals(origin) && p.getDeltaMovement().equals(Vec3.ZERO),"Needle did not lock airborne position");
        SwordFocus.stop(p);p.stopUsingItem();p.setPos(origin.add(1,0,0));
        SwordFocus.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(p.position().equals(origin.add(1,0,0)) && !p.isNoGravity(),"Focus left a movement/gravity lock");h.succeed();
    }
    private static ItemStack crystal(ElementType target,ElementType shape) {
        var c=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());c.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(target,shape));
        c.set(ModDataComponents.SPELL_PAYLOAD,new SpellPayload(30,4));return c;
    }
    @GameTest(template="spell_arena") public static void swordSlotAndHeldRepair(GameTestHelper h) {
        var p=player(h,ModItems.CRYSTALLIZED_NAIL.get());var sword=p.getMainHandItem();
        var menu=ArquebusMenu.sword(1,p.getInventory(),0);
        for(var target:List.of(ElementType.WATER,ElementType.EARTH,ElementType.FIRE,ElementType.AIR))for(var shape:List.of(ElementType.WATER,ElementType.EARTH,ElementType.FIRE,ElementType.AIR))
            h.assertTrue(menu.getSlot(0).mayPlace(crystal(target,shape))==(target==ElementType.WATER && shape==ElementType.EARTH),"Invalid sword crystal accepted");
        var valid=crystal(ElementType.WATER,ElementType.EARTH);menu.getSlot(0).set(valid);menu.broadcastChanges();
        h.assertTrue(ItemStack.matches(RitualSwordItem.crystal(sword),valid),"Spell crystal not persisted in sword");
        sword.setDamageValue(2);HeldManaRepair.repair(sword,p);
        h.assertTrue(sword.getDamageValue()==1 && p.getData(ModAttachments.MANA).current()==115 && sword.getMaxDamage()==1024,"Sword repair differs from infused pickaxe");
        h.assertTrue(sword.is(net.minecraft.tags.ItemTags.SWORDS),"Sword enchantment tag missing");h.succeed();
    }
    @GameTest(template="spell_arena") public static void weaponAttributesAndEnchantments(GameTestHelper h) {
        for(var item:List.of(ModItems.CRYSTALLIZED_NAIL.get(),ModItems.ARCANE_NEEDLE.get())) {
            var stack=new ItemStack(item);var modifiers=stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
            h.assertTrue(modifiers.stream().anyMatch(m->m.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) && m.modifier().amount()==7),"Expected eight total attack damage");
            h.assertTrue(modifiers.stream().anyMatch(m->m.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED) && Math.abs(m.modifier().amount()+2.2)<1E-6),"Expected 1.8 attack speed");
            var enchants=h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            for(var enchant:List.of(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,net.minecraft.world.item.enchantment.Enchantments.LOOTING,net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE))
                h.assertTrue(enchants.getHolderOrThrow(enchant).value().isSupportedItem(stack),"Sword enchantment rejected");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void spellContactAndDownstrike(GameTestHelper h) {
        var p=player(h,ModItems.CRYSTALLIZED_NAIL.get());p.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(0);
        p.getAttribute(ModAttributes.SPELL_POWER).setBaseValue(1.5);
        p.getMainHandItem().set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal(ElementType.WATER,ElementType.EARTH))));
        var cow=h.spawn(EntityType.COW,5,2,5);cow.setNoAi(true);cow.invulnerableTime=20;
        RitualSwordItem.contact(p,cow,true);
        h.assertTrue(cow.getHealth()==4 && p.getData(ModAttachments.MANA).current()==90,"On-hit spell suppressed, wrong power or wrong cost");
        p.getMainHandItem().remove(DataComponents.CONTAINER);p.setOnGround(false);p.setXRot(80);p.setPos(cow.getX(),cow.getBoundingBox().maxY+.1,cow.getZ());p.setDeltaMovement(0,-.3,0);
        RitualSwordItem.contact(p,cow,true);h.assertTrue(p.getDeltaMovement().y==.72,"Downstrike did not bounce");h.succeed();
    }
    private static PureVesselBlockEntity ritual(GameTestHelper h,boolean needle) {
        var pos=h.absolutePos(new BlockPos(5,2,5));var level=h.getLevel();level.setBlockAndUpdate(pos,ModBlocks.PURE_VESSEL.get().defaultBlockState());
        var vessel=(PureVesselBlockEntity)level.getBlockEntity(pos);vessel.setItem(0,new ItemStack(Items.IRON_SWORD));
        var materials=needle?new Item[]{Items.IRON_INGOT,DreamContent.SHADOW_BERRIES.get().asItem(),ModItems.MANA_ENGINE.get(),ModItems.ANALYSIS_CRYSTAL.get()}
                :new Item[]{Items.BONE,DreamContent.LIGHT_CRYSTAL_CORE.get().asItem(),ModItems.SPIRITUAL_CRYSTAL.get(),ModItems.RESONANCE_CRYSTAL.get()};
        for(int i=0;i<4;i++){var a=SwordShapingRitual.ANCHORS[i];var at=pos.offset(a[0],0,a[1]);level.setBlockAndUpdate(at,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());((CrystalPedestalBlockEntity)level.getBlockEntity(at)).setItem(0,new ItemStack(materials[i]));}
        return vessel;
    }
    @GameTest(template="spell_arena") public static void ritualConsumesAndCreatesMist(GameTestHelper h) {
        var v=ritual(h,true);v.removeItemNoUpdate(0);var p=player(h,Items.IRON_SWORD);
        var hit=new BlockHitResult(Vec3.atCenterOf(v.getBlockPos()),net.minecraft.core.Direction.UP,v.getBlockPos(),false);
        v.getBlockState().useItemOn(p.getMainHandItem(),h.getLevel(),p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(v.ritual.active() && p.getMainHandItem().isEmpty(),"Right-click did not start unordered ritual and consume weapon");
        for(int i=0;i<159;i++)v.ritual.tick();h.assertTrue(!v.getItem(0).isEmpty(),"Ritual completed too early");
        v.ritual.tick();h.assertTrue(h.getLevel().getBlockState(v.getBlockPos()).isAir(),"Vessel was not consumed");
        var area=new AABB(v.getBlockPos()).inflate(3);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
        h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.ARCANE_NEEDLE.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Missing or duplicated ritual output");
        h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(ModItems.ANALYSIS_CRYSTAL.get()) || e.getItem().is(Items.IRON_SWORD) || e.getItem().is(ModItems.PURE_VESSEL.get())),"Consumed input returned");
        h.assertTrue(h.getLevel().getEntitiesOfClass(com.mcmagic.omnira.entity.ShadowMist.class,area).isEmpty(),"Colored collapse should not spawn dark mist");h.succeed();
    }
    @GameTest(template="spell_arena") public static void ritualPersistsAndRefundsOnBreak(GameTestHelper h) {
        var v=ritual(h,false);var p=player(h,Items.AIR);h.assertTrue(v.ritual.start(p),"Nail recipe missing");
        for(int i=0;i<45;i++)v.ritual.tick();var registries=h.getLevel().registryAccess();
        var restored=new PureVesselBlockEntity(v.getBlockPos(),v.getBlockState());restored.loadWithComponents(v.saveWithFullMetadata(registries),registries);
        h.assertTrue(restored.ritual.active() && restored.ritual.progress()==45,"Ritual progress lost on reload");
        h.getLevel().destroyBlock(v.getBlockPos(),false);
        var north=(CrystalPedestalBlockEntity)h.getLevel().getBlockEntity(v.getBlockPos().north(2));
        h.assertTrue(north.getItem(0).is(Items.BONE) && north.getItem(0).getCount()==1,"Absorbed material lost on interruption");h.succeed();
    }
    @GameTest(template="spell_arena") public static void vanillaAttackTriggersPayload(GameTestHelper h) {
        var p=player(h,ModItems.CRYSTALLIZED_NAIL.get());p.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(0);
        p.getMainHandItem().set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal(ElementType.WATER,ElementType.EARTH))));
        var cow=h.spawn(EntityType.COW,5,2,5);cow.setNoAi(true);
        p.attack(cow);
        h.assertTrue(p.getData(ModAttachments.MANA).current()==90,"Vanilla attack did not invoke loaded spell");h.succeed();
    }
    @GameTest(template="spell_arena") public static void hoverFollowsSableTranslationAndRotation(GameTestHelper h) {
        if(!net.neoforged.fml.ModList.get().isLoaded("sable")){h.succeed();return;}
        var p=player(h,ModItems.ARCANE_NEEDLE.get());
        var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(h.getLevel());
        var pose=new dev.ryanhcode.sable.companion.math.Pose3d();pose.position().set(p.getX(),p.getY(),p.getZ());
        var ship=container.allocateNewSubLevel(pose);
        try {
            ((dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension)p).sable$setTrackingSubLevel(ship);
            var local=ship.logicalPose().transformPositionInverse(p.position());begin(p);
            ship.logicalPose().position().add(4,2,-3);ship.logicalPose().orientation().rotateY(.4);
            SwordFocus.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            h.assertTrue(p.position().distanceTo(ship.logicalPose().transformPosition(local))<.00001,"Needle anchored in world coordinates instead of ship coordinates");
            SwordFocus.stop(p);h.succeed();
        } finally {
            ((dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension)p).sable$setTrackingSubLevel(null);
            container.removeSubLevel(ship,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
        }
    }
}
