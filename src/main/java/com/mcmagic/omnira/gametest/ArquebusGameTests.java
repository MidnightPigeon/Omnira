package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.ArcaneArquebusItem;
import com.mcmagic.omnira.menu.ArquebusMenu;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_arquebus")
@PrefixGameTestTemplate(false)
public final class ArquebusGameTests {
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void ancestorConcealmentTiming(GameTestHelper h) {
        var player=player(h);var gun=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());
        gun.set(ModDataComponents.ARQUEBUS_PLUGIN,com.mcmagic.omnira.item.ArquebusPlugin.ANCESTOR_LAUNCHER);
        player.setItemInHand(InteractionHand.MAIN_HAND,gun);
        player.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(100);
        player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(29,100));
        com.mcmagic.omnira.item.ArquebusEvents.concealTick(player);
        h.runAfterDelay(59,()->{
            com.mcmagic.omnira.item.ArquebusEvents.concealTick(player);
            h.assertTrue(player.getData(ModAttachments.MANA).current()==29 && !player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),"Concealment triggered too early");
        });
        h.runAfterDelay(60,()->{
            com.mcmagic.omnira.item.ArquebusEvents.concealTick(player);
            com.mcmagic.omnira.item.ArquebusEvents.concealTick(player);
            h.assertTrue(player.getData(ModAttachments.MANA).current()==19,"Concealment was discounted or charged twice");
            h.assertTrue(player.getEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY).getDuration()==100,"Expected 5 seconds of invisibility");
            player.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        });
        h.runAfterDelay(120,()->{
            com.mcmagic.omnira.item.ArquebusEvents.concealTick(player);
            h.assertTrue(player.getData(ModAttachments.MANA).current()==9 && player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),"Concealment not renewed at 3 seconds");
            player.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        });
        h.runAfterDelay(180,()->{
            com.mcmagic.omnira.item.ArquebusEvents.concealTick(player);
            h.assertTrue(player.getData(ModAttachments.MANA).current()==9 && !player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),"Insufficient mana granted concealment");
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,gun);
            com.mcmagic.omnira.item.ArquebusEvents.concealTick(player);h.succeed();
        });
    }
    @GameTest(template="spell_arena")
    public static void ghostSelectsThroughWallsWithinRange(GameTestHelper h) {
        var player=player(h);player.setPos(player.getX(),230,player.getZ());player.setYRot(0);player.setXRot(0);
        var eye=player.getEyePosition();var wall=BlockPos.containing(eye.add(0,0,2));
        h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        var cow=h.spawn(EntityType.COW,4,4,4);cow.setNoAi(true);cow.setNoGravity(true);
        cow.setPos(eye.add(0,-cow.getBbHeight()/2,8));
        var pattern=new SpellPattern(ElementType.FIRE,ElementType.AIR);
        h.assertTrue(SpellCasting.castArquebus(player,pattern,new SpellPayload(30,4),1,
                com.mcmagic.omnira.item.ArquebusPlugin.KINGS_NEW_CLOTHES),"Through-wall target failed");
        var shot=h.getLevel().getEntitiesOfClass(SpellEntity.class,player.getBoundingBox().inflate(3),e->e.ownedBy(player)).getFirst();
        for(int i=0;i<30 && shot.isAlive();i++)shot.tick();
        h.assertTrue(cow.getHealth()==6,"Target selected wall instead of entity");
        cow.discard();h.getLevel().removeBlock(wall,false);
        var far=BlockPos.containing(eye.add(0,0,37));h.getLevel().getChunkAt(far);h.getLevel().setBlockAndUpdate(far,Blocks.STONE.defaultBlockState());
        h.assertTrue(!SpellCasting.castArquebus(player,pattern,SpellPayload.EMPTY,1,
                com.mcmagic.omnira.item.ArquebusPlugin.KINGS_NEW_CLOTHES),"Ghost TARGET exceeded 36 blocks");
        h.getLevel().removeBlock(far,false);h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=25)
    public static void changingGunCancelsFollowUp(GameTestHelper h) {
        var player=player(h);var gun=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());
        gun.set(ModDataComponents.ARQUEBUS_PLUGIN,com.mcmagic.omnira.item.ArquebusPlugin.KINGS_NEW_CLOTHES);
        gun.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal(ElementType.AIR,ElementType.AIR))));
        player.setItemInHand(InteractionHand.MAIN_HAND,gun);
        h.assertTrue(ArcaneArquebusItem.castLoaded(player,gun),"First shot failed");
        player.setItemInHand(InteractionHand.MAIN_HAND,gun.copy());
        h.runAfterDelay(6,()->{
            com.mcmagic.omnira.item.ArquebusEvents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            h.assertTrue(player.getData(ModAttachments.MANA).current()==955,"Swapping guns failed to cancel second charge");h.succeed();
        });
    }
    @GameTest(template="spell_arena")
    public static void ghostUpgradeExclusive(GameTestHelper h) {
        var input=new SimpleContainer(7);var gun=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());
        gun.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal(ElementType.AIR,ElementType.AIR))));
        input.setItem(6,gun);input.setItem(2,new ItemStack(ModItems.KINGS_NEW_CLOTHES.get()));
        var wrapped=new AssemblyRecipe.Input(input);var recipe=recipe(h,"arquebus_ghost_upgrade");
        h.assertTrue(recipe.matches(wrapped,h.getLevel()),"Ghost installation must match");
        var upgraded=recipe.assemble(wrapped,h.getLevel().registryAccess());
        h.assertTrue(com.mcmagic.omnira.item.ArquebusPlugin.of(upgraded)==com.mcmagic.omnira.item.ArquebusPlugin.KINGS_NEW_CLOTHES
                && ItemStack.matches(ArcaneArquebusItem.crystal(gun),ArcaneArquebusItem.crystal(upgraded)),"Wrong plugin or lost crystal");
        input.setItem(6,upgraded);h.assertTrue(!recipe.matches(wrapped,h.getLevel()),"Duplicate ghost plugin accepted");
        input.setItem(2,new ItemStack(ModItems.ANCESTOR_LAUNCHER.get()));
        h.assertTrue(!recipe(h,"arquebus_upgrade").matches(wrapped,h.getLevel()),"Ancestor accepted on ghost gun");
        gun.set(ModDataComponents.ANCESTOR_LAUNCHER,true);input.setItem(6,gun);input.setItem(2,new ItemStack(ModItems.KINGS_NEW_CLOTHES.get()));
        h.assertTrue(!recipe.matches(wrapped,h.getLevel()),"Ghost accepted on legacy ancestor gun");
        var ops=h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        h.assertTrue(ItemStack.matches(upgraded,ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,upgraded).getOrThrow()).getOrThrow()),"Ghost component serialization failed");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=30)
    public static void ghostDoubleShot(GameTestHelper h) {
        var player=player(h);player.setPos(player.getX(),220,player.getZ());
        var gun=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());
        gun.set(ModDataComponents.ARQUEBUS_PLUGIN,com.mcmagic.omnira.item.ArquebusPlugin.KINGS_NEW_CLOTHES);
        gun.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal(ElementType.AIR,ElementType.AIR))));
        player.setItemInHand(InteractionHand.MAIN_HAND,gun);player.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(5);
        h.assertTrue(ArcaneArquebusItem.castLoaded(player,gun),"Ghost shot failed");
        h.assertTrue(player.getData(ModAttachments.MANA).current()==960,"Ghost first cost must be 30*1.5-5");
        h.runAfterDelay(5,()->{
            com.mcmagic.omnira.item.ArquebusEvents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            h.assertTrue(player.getData(ModAttachments.MANA).current()==960,"Second shot too early");
        });
        h.runAfterDelay(6,()->{
            com.mcmagic.omnira.item.ArquebusEvents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            h.assertTrue(player.getData(ModAttachments.MANA).current()==920,"Second shot not charged once");
            var shots=h.getLevel().getEntitiesOfClass(SpellEntity.class,player.getBoundingBox().inflate(10),e->e.ownedBy(player));
            h.assertTrue(shots.size()==2 && shots.stream().allMatch(s->s.ghost() && !s.explosiveImpact()
                    && Math.abs(s.getDeltaMovement().length()-.45)<1e-6),"Ghost speed or double shot incorrect");h.succeed();
        });
    }
    @GameTest(template="spell_arena")
    public static void ghostPassesWallsButHitsEntitiesAndSelectedBlocks(GameTestHelper h) {
        var owner=player(h);Vec3 origin=owner.position().add(0,30,0);owner.setPos(origin.add(-5,0,0));
        var wall=BlockPos.containing(origin.add(0,0,1));h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        var slab=wall.south();h.getLevel().setBlockAndUpdate(slab,Blocks.STONE_SLAB.defaultBlockState());
        var cow=h.spawn(EntityType.COW,4,4,4);cow.setNoAi(true);cow.setPos(origin.add(0,0,3));
        var shot=SpellEntity.spawn(h.getLevel(),owner,SpellEntity.Kind.PROJECTILE,origin);
        shot.ghost(true,null);shot.configure(1,new SpellPayload(30,4));shot.setDeltaMovement(0,0,.45);
        for(int i=0;i<12 && shot.isAlive();i++)shot.tick();
        h.assertTrue(!shot.isAlive() && cow.getHealth()==6,"Ghost must cross full and partial blocks and stop at entity");
        h.assertTrue(h.getLevel().getBlockState(wall).is(Blocks.STONE),"Unselected wall changed");
        var selected=SpellEntity.spawn(h.getLevel(),owner,SpellEntity.Kind.PROJECTILE,origin);
        selected.ghost(true,wall);selected.setDeltaMovement(0,0,.45);
        for(int i=0;i<5 && selected.isAlive();i++)selected.tick();
        h.assertTrue(!selected.isAlive(),"Selected block did not stop ghost");
        h.getLevel().removeBlock(wall,false);h.getLevel().removeBlock(slab,false);cow.discard();h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=230)
    public static void ghostSenseTimingAndFixedCost(GameTestHelper h) {
        var player=player(h);var gun=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());
        gun.set(ModDataComponents.ARQUEBUS_PLUGIN,com.mcmagic.omnira.item.ArquebusPlugin.KINGS_NEW_CLOTHES);
        player.setItemInHand(InteractionHand.MAIN_HAND,gun);player.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(100);
        player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(99,100));
        com.mcmagic.omnira.item.ArquebusEvents.senseTick(player);
        h.runAfterDelay(99,()->{
            com.mcmagic.omnira.item.ArquebusEvents.senseTick(player);
            h.assertTrue(player.getData(ModAttachments.MANA).current()==99,"Perception fired before 5 seconds");
        });
        h.runAfterDelay(100,()->{
            com.mcmagic.omnira.item.ArquebusEvents.senseTick(player);
            com.mcmagic.omnira.item.ArquebusEvents.senseTick(player);
            h.assertTrue(player.getData(ModAttachments.MANA).current()==49,"Perception cost discounted or charged twice");
        });
        h.runAfterDelay(200,()->{
            com.mcmagic.omnira.item.ArquebusEvents.senseTick(player);
            h.assertTrue(player.getData(ModAttachments.MANA).current()==49,"Insufficient perception mana spent");h.succeed();
        });
    }
    private static ItemStack crystal(ElementType target,ElementType shape) {
        var stack=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
        stack.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(target,shape));
        stack.set(ModDataComponents.SPELL_PAYLOAD,new SpellPayload(30,4));return stack;
    }
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"arquebus-test"));
        player.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4,4,4))));
        player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(1000,1000));
        return player;
    }
    private static AssemblyRecipe recipe(GameTestHelper h,String id) {
        return (AssemblyRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira","assembly/"+id)).orElseThrow().value();
    }
    @GameTest(template="spell_arena")
    public static void slotValidationAndPersistence(GameTestHelper h) {
        var player=player(h);var gun=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());
        player.getInventory().setItem(0,gun);
        var menu=new ArquebusMenu(1,player.getInventory(),0);
        for(var target:new ElementType[]{ElementType.EARTH,ElementType.WATER,ElementType.FIRE,ElementType.AIR})
            for(var shape:new ElementType[]{ElementType.EARTH,ElementType.WATER,ElementType.FIRE,ElementType.AIR}) {
                boolean expected=shape==ElementType.AIR&&(target==ElementType.FIRE||target==ElementType.AIR);
                h.assertTrue(menu.getSlot(0).mayPlace(crystal(target,shape))==expected,"Wrong accepted spell pattern");
            }
        menu.setCarried(crystal(ElementType.AIR,ElementType.AIR));
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        h.assertTrue(ArcaneArquebusItem.accepts(ArcaneArquebusItem.crystal(gun)) && menu.getCarried().isEmpty(),"Loading did not persist");
        menu.clicked(28,0,net.minecraft.world.inventory.ClickType.THROW,player);
        h.assertTrue(player.getInventory().getItem(0)==gun,"Bound gun must not be movable");
        var ops=h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var loaded=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,gun).getOrThrow()).getOrThrow();
        h.assertTrue(ItemStack.matches(ArcaneArquebusItem.crystal(gun),ArcaneArquebusItem.crystal(loaded)),"Crystal lost after serialization");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void recipesAndUpgradePreserveContents(GameTestHelper h) {
        var input=new SimpleContainer(7);
        input.setItem(6,new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get()));
        input.setItem(5,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        input.setItem(2,new ItemStack(ModItems.WIDE_AREA_RUNE.get()));
        input.setItem(0,new ItemStack(ModItems.LIGHT_MICROCORE.get()));
        var wrapped=new AssemblyRecipe.Input(input);var craft=recipe(h,"arcane_arquebus");
        h.assertTrue(craft.matches(wrapped,h.getLevel()),"Gun outer ingredients must be shapeless");
        input.getItem(6).set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal(ElementType.AIR,ElementType.AIR))));
        h.assertTrue(!craft.matches(wrapped,h.getLevel()),"Do not consume stored grid crystals");
        var gun=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());
        gun.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal(ElementType.FIRE,ElementType.AIR))));
        gun.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Keepsake"));
        input.clearContent();input.setItem(6,gun);input.setItem(4,new ItemStack(ModItems.ANCESTOR_LAUNCHER.get()));
        var upgrade=recipe(h,"arquebus_upgrade");
        h.assertTrue(upgrade.matches(wrapped,h.getLevel()),"Upgrade should match");
        var output=upgrade.assemble(wrapped,h.getLevel().registryAccess());
        h.assertTrue(ArcaneArquebusItem.upgraded(output) && !ArcaneArquebusItem.upgraded(gun)
                && ItemStack.matches(ArcaneArquebusItem.crystal(output),ArcaneArquebusItem.crystal(gun))
                && output.getHoverName().equals(gun.getHoverName()),"Upgrade lost data or mutated input");
        input.setItem(6,output);h.assertTrue(!upgrade.matches(wrapped,h.getLevel()),"Duplicate plugin accepted");
        input.clearContent();input.setItem(6,new ItemStack(ModItems.DARK_MICROCORE.get()));
        for(int i=0;i<6;i++)input.setItem(i,new ItemStack(i%2==0?Items.TNT:ModItems.FIRE_MICROCORE.get()));
        h.assertTrue(recipe(h,"ancestor_launcher").matches(wrapped,h.getLevel()),"Rotated alternating recipe failed");
        input.setItem(0,new ItemStack(ModItems.FIRE_MICROCORE.get()));
        h.assertTrue(!recipe(h,"ancestor_launcher").matches(wrapped,h.getLevel()),"Nonalternating recipe accepted");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=80)
    public static void castingBonusesAndMana(GameTestHelper h) {
        var player=player(h);var gun=new ItemStack(ModItems.ARCANE_ARQUEBUS.get());
        gun.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(crystal(ElementType.AIR,ElementType.AIR))));
        gun.set(ModDataComponents.ANCESTOR_LAUNCHER,true);
        player.setItemInHand(InteractionHand.OFF_HAND,gun);
        h.assertTrue(!ArcaneArquebusItem.castLoaded(player,gun),"Offhand gun fired");
        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.MAIN_HAND,gun);
        player.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(5);
        h.assertTrue(ArcaneArquebusItem.castLoaded(player,gun),"Loaded aim spell failed");
        h.assertTrue(player.getData(ModAttachments.MANA).current()==945,"Expected base multiplier before reduction: 30*2-5");
        h.assertTrue(com.mcmagic.omnira.mana.ManaCosts.cost(player,30,1.5)==40,"Future percentage costs must precede reduction");
        player.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(100);
        h.assertTrue(ArcaneArquebusItem.manaCost(player,player.getMainHandItem(),new SpellPayload(30,4))==0,"Costs cannot become negative");
        player.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(5);
        var shots=h.getLevel().getEntitiesOfClass(SpellEntity.class,player.getBoundingBox().inflate(3),e->e.ownedBy(player));
        h.assertTrue(shots.size()==1 && Math.abs(shots.getFirst().getDeltaMovement().length()-.6)<1e-6
                && shots.getFirst().explosiveImpact(),"Shot must snapshot double speed and plugin");
        var power=gun.getAttributeModifiers().modifiers().stream().filter(e->e.attribute().equals(ModAttributes.SPELL_POWER)).findFirst().orElseThrow();
        h.assertTrue(power.slot()==EquipmentSlotGroup.MAINHAND && power.modifier().amount()==.5
                && power.modifier().operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE,"Power must be additive and mainhand only");
        h.assertTrue(!IndependentSpellCooldown.ready(player,gun),"Initial gun cooldown missing");
        h.assertTrue(IndependentSpellCooldown.ready(player,new ItemStack(gun.getItem())),"A second gun inherited the first one's cooldown");
        h.runAfterDelay(59,()->h.assertTrue(!IndependentSpellCooldown.ready(player,gun),"Ancestor cooldown ended before 3 seconds"));
        h.runAfterDelay(60,()->{
            h.assertTrue(IndependentSpellCooldown.ready(player,gun),"Ancestor cooldown longer than 3 seconds");
            player.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(54,1000));
            h.assertTrue(!ArcaneArquebusItem.castLoaded(player,gun) && player.getData(ModAttachments.MANA).current()==54,"Insufficient mana spent or fired");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena")
    public static void extendedTargetRange(GameTestHelper h) {
        var player=player(h);player.setPos(player.getX(),200,player.getZ());player.setYRot(0);player.setXRot(0);
        var point=BlockPos.containing(player.getEyePosition().add(0,0,40));
        h.getLevel().getChunkAt(point);h.getLevel().setBlockAndUpdate(point,Blocks.STONE.defaultBlockState());
        var pattern=new SpellPattern(ElementType.FIRE,ElementType.AIR);
        h.assertTrue(!SpellCasting.cast(player,pattern,SpellPayload.EMPTY,1),"Ordinary target unexpectedly reached 40 blocks");
        h.assertTrue(SpellCasting.castArquebus(player,pattern,SpellPayload.EMPTY,1,false),"Gun must reach 40 blocks");
        h.getLevel().setBlockAndUpdate(point,Blocks.AIR.defaultBlockState());
        var far=BlockPos.containing(player.getEyePosition().add(0,0,49));h.getLevel().getChunkAt(far);
        h.getLevel().setBlockAndUpdate(far,Blocks.STONE.defaultBlockState());
        h.assertTrue(!SpellCasting.castArquebus(player,pattern,SpellPayload.EMPTY,1,false),"Target selection must stop at 48");
        h.getLevel().setBlockAndUpdate(far,Blocks.AIR.defaultBlockState());h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void splashIsSmallAndDoesNotDoubleHit(GameTestHelper h) {
        var owner=player(h);owner.setPos(owner.position().add(8,0,0));
        Vec3 center=Vec3.atCenterOf(h.absolutePos(new BlockPos(4,4,4)));
        var direct=h.spawn(EntityType.COW,4,4,4);direct.setPos(center.subtract(0,direct.getBbHeight()/2,0));
        var near=h.spawn(EntityType.COW,5,4,4);near.setPos(center.add(1.2,-near.getBbHeight()/2,0));
        var far=h.spawn(EntityType.COW,7,4,4);far.setPos(center.add(2.5,-far.getBbHeight()/2,0));
        var shot=SpellEntity.spawn(h.getLevel(),owner,SpellEntity.Kind.PROJECTILE,center);
        shot.configure(1,new SpellPayload(30,4));shot.explosiveImpact(true);
        shot.applyEffects(direct);shot.trigger(true);shot.trigger(true);
        h.assertTrue(direct.getHealth()==6 && near.getHealth()==6 && far.getHealth()==10,"Wrong radius, damage or duplicate impact");
        h.succeed();
    }
}
